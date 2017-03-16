#include <assert.h>
#include <err.h>
#include <inttypes.h>	/* for PRIu32 */
#include <stdbool.h>
#include <stdio.h>	/* for printf */
#include <stdlib.h>	/* for malloc(3), NULL */
#include <string.h>	/* for memmove(3) */
#include <unistd.h>	/* for read(2) */
#include <sys/param.h>	/* for MAX() */
#include <sys/uio.h>	/* for readv(2) */
#include <sys/types.h>	/* for open(2) */
#include <sys/stat.h>	/* for open(2) */
#include <fcntl.h>	/* for open(2) */

#include "nbcompat.h"	/* for __arraycount, offsetof */
#include "tracefmt.h"
#include "legacy.h"
#include "reader.h"

#define	RSVD_NINTR_OUTST 0

typedef union {
	rvp_addr_t ub_pc;
	rvp_begin_t ub_begin;
	rvp_fork_join_switch_t ub_fork_join_switch;
	rvp_load1_2_4_store1_2_4_t ub_load1_2_4_store1_2_4;
	rvp_acquire_release_t ub_acquire_release;
	rvp_load8_store8_t ub_load8_store8;
	rvp_cog_t ub_cog;
	rvp_entersig_t ub_entersig;
	rvp_exitsig_t ub_exitsig;
	rvp_sigoutst_t ub_sigoutst;
	rvp_sigest_t ub_sigest;
	rvp_sigdis_t ub_sigdis;
	rvp_sigmaskmemo_t ub_sigmaskmemo;
	rvp_masksigs_t ub_masksigs;
	char ub_bytes[4096];
} rvp_ubuf_t;

typedef struct {
	size_t oi_reclen;
	const char *oi_descr;
} op_info_t;

#define OP_INFO_INIT(__ty, __descr) {.oi_reclen = sizeof(__ty), .oi_descr = __descr}

typedef struct _rvp_call {
	rvp_addr_t	*cs_funcs;
	int		cs_depth;
} rvp_callstack_t;

struct _rvp_pstate;
typedef struct _rvp_pstate rvp_pstate_t;

typedef struct _rvp_emitters {
	void (*init)(const rvp_pstate_t *);
	void (*emit_jump)(const rvp_pstate_t *, rvp_addr_t);
	void (*emit_op)(const rvp_pstate_t *, const rvp_ubuf_t *, rvp_op_t,
	    bool, int);
} rvp_emitters_t;

/* parse state: per-thread */
typedef struct _rvp_thread_pstate {
	rvp_addr_t	ts_lastpc[2];
	rvp_callstack_t	ts_callstack;
	bool		ts_present;
	uint64_t	ts_generation[2];
	uint64_t	ts_nops[2];
	uint64_t	ts_last_gid[2];
	bool		ts_sigs_masked;
} rvp_thread_pstate_t;

/* parse state: global */
struct _rvp_pstate {
	rvp_thread_pstate_t	*ps_thread;
	uint32_t		ps_nthreads;
	rvp_addr_t		ps_deltop_first, ps_deltop_last;
	uint32_t		ps_curthread;
	const rvp_emitters_t	*ps_emitters;
	uint32_t		ps_nintr_outst;
	int			ps_zeromasknum;
};

typedef struct _intmax_item intmax_item_t;

struct _intmax_item {
	uintmax_t	i_v;
	intmax_item_t *	i_next;
	uint32_t	i_id;
};

typedef struct _intmax_bucket {
	intmax_item_t *	b_head;
	size_t		b_nitems;
} intmax_bucket_t;

typedef struct _intmax_table {
	intmax_bucket_t	t_bucket[256];
	size_t		t_nitems;
	uint32_t	t_next_id;
	uint32_t	(*t_get_next_id)(uintmax_t);
} intmax_table_t;

static const op_info_t op_to_info[RVP_NOPS] = {
	  [RVP_OP_ENTERFN] = OP_INFO_INIT(rvp_end_enterfn_exitfn_t,
	     "enter function")
	, [RVP_OP_EXITFN] = OP_INFO_INIT(rvp_end_enterfn_exitfn_t,
	    "exit function")

	, [RVP_OP_BEGIN] = OP_INFO_INIT(rvp_begin_t, "begin thread")
	, [RVP_OP_COG] = OP_INFO_INIT(rvp_cog_t, "change of generation")
	, [RVP_OP_END] = OP_INFO_INIT(rvp_end_enterfn_exitfn_t, "end thread")
	, [RVP_OP_SIGOUTST] = OP_INFO_INIT(rvp_sigoutst_t,
	    "outstanding signals")
	, [RVP_OP_SWITCH] = OP_INFO_INIT(rvp_fork_join_switch_t,
					 "switch thread")
	, [RVP_OP_FORK] = OP_INFO_INIT(rvp_fork_join_switch_t,
	    "fork thread")
	, [RVP_OP_JOIN] = OP_INFO_INIT(rvp_fork_join_switch_t,
	    "join thread")

	, [RVP_OP_LOAD1] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "load 1")
	, [RVP_OP_STORE1] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "store 1")
	, [RVP_OP_LOAD2] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "load 2")
	, [RVP_OP_STORE2] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "store 2")
	, [RVP_OP_LOAD4] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "load 4")
	, [RVP_OP_STORE4] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "store 4")
	, [RVP_OP_LOAD8] = OP_INFO_INIT(rvp_load8_store8_t, "load 8")
	, [RVP_OP_STORE8] = OP_INFO_INIT(rvp_load8_store8_t, "store 8")

	, [RVP_OP_ATOMIC_LOAD4] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t,
					       "atomic load 4")
	, [RVP_OP_ATOMIC_STORE4] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t,
						"atomic store 4")
	, [RVP_OP_ATOMIC_LOAD8] = OP_INFO_INIT(rvp_load8_store8_t,
					       "atomic load 8")
	, [RVP_OP_ATOMIC_STORE8] = OP_INFO_INIT(rvp_load8_store8_t,
					        "atomic store 8")
	, [RVP_OP_ACQUIRE] = OP_INFO_INIT(rvp_acquire_release_t,
	    "acquire mutex")
	, [RVP_OP_RELEASE] = OP_INFO_INIT(rvp_acquire_release_t,
	    "release mutex")
	, [RVP_OP_ENTERSIG] = OP_INFO_INIT(rvp_entersig_t,
	    "enter signal handler")
	, [RVP_OP_EXITSIG] = OP_INFO_INIT(rvp_exitsig_t, "exit signal handler")
	, [RVP_OP_SIGEST] =
	    OP_INFO_INIT(rvp_sigest_t, "establish signal action")
	, [RVP_OP_SIGDIS] =
	    OP_INFO_INIT(rvp_sigdis_t, "disestablish signal action")
	, [RVP_OP_SIGMASKMEMO] =
	    OP_INFO_INIT(rvp_sigmaskmemo_t, "memoize signal mask")
	, [RVP_OP_MASKSIGS] =
	    OP_INFO_INIT(rvp_masksigs_t, "set signal mask")
};

static void emit_no_jump(const rvp_pstate_t *, rvp_addr_t);
static void emit_legacy_op(const rvp_pstate_t *, const rvp_ubuf_t *, rvp_op_t,
    bool, int);
static void emit_fork_metadata(uint64_t, uint64_t, uint32_t);

static uint32_t get_next_thdfd(uintmax_t);

static void legacy_init(const rvp_pstate_t *);

static void print_jump(const rvp_pstate_t *, rvp_addr_t);
static void print_op(const rvp_pstate_t *, const rvp_ubuf_t *, rvp_op_t,
    bool, int);

static const rvp_emitters_t plain_text = {
	  .emit_jump = print_jump
	, .emit_op = print_op
};

static const rvp_emitters_t legacy_binary = {
	  .init = legacy_init
	, .emit_jump = emit_no_jump
	, .emit_op = emit_legacy_op
};

static intmax_table_t thd_table = {.t_get_next_id = get_next_thdfd};
static int thdfd = -1;

static void
extract_jmpvec_and_op_from_deltop(rvp_addr_t deltop0,
    rvp_addr_t pc, int *jmpvecp, rvp_op_t *opp)
{
	/* XXX it's not strictly necessary for deltops to have any concrete
	 * storage
	 */
	deltops_t deltops;

	int row = (pc - deltop0) / __arraycount(deltops.matrix[0]);
	int jmpvec = row - RVP_NJMPS / 2;

	rvp_op_t op = (pc - deltop0) - (&deltops.matrix[row][0] - &deltops.matrix[0][0]);

	assert(op < RVP_NOPS);

	*opp = op;
	*jmpvecp = jmpvec;
}

static void
advance(char *buf, size_t *nfullp, size_t nbytes)
{
	size_t nfull = *nfullp;

	assert(nfull >= nbytes);
	nfull -= nbytes;
	memmove(buf, &buf[nbytes], nfull);
	*nfullp = nfull;
}

static void
rvp_pstate_extend_threads_over(rvp_pstate_t *ps, uint32_t tid,
    rvp_thread_pstate_t *othread, uint32_t onthreads)
{
	unsigned int i;

	assert(tid >= onthreads);

	for (ps->ps_nthreads = MAX(8, onthreads);
	     tid >= ps->ps_nthreads;
	     ps->ps_nthreads *= 2)
		;	/* do nothing */

	ps->ps_thread = calloc(ps->ps_nthreads, sizeof(rvp_thread_pstate_t));
	if (ps->ps_thread == NULL) {
		err(EXIT_FAILURE, "%s: malloc(%" PRIu32 " threads", __func__,
		    ps->ps_nthreads);
	}

	for (i = 0; i < ps->ps_nthreads; i++) {
		if (othread != NULL && i < onthreads && othread[i].ts_present)
			ps->ps_thread[i] = othread[i];
		else
			ps->ps_thread[i].ts_present = false;
	}
	if (othread != NULL)
		free(othread);
}

static void
rvp_pstate_begin_thread(rvp_pstate_t *ps, uint32_t tid, uint64_t generation)
{
	if (ps->ps_nthreads <= tid) {
		rvp_pstate_extend_threads_over(ps, tid, ps->ps_thread,
		    ps->ps_nthreads);
	}
	ps->ps_curthread = tid;
	ps->ps_nintr_outst = 0;
	rvp_thread_pstate_t *ts = &ps->ps_thread[tid];
	assert(!ts->ts_present);
	ts->ts_lastpc[0] = ps->ps_deltop_first;
	ts->ts_callstack = (rvp_callstack_t){NULL, 0};
	ts->ts_present = true;
	ts->ts_generation[0] = generation;
	ts->ts_generation[1] = 0;
	ts->ts_nops[ps->ps_nintr_outst] = 0;
	ts->ts_sigs_masked = false;
}

static void
rvp_pstate_init(rvp_pstate_t *ps, const rvp_emitters_t *emitters,
    rvp_addr_t op0, uint32_t tid, uint64_t generation)
{
	/* XXX it's not strictly necessary for deltops to have any concrete
	 * storage
	 */
	deltops_t deltops;

	ps->ps_emitters = emitters;

	ps->ps_deltop_first = op0 -
	    (&deltops.matrix[RVP_NJMPS / 2][RVP_OP_BEGIN] -
	     &deltops.matrix[0][0]);
	ps->ps_deltop_last = ps->ps_deltop_first +
	    (&deltops.matrix[RVP_NJMPS - 1][RVP_NOPS - 1] -
	     &deltops.matrix[0][0]);
	ps->ps_thread = NULL;
	ps->ps_nthreads = 0;

#if 0
	rvp_pstate_begin_thread(ps, tid, generation);
#endif
}

static ssize_t
iovsum(const struct iovec *iov, int iovcnt)
{
	int i;
	ssize_t sum = 0;

	for (i = 0; i < iovcnt; i++)
		sum += iov[i].iov_len;
	
	return sum;
}

static inline bool
pc_is_not_deltop(rvp_pstate_t *ps, rvp_addr_t pc)
{
	return pc < ps->ps_deltop_first || ps->ps_deltop_last < pc;
}

static void
legacy_init(const rvp_pstate_t *ps)
{
	thdfd = open("thd_metadata.bin", O_WRONLY|O_CREAT|O_TRUNC, 0600);
	if (thdfd == -1)
		err(EXIT_FAILURE, "%s: open", __func__);
}

static void
emit_no_jump(const rvp_pstate_t *ps, rvp_addr_t pc)
{
	return;
}

static int
rvp_op_to_legacy_op(rvp_op_t op)
{
	switch (op) {
	case RVP_OP_ENTERFN:
		return INVOKE_METHOD;
	case RVP_OP_EXITFN:
		return FINISH_METHOD;
	case RVP_OP_LOAD8:
	case RVP_OP_LOAD4:
	case RVP_OP_LOAD2:
	case RVP_OP_LOAD1:
		return READ;
	case RVP_OP_STORE8:
	case RVP_OP_STORE4:
	case RVP_OP_STORE2:
	case RVP_OP_STORE1:
		return WRITE;
	case RVP_OP_FORK:
		return START;
	case RVP_OP_JOIN:
		return JOIN;
	case RVP_OP_ACQUIRE:
		return WRITE_LOCK;
	case RVP_OP_RELEASE:
		return WRITE_UNLOCK;
	case RVP_OP_COG:	// bookkeeping w/ no legacy correspondence
	case RVP_OP_SWITCH:	// bookkeeping w/ no legacy correspondence
	case RVP_OP_BEGIN:	// implicit w/ first operation for a tid
	case RVP_OP_END:	// implicit w/ last operation for a tid
	case RVP_OP_ATOMIC_RMW1:
	case RVP_OP_ATOMIC_RMW2:
	case RVP_OP_ATOMIC_RMW4:
	case RVP_OP_ATOMIC_RMW8:
	case RVP_OP_ATOMIC_RMW16:
	case RVP_OP_ATOMIC_STORE16:
	case RVP_OP_ATOMIC_LOAD16:
	case RVP_OP_STORE16:
	case RVP_OP_LOAD16:
	default:
		return -1;
	}
}

/* Look up `v` in the table `t` and add it if it is not present.  Return
 * the unique ID assigned to `v` by the table.
 */
static uint32_t
intmax_table_put(intmax_table_t *t, uintmax_t v, bool *is_newp)
{
	int hashcode = v % __arraycount(t->t_bucket);
	intmax_bucket_t *b = &t->t_bucket[hashcode];
	intmax_item_t **itemp, *item, *nitem;

	for (itemp = &b->b_head;
	     (item = *itemp) != NULL;
	     itemp = &item->i_next) {
		if (item->i_v == v) {
			if (is_newp != NULL)
				*is_newp = false;
			return item->i_id;
		}
		if (item->i_v > v)
			break;
	}
	if ((nitem = malloc(sizeof(*nitem))) == NULL)
		err(EXIT_FAILURE, "%s: malloc", __func__);
	t->t_nitems++;
	b->b_nitems++;
	nitem->i_id = (t->t_get_next_id == NULL)
	    ? t->t_next_id++
	    : (*t->t_get_next_id)(v);
	nitem->i_v = v;
	nitem->i_next = item;
	*itemp = nitem;
	if (is_newp != NULL)
		*is_newp = true;
	return nitem->i_id;
}

static ssize_t
writeall(int fd, const void *buf, size_t nbytes)
{
	ssize_t nwritten;
	const char *p = buf;

	for (; nbytes > 0; p += nwritten, nbytes -= nwritten) {
		nwritten = write(fd, p, nbytes);
		if (nwritten == -1)
			return -1;
	}
	return nbytes;
}

static uint64_t
emit_addr(rvp_addr_t addr)
{
	const char nil = '\0';
	static intmax_table_t addr_table;
	static int fd = -1;
	uint64_t id64;
	bool is_new = false;

	id64 = intmax_table_put(&addr_table, addr, &is_new);

	if (!is_new)
		return id64;

	if (fd == -1) {
		fd = open("./var_metadata.bin", O_WRONLY|O_CREAT|O_TRUNC, 0600);
		if (fd == -1)
			err(EXIT_FAILURE, "%s: open", __func__);
	}
	if (writeall(fd, &id64, sizeof(id64)) == -1 ||
	    dprintf(fd, "%#016" PRIxPTR, addr) < 0 ||
	    writeall(fd, &nil, sizeof(nil)) == -1)
		errx(EXIT_FAILURE, "%s: could not write record", __func__);

	return id64;
}

static uint32_t
compress_pc(uint64_t pc)
{
	const char nil = '\0';
	static intmax_table_t pc_table;
	static int fd = -1;
	uint32_t id;
	uint64_t id64;
	bool is_new = false;

	id = intmax_table_put(&pc_table, pc, &is_new);

	if (!is_new)
		return id;

	if (fd == -1) {
		fd = open("./loc_metadata.bin", O_WRONLY|O_CREAT|O_TRUNC, 0600);
		if (fd == -1)
			err(EXIT_FAILURE, "%s: open", __func__);
	}
	id64 = id;
	if (writeall(fd, &id64, sizeof(id64)) == -1 ||
	    dprintf(fd, "fn:%#016" PRIx64 ";file:%s;line:%d", pc, "dummy.c",
	            999) < 0 ||
	    writeall(fd, &nil, sizeof(nil)) == -1)
		errx(EXIT_FAILURE, "%s: could not write record", __func__);

	return id;
}

static uint32_t
get_next_thdfd(uintmax_t tid)
{
	int fd, rc;
	static int next_trace_fileid = 0;
	char buf[sizeof("18446744073709551615_trace.bin")];

	rc = snprintf(buf, sizeof(buf), "%d_trace.bin", next_trace_fileid++);
	if (rc < 0 || rc >= (int)sizeof(buf))
		errx(EXIT_FAILURE, "%s: snprintf", __func__);

	fd = open(buf, O_WRONLY|O_CREAT|O_TRUNC, 0600);
	if (fd == -1)
		err(EXIT_FAILURE, "%s: open", __func__);

	return fd;
}


/* In the assignment of a GID, it is important that an event is not processed
 * in a later window than any of its side-effects.
 *
 * As of Mon Feb 20 15:14:08 CST 2017, the analysis backend does not
 * respect the GID's generation "field" in the formation of windows,
 * but it really should!
 *
 * As of Fri Mar 3 10:43:32 CST 2017, the analysis backend on the
 * simpler-faster branch does respect the generation field.
 */
static uint64_t
rvp_pstate_next_gid(const rvp_pstate_t *ps)
{
	uint64_t gid;
	rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];

	assert(ts->ts_generation[ps->ps_nintr_outst] <= UINT16_MAX);
	assert(ts->ts_nops[ps->ps_nintr_outst] < UINT32_MAX);
	assert(ps->ps_curthread <= UINT16_MAX);

	++ts->ts_nops[ps->ps_nintr_outst];
	gid = (ts->ts_generation[ps->ps_nintr_outst] << 48) |
	    (ts->ts_nops[ps->ps_nintr_outst] << 16) | ps->ps_curthread;

	assert(ts->ts_last_gid[ps->ps_nintr_outst] < gid);

	ts->ts_last_gid[ps->ps_nintr_outst] = gid;
	return gid;
}

static uint64_t
legacy_tid(const rvp_pstate_t *ps)
{
	/* TBD when we're handling multiple outstanding signals, put that
	 * into the TID
	 */
	assert(ps->ps_nintr_outst <= 0x7fffffff);
	if (ps->ps_nintr_outst != RSVD_NINTR_OUTST) {
		return ps->ps_curthread | ((uint64_t)ps->ps_nintr_outst << 32) |
		    ((uint64_t)1 << 63);
	}

	return ps->ps_curthread;
}

static uint64_t
legacy_signal_tid(uint32_t signum)
{
	return (uint64_t)signum | ((uint64_t)RSVD_NINTR_OUTST << 32) |
	    ((uint64_t)1 << 63);
}

static void
emit_signal_legacy_op(const rvp_pstate_t *ps, const rvp_ubuf_t *ub, rvp_op_t op)
{
	int fd;
	static int zeromasknum = -1;
	legacy_event_t auxev, ev;
	rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];

	switch (op) {
	case RVP_OP_SIGMASKMEMO:
		assert(ub->ub_sigmaskmemo.masknum <= INT_MAX);
		if (ub->ub_sigmaskmemo.mask == 0)
			zeromasknum = ub->ub_sigmaskmemo.masknum;
		return;
	case RVP_OP_MASKSIGS:
		assert(ub->ub_masksigs.masknum <= INT_MAX);
		bool masksigs;
		if (zeromasknum == -1) {
			// if no zero mask memo has been seen, then assume
			// that this is the non-zero mask
			//
			// TBD what keeps a memo from appearing before its
			// first use in a trace, anyway?
			masksigs = true;
		} else if ((int)ub->ub_masksigs.masknum == zeromasknum) {
			// release signal-blocking mutex
			masksigs = false;
		} else {
			// acquire signal-blocking mutex
			masksigs = true;
		}
		if (ts->ts_sigs_masked == masksigs)
			return;
		ts->ts_sigs_masked = masksigs;
		ev.type = masksigs ? WRITE_LOCK : WRITE_UNLOCK;
		ev.addr = emit_addr(ps->ps_deltop_first +
		    offsetof(deltops_t, rsvd) -
		    offsetof(deltops_t, matrix[0][0]));
		break;
	case RVP_OP_SIGEST:
		ev.type = START;
		ev.addr = legacy_signal_tid(ub->ub_sigest.signum);
		ev.value = 0;
		emit_fork_metadata(
		    legacy_tid(ps), legacy_signal_tid(ub->ub_sigest.signum),
		    compress_pc(ts->ts_lastpc[ps->ps_nintr_outst]));
		break;
	case RVP_OP_ENTERSIG:
		/* Emit events 'begin' and 'acquire'.
		 *
		 * The 'begin' is implicit in the first use of this signal's
		 * TID
		 *
		 * I use a WRITE_LOCK for the 'acquire'.  The 'acquire'
		 * is to ensure that this signal respects the signal mask.
		 * Note that, for now, I treat any non-zero mask like it
		 * blocks *all* signals.
		 */

		auxev.type = START;
		auxev.tid = legacy_signal_tid(ub->ub_entersig.signum);
		auxev.stmtid = compress_pc(ts->ts_lastpc[ps->ps_nintr_outst]);
		auxev.gid = rvp_pstate_next_gid(ps);
		auxev.addr = legacy_tid(ps);
		auxev.value = 0;

		fd = intmax_table_put(&thd_table, auxev.tid, NULL);
		if (write(fd, &auxev, sizeof(auxev)) == -1)
			err(EXIT_FAILURE, "%s: write", __func__);

		emit_fork_metadata(
		    legacy_signal_tid(ub->ub_entersig.signum),
		    legacy_tid(ps), compress_pc(ts->ts_lastpc[ps->ps_nintr_outst]));

#if 0
		assert(!ts->ts_sigs_masked);
#endif

		ts->ts_sigs_masked = true;

		ev.type = WRITE_LOCK;
		ev.addr = emit_addr(ps->ps_deltop_first +
		    offsetof(deltops_t, rsvd) -
		    offsetof(deltops_t, matrix[0][0]));
		break;
	case RVP_OP_EXITSIG:
		/* Emit events 'release' and 'end'.
		 *
		 * I use a WRITE_UNLOCK for the 'release'.
		 *
		 * The 'end' is implicit in the last use of the signal's TID.
		 */
		assert(ts->ts_sigs_masked);

		ts->ts_sigs_masked = false;

		ev.type = WRITE_UNLOCK;
		ev.addr = emit_addr(ps->ps_deltop_first +
		    offsetof(deltops_t, rsvd) -
		    offsetof(deltops_t, matrix[0][0]));
		break;
	default:
		return;
	}

	ev.tid = legacy_tid(ps);
	ev.stmtid = compress_pc(ts->ts_lastpc[ps->ps_nintr_outst]);
	// assign GID after we're sure this op is one we can handle
	ev.gid = rvp_pstate_next_gid(ps);

	fd = intmax_table_put(&thd_table, legacy_tid(ps), NULL);
	if (write(fd, &ev, sizeof(ev)) == -1)
		err(EXIT_FAILURE, "%s: write", __func__);
}

static void
emit_extended_legacy_op(const rvp_pstate_t *ps, const rvp_ubuf_t *ub,
    rvp_op_t op)
{
	int fd, i;
	legacy_event_t ev[3];
	rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];

	switch (op) {
	case RVP_OP_ATOMIC_LOAD8:
	case RVP_OP_ATOMIC_LOAD4:
	case RVP_OP_ATOMIC_LOAD2:
	case RVP_OP_ATOMIC_LOAD1:
		ev[1].type = READ;
		break;
	case RVP_OP_ATOMIC_STORE8:
	case RVP_OP_ATOMIC_STORE4:
	case RVP_OP_ATOMIC_STORE2:
	case RVP_OP_ATOMIC_STORE1:
		ev[1].type = WRITE;
		break;
	default:
		emit_signal_legacy_op(ps, ub, op);
		return;
	}

	for (i = 0; i < 3; i++) {
		/* XXX XXX XXX TBD use generation number plus a
		 * thread-local op counter?  The important thing
		 * is that an event is not processed in a later
		 * window than any of its side-effects.
		 */
		ev[i].gid = rvp_pstate_next_gid(ps);
		ev[i].tid = legacy_tid(ps);
		ev[i].stmtid = compress_pc(ts->ts_lastpc[ps->ps_nintr_outst]);
	}

	switch (op) {
	case RVP_OP_ATOMIC_LOAD8:
	case RVP_OP_ATOMIC_STORE8:
		ev[1].value = ub->ub_load8_store8.data;
		ev[1].addr = emit_addr(ub->ub_load8_store8.addr);
		break;
	case RVP_OP_ATOMIC_LOAD4:
	case RVP_OP_ATOMIC_STORE4:
	case RVP_OP_ATOMIC_LOAD2:
	case RVP_OP_ATOMIC_STORE2:
	case RVP_OP_ATOMIC_LOAD1:
	case RVP_OP_ATOMIC_STORE1:
		ev[1].value = ub->ub_load1_2_4_store1_2_4.data;
		ev[1].addr = emit_addr(ub->ub_load1_2_4_store1_2_4.addr);
		break;
	default:
		errx(EXIT_FAILURE, "%s: conversion unknown", __func__);
	}

	ev[0].type = WRITE_LOCK;
	ev[0].addr = emit_addr(~ev[1].addr);

	ev[2].type = WRITE_UNLOCK;
	ev[2].addr = emit_addr(~ev[1].addr);

	fd = intmax_table_put(&thd_table, legacy_tid(ps), NULL);
	if (write(fd, &ev, sizeof(ev)) == -1)
		err(EXIT_FAILURE, "%s: write", __func__);
}

static void
emit_fork_metadata(uint64_t curtid, uint64_t newtid, uint32_t stmtid)
{
	thd_record_t thd_record = {
		  .newtid = newtid
		, .curtid = curtid
		, .stmtid = stmtid
	};
	if (writeall(thdfd, &thd_record, sizeof(thd_record)) == -1) {
		errx(EXIT_FAILURE, "%s: could not write record",
		     __func__);
	}
}

static void
emit_legacy_op(const rvp_pstate_t *ps, const rvp_ubuf_t *ub, rvp_op_t op,
    bool is_load, int field_width)
{
	legacy_event_t ev;
	rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
	int lop = rvp_op_to_legacy_op(op);

	if (lop < 0) {
		emit_extended_legacy_op(ps, ub, op);
		return;
	}

	ev.gid = rvp_pstate_next_gid(ps);
	ev.tid = legacy_tid(ps);
	ev.stmtid = compress_pc(ts->ts_lastpc[ps->ps_nintr_outst]);

	assert(lop <= UINT8_MAX);

	ev.type = lop;

	switch (op) {
	case RVP_OP_ATOMIC_LOAD8:
	case RVP_OP_ATOMIC_STORE8:
	case RVP_OP_LOAD8:
	case RVP_OP_STORE8:
		ev.value = ub->ub_load8_store8.data;
		ev.addr = emit_addr(ub->ub_load8_store8.addr);
		break;
	case RVP_OP_ATOMIC_LOAD4:
	case RVP_OP_ATOMIC_STORE4:
	case RVP_OP_ATOMIC_LOAD2:
	case RVP_OP_ATOMIC_STORE2:
	case RVP_OP_ATOMIC_LOAD1:
	case RVP_OP_ATOMIC_STORE1:
	case RVP_OP_LOAD4:
	case RVP_OP_STORE4:
	case RVP_OP_LOAD2:
	case RVP_OP_STORE2:
	case RVP_OP_LOAD1:
	case RVP_OP_STORE1:
		ev.value = ub->ub_load1_2_4_store1_2_4.data;
		ev.addr = emit_addr(ub->ub_load1_2_4_store1_2_4.addr);
		break;
	default:
		errx(EXIT_FAILURE, "%s: conversion unknown", __func__);
	case RVP_OP_FORK:
		emit_fork_metadata(ps->ps_curthread,
		    ub->ub_fork_join_switch.tid, compress_pc(ts->ts_lastpc[ps->ps_nintr_outst]));
		/*FALLTHROUGH*/
	case RVP_OP_JOIN:
		ev.addr = ub->ub_fork_join_switch.tid;
		ev.value = 0;
		break;
	case RVP_OP_ENTERFN:
	case RVP_OP_EXITFN:
		break;
	case RVP_OP_ACQUIRE:
	case RVP_OP_RELEASE:
		ev.addr = emit_addr(ub->ub_acquire_release.addr);
		break;
	}
	int fd = intmax_table_put(&thd_table, legacy_tid(ps), NULL);
	if (write(fd, &ev, sizeof(ev)) == -1)
		err(EXIT_FAILURE, "%s: write", __func__);
}

static void
print_jump(const rvp_pstate_t *ps, rvp_addr_t pc)
{ 
	printf("tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR " jump\n",
	    ps->ps_curthread, ps->ps_nintr_outst, pc);
}

static void
print_op(const rvp_pstate_t *ps, const rvp_ubuf_t *ub, rvp_op_t op,
    bool is_load, int field_width)
{
	const op_info_t *oi = &op_to_info[op];

	switch (op) {
	case RVP_OP_ATOMIC_LOAD8:
	case RVP_OP_ATOMIC_STORE8:
	case RVP_OP_LOAD8:
	case RVP_OP_STORE8:
		printf("tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR
		    " %s %#.*" PRIx64 " %s [%#016" PRIxPTR "]\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst], oi->oi_descr,
		    field_width,
		    ub->ub_load8_store8.data,
		    is_load ? "<-" : "->",
		    ub->ub_load8_store8.addr);
		break;
	case RVP_OP_ATOMIC_LOAD4:
	case RVP_OP_ATOMIC_STORE4:
	case RVP_OP_ATOMIC_LOAD2:
	case RVP_OP_ATOMIC_STORE2:
	case RVP_OP_ATOMIC_LOAD1:
	case RVP_OP_ATOMIC_STORE1:
	case RVP_OP_LOAD4:
	case RVP_OP_STORE4:
	case RVP_OP_LOAD2:
	case RVP_OP_STORE2:
	case RVP_OP_LOAD1:
	case RVP_OP_STORE1:
		printf("tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR
		    " %s %#.*" PRIx32 " %s [%#016" PRIxPTR "]\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst], oi->oi_descr,
		    field_width,
		    ub->ub_load1_2_4_store1_2_4.data,
		    is_load ? "<-" : "->",
		    ub->ub_load1_2_4_store1_2_4.addr);
		break;
	case RVP_OP_BEGIN:
		printf(
		    "tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR " %s"
		    " generation %" PRIu64 "\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst],
		    oi->oi_descr, ub->ub_begin.generation);
		break;
	case RVP_OP_SIGOUTST:
		printf(
		    "tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR " %s"
		    " -> %" PRIu32 "\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst],
		    oi->oi_descr, ub->ub_sigoutst.noutst);
		break;
	case RVP_OP_COG:
		printf(
		    "tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR " %s"
		    " -> %" PRIu64 "\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst],
		    oi->oi_descr, ub->ub_cog.generation);
		break;
	case RVP_OP_END:
	default:
		printf("tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR " %s\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst], oi->oi_descr);
		break;
	case RVP_OP_FORK:
	case RVP_OP_JOIN:
	case RVP_OP_SWITCH:
		printf(
		    "tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR " %s tid %" PRIu32 "\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst],
		    oi->oi_descr, ub->ub_fork_join_switch.tid);
		// TBD create a fledgling rvp_thread_pstate_t on fork?
		break;
	case RVP_OP_ENTERSIG:
		printf(
		    "tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR
		    " %s signal %" PRIu32 " generation %" PRIu64 "\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst],
		    oi->oi_descr, ub->ub_entersig.signum,
		    ub->ub_entersig.generation);
		break;
	case RVP_OP_ACQUIRE:
	case RVP_OP_RELEASE:
		printf("tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR
		    " %s [%#016" PRIxPTR "]\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst], oi->oi_descr,
		    ub->ub_acquire_release.addr);
		break;
	case RVP_OP_SIGDIS:
		printf("tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR
		    " %s signal %" PRIu32 "\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst], oi->oi_descr,
		    ub->ub_sigest.signum);
		break;
	case RVP_OP_SIGEST:
		printf("tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR
		    " %s signal %" PRIu32 " handler %#016" PRIxPTR
		    " mask #%" PRIu32 "\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst], oi->oi_descr,
		    ub->ub_sigest.signum,
		    ub->ub_sigest.handler,
		    ub->ub_sigest.masknum);
		break;
	case RVP_OP_SIGMASKMEMO:
		printf("tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR
		    " %s #%" PRIu32 " origin %" PRIu32
		    " bits %#016" PRIx64 "\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst], oi->oi_descr,
		    ub->ub_sigmaskmemo.masknum,
		    ub->ub_sigmaskmemo.origin,
		    ub->ub_sigmaskmemo.mask);
		break;
	case RVP_OP_MASKSIGS:
		printf("tid %" PRIu32 ".%" PRIu32 " pc %#016" PRIxPTR
		    " %s #%" PRIu32 "\n",
		    ps->ps_curthread, ps->ps_nintr_outst,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst], oi->oi_descr,
		    ub->ub_masksigs.masknum);
		break;
	}
}

/*
 * Consumes nothing and returns the number of bytes that the buffer is
 * short of a full trace.
 *
 * Otherwise, consumes the trace at the start of the buffer `ub`, prints
 * the trace, shifts the bytes of the buffer left by the number of bytes
 * consumed, and returns 0.
 */
static size_t
consume_and_print_trace(rvp_pstate_t *ps, rvp_ubuf_t *ub, size_t *nfullp)
{
	const rvp_emitters_t *emitters = ps->ps_emitters;
	rvp_op_t op;
	rvp_addr_t lastpc;
	int jmpvec;
	bool is_load = false;
	int field_width = 0;

	if (pc_is_not_deltop(ps, ub->ub_pc)) {
		ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst] = ub->ub_pc;
		(*emitters->emit_jump)(ps, ub->ub_pc);
		advance(&ub->ub_bytes[0], nfullp, sizeof(ub->ub_pc));
		return 0;
	}
	extract_jmpvec_and_op_from_deltop(ps->ps_deltop_first,
	    ub->ub_pc, &jmpvec, &op);
	const op_info_t *oi = &op_to_info[op];
	if (oi->oi_descr == NULL)
		errx(EXIT_FAILURE, "unknown op %d\n", op);
	/* need to top off buffer? */
	if (*nfullp < oi->oi_reclen)
		return oi->oi_reclen - *nfullp;
	/* If this is the first 'begin', then we won't have a valid
	 * ps->ps_curthread until after the rvp_pstate_begin_thread()
	 * call.
	 */
	if (op == RVP_OP_BEGIN) {
		rvp_pstate_begin_thread(ps, ub->ub_begin.tid,
		    ub->ub_begin.generation);
	} else if (op == RVP_OP_ENTERSIG) {
		rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
		ts->ts_generation[ps->ps_nintr_outst] =
		    ub->ub_entersig.generation;
	}
	assert(op != RVP_OP_ENTERSIG || ps->ps_nintr_outst != 0);

	lastpc = ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst];
	ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_nintr_outst] = lastpc + jmpvec;
	switch (op) {
	case RVP_OP_ATOMIC_LOAD8:
	case RVP_OP_ATOMIC_STORE8:
	case RVP_OP_LOAD8:
	case RVP_OP_STORE8:
		field_width = 16;
		break;
	case RVP_OP_ATOMIC_LOAD4:
	case RVP_OP_ATOMIC_STORE4:
	case RVP_OP_LOAD4:
	case RVP_OP_STORE4:
		field_width = 8;
		break;
	case RVP_OP_ATOMIC_LOAD2:
	case RVP_OP_ATOMIC_STORE2:
	case RVP_OP_LOAD2:
	case RVP_OP_STORE2:
		field_width = 4;
		break;
	case RVP_OP_ATOMIC_LOAD1:
	case RVP_OP_ATOMIC_STORE1:
	case RVP_OP_LOAD1:
	case RVP_OP_STORE1:
		field_width = 2;
		break;
	default:
		break;
	}
	switch (op) {
	case RVP_OP_ATOMIC_LOAD8:
	case RVP_OP_ATOMIC_LOAD4:
	case RVP_OP_ATOMIC_LOAD2:
	case RVP_OP_ATOMIC_LOAD1:
	case RVP_OP_LOAD8:
	case RVP_OP_LOAD4:
	case RVP_OP_LOAD2:
	case RVP_OP_LOAD1:
		is_load = true;
		break;
	default:
		break;
	}

	(*emitters->emit_op)(ps, ub, op, is_load, field_width);

	if (op == RVP_OP_COG) {
		rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
		assert(ps->ps_nintr_outst < __arraycount(ts->ts_generation));
		assert(ts->ts_generation[ps->ps_nintr_outst] < ub->ub_cog.generation);
		ts->ts_generation[ps->ps_nintr_outst] = ub->ub_cog.generation;
		ts->ts_nops[ps->ps_nintr_outst] = 0;
	}

	if (op == RVP_OP_END) {
		rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
		ts->ts_present = false;
	}

	if (op == RVP_OP_SWITCH) {
		ps->ps_curthread = ub->ub_fork_join_switch.tid;
		ps->ps_nintr_outst = 0;
	} else if (op == RVP_OP_EXITSIG) {
		assert(ps->ps_nintr_outst != 0);
	} else if (op == RVP_OP_SIGOUTST) {
		rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];

		assert(ub->ub_sigoutst.noutst <
		    __arraycount(ts->ts_generation));
		ps->ps_nintr_outst = ub->ub_sigoutst.noutst;
	}

	advance(&ub->ub_bytes[0], nfullp, oi->oi_reclen);
	return 0;
}

void
rvp_trace_dump(rvp_output_type_t otype, int fd)
{
	rvp_pstate_t ps0;
	rvp_pstate_t *ps = &ps0;
	rvp_trace_header_t th;
	/* XXX make a compile-time assertion that this is a little-endian
	 * machine, since we assume that when we read the initial 'begin'
	 * operation, below.
	 */
	const rvp_trace_header_t expected_th = {
		  .th_magic = "RVP_"
		, .th_version = 0
		, .th_byteorder = '0' | ('1' << 8) | ('2' << 16) | ('3' << 24)
		, .th_pointer_width = sizeof(rvp_addr_t)
		, .th_data_width = sizeof(uint32_t)
		, .th_pad1 = { 0 }
	};
	rvp_addr_t pc0;
	uint32_t tid;
	uint64_t generation;
	ssize_t nread;
	rvp_ubuf_t ub;
	const struct iovec iov[] = {
		  { .iov_base = &th, .iov_len = sizeof(th) }
		, { .iov_base = &pc0, .iov_len = sizeof(pc0) }
		, { .iov_base = &tid, .iov_len = sizeof(tid) }
		, { .iov_base = &generation, .iov_len = sizeof(generation) }
	};
	const rvp_emitters_t *emitters;

	switch (otype) {
	case RVP_OUTPUT_PLAIN_TEXT:
		emitters = &plain_text;
		break;
	case RVP_OUTPUT_LEGACY_BINARY:
		emitters = &legacy_binary;
		break;
	default:
		errx(EXIT_FAILURE, "%s: unknown output type %d", __func__,
		     otype);
	}

	if ((nread = readv(fd, iov, __arraycount(iov))) == -1)
		err(EXIT_FAILURE, "%s: readv(header)", __func__);

	if (nread < iovsum(iov, __arraycount(iov))) {
		errx(EXIT_FAILURE, "%s: short read (header + 1st deltop + ggen init)",
		    __func__);
	}

	if (memcmp(th.th_magic, expected_th.th_magic, sizeof(th.th_magic)) != 0)
		errx(EXIT_FAILURE, "%s: bad magic %4s", __func__, th.th_magic);
	if (th.th_version != expected_th.th_version)
		errx(EXIT_FAILURE, "%s: unsupported version", __func__);
	if (th.th_byteorder != expected_th.th_byteorder)
		errx(EXIT_FAILURE, "%s: unsupported byteorder", __func__);
	if (th.th_pointer_width != expected_th.th_pointer_width)
		errx(EXIT_FAILURE, "%s: unsupported pointer width", __func__);
	if (th.th_data_width != expected_th.th_data_width)
		errx(EXIT_FAILURE, "%s: unsupported data width", __func__);

	if (tid != 1) {
		errx(EXIT_FAILURE, "%s: expected thread 1, read %" PRIu32,
		    __func__, tid);
	}

	rvp_pstate_init(ps, emitters, pc0, tid, generation);

	if (emitters->init != NULL)
		(*emitters->init)(ps);

	ub.ub_begin = (rvp_begin_t){.deltop = pc0, .tid = tid};

	size_t nfull = sizeof(ub.ub_begin);
	ssize_t nshort = 0;
	for (;;) {
		nread = read(fd, &ub.ub_bytes[nfull], sizeof(ub) - nfull);
		if (nread == -1) {
			err(EXIT_FAILURE, "%s: read failed (pc/deltop)",
			    __func__);
		}
		nfull += nread;
		if (nread < nshort) {
			errx(EXIT_FAILURE, "%s: trace is short %zu bytes",
			    __func__, nshort - nread);
		}
		nshort = 0;
		if (nfull == 0)
			break;
		if (nfull < sizeof(ub.ub_pc)) {
			errx(EXIT_FAILURE, "%s: short read (pc/deltop)",
			    __func__);
		}
		do {
			nshort = consume_and_print_trace(ps, &ub, &nfull);
			if (nshort != 0)
				break;
		} while (nfull >= sizeof(ub.ub_pc));
	}
}
