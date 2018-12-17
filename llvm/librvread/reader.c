#include <assert.h>
#include <err.h>
#include <inttypes.h>	/* for PRIu32 */
#include <stdbool.h>
#include <stdio.h>	/* for printf */
#include <stdlib.h>	/* for malloc(3), NULL */
#include <string.h>	/* for memmove(3) */
#include <unistd.h>	/* for read(2) */
#include <sys/param.h>	/* for MAX() */
#include <sys/types.h>	/* for open(2) */
#include <sys/stat.h>	/* for open(2) */
#include <fcntl.h>	/* for open(2) */

#include "io.h"	/* for __arraycount, offsetof */
#include "nbcompat.h"	/* for __arraycount, offsetof */
#include "reader.h"

#define	RSVD_IDEPTH 0

typedef char prebuf_t[
    sizeof("tid 4294967295.4294967295 18446744073709551615 ") +
    MAX(sizeof("{0x0123456789abcdef}"), sizeof("pc 0x0123456789abcdef"))];

typedef struct {
	size_t oi_reclen;
	const char *oi_descr;
} op_info_t;

#define OP_INFO_INIT(__ty, __descr)	\
	{.oi_reclen = sizeof(__ty), .oi_descr = __descr}

static char *preamble_string(const rvp_pstate_t *, char *, size_t);

char *insnptr_to_simple_string(const rvp_pstate_t *, char *, size_t,
    rvp_addr_t);
char *insnptr_to_symbol_friendly_string(const rvp_pstate_t *, char *, size_t,
    rvp_addr_t);
char *dataptr_to_simple_string(const rvp_pstate_t *, char *, size_t,
    rvp_addr_t);
char *dataptr_to_symbol_friendly_string(const rvp_pstate_t *, char *, size_t,
    rvp_addr_t);

static void print_nop(const rvp_pstate_t *, const rvp_ubuf_t *);
static void print_op(const rvp_pstate_t *, const rvp_ubuf_t *, rvp_op_t,
    bool, int);

static void reemit_binary_init(const rvp_pstate_t *,
    const rvp_trace_header_t *);
static void reemit_binary_nop(const rvp_pstate_t *, const rvp_ubuf_t *);
static void reemit_binary_op(const rvp_pstate_t *, const rvp_ubuf_t *, rvp_op_t,
    bool, int);

static const op_info_t op_to_info[RVP_NOPS] = {
	  [RVP_OP_ENTERFN] = OP_INFO_INIT(rvp_enterfn_t, "enter function")
	, [RVP_OP_EXITFN] = OP_INFO_INIT(rvp_end_exitfn_t, "exit function")

	, [RVP_OP_BEGIN] = OP_INFO_INIT(rvp_begin_t, "begin thread")
	, [RVP_OP_COG] = OP_INFO_INIT(rvp_cog_t, "change of generation")
	, [RVP_OP_END] = OP_INFO_INIT(rvp_end_exitfn_t, "end thread")
	, [RVP_OP_SIGDEPTH] = OP_INFO_INIT(rvp_sigdepth_t,
	    "signal depth")
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

	, [RVP_OP_ATOMIC_RMW1] = OP_INFO_INIT(rvp_rmw1_2_t, "atomic rmw 1")
	, [RVP_OP_ATOMIC_RMW2] = OP_INFO_INIT(rvp_rmw1_2_t, "atomic rmw 2")
	, [RVP_OP_ATOMIC_RMW4] = OP_INFO_INIT(rvp_rmw4_t, "atomic rmw 4")
	, [RVP_OP_ATOMIC_RMW8] = OP_INFO_INIT(rvp_rmw8_t, "atomic rmw 8")

	, [RVP_OP_ATOMIC_LOAD1] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t,
					       "atomic load 1")
	, [RVP_OP_ATOMIC_STORE1] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t,
						"atomic store 1")
	, [RVP_OP_ATOMIC_LOAD2] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t,
					       "atomic load 2")
	, [RVP_OP_ATOMIC_STORE2] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t,
						"atomic store 2")
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
	, [RVP_OP_SIGSETMASK] =
	    OP_INFO_INIT(rvp_sigmask_access_t, "set signal mask")
	, [RVP_OP_SIGGETMASK] =
	    OP_INFO_INIT(rvp_sigmask_access_t, "get signal mask")
	, [RVP_OP_SIGGETSETMASK] = OP_INFO_INIT(rvp_sigmask_rmw_t,
	    "get & set signal mask")
	, [RVP_OP_SIGBLOCK] =
	    OP_INFO_INIT(rvp_sigmask_access_t, "block signals")
	, [RVP_OP_SIGUNBLOCK] =
	    OP_INFO_INIT(rvp_sigmask_access_t, "unblock signals")
};

static const rvp_emitters_t binary = {
	  .init = reemit_binary_init
	, .emit_nop = reemit_binary_nop
	, .emit_op = reemit_binary_op
	, .dataptr_to_string = NULL
	, .insnptr_to_string = NULL
};

static const rvp_emitters_t plain_text = {
	  .emit_nop = print_nop
	, .emit_op = print_op
	, .dataptr_to_string = dataptr_to_simple_string
	, .insnptr_to_string = insnptr_to_simple_string
};

static const rvp_emitters_t symbol_friendly = {
	  .emit_nop = print_nop
	, .emit_op = print_op
	, .dataptr_to_string = dataptr_to_symbol_friendly_string
	, .insnptr_to_string = insnptr_to_symbol_friendly_string
};

static void
rvp_callstack_expand(rvp_callstack_t *cs)
{
	int i, nframes;
	rvp_frame_t *frame;

	nframes = MAX(8, 2 * cs->cs_nframes);

	frame = realloc(cs->cs_frame, nframes * sizeof(*cs->cs_frame));

	if (frame == NULL)
		errx(EXIT_FAILURE, "%s: calloc", __func__);
	for (i = cs->cs_nframes; i < nframes; i++)
		frame[i] = (rvp_frame_t){0, 0};
	cs->cs_frame = frame;
	cs->cs_nframes = nframes;
}

static void
rvp_callstack_push(rvp_callstack_t *cs, rvp_addr_t pc, rvp_addr_t cfa)
{
	if (cs->cs_depth == cs->cs_nframes)
		rvp_callstack_expand(cs);
	cs->cs_frame[cs->cs_depth++] = (rvp_frame_t){.f_pc = pc, .f_cfa = cfa};
}

static void
rvp_callstack_pop(rvp_callstack_t *cs)
{
	assert(cs->cs_depth > 0);
	--cs->cs_depth;
}

static void
extract_jmpvec_and_op_from_deltop(rvp_addr_t deltop0,
    rvp_addr_t pc, int *jmpvecp, rvp_op_t *opp)
{
	/* XXX it's not strictly necessary for deltops to have any concrete
	 * storage
	 */
	deltops_t deltops;

	int row = (pc - deltop0) / __arraycount(deltops.matrix[0]);

	int col = (pc - deltop0) -
	    (&deltops.matrix[row][0] - &deltops.matrix[0][0]);
	int jmpvec = col - RVP_NJMPS / 2;

	assert(row < RVP_NOPS);

	*opp = row;
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
	int i;

	if (ps->ps_nthreads <= tid) {
		rvp_pstate_extend_threads_over(ps, tid, ps->ps_thread,
		    ps->ps_nthreads);
	}
	ps->ps_curthread = tid;
	ps->ps_idepth = 0;
	rvp_thread_pstate_t *ts = &ps->ps_thread[tid];
	assert(!ts->ts_present);
	for (i = 0; i < __arraycount(ts->ts_lastpc); i++)
		ts->ts_lastpc[i] = ps->ps_deltop_center;
	ts->ts_callstack[0] = ts->ts_callstack[1] = (rvp_callstack_t){NULL, 0};
	ts->ts_present = true;
	ts->ts_generation[0] = generation;
	ts->ts_generation[1] = 0;
	ts->ts_nops[ps->ps_idepth] = 0;
	ts->ts_sigs_masked = false;
}

static void
rvp_pstate_init(rvp_pstate_t *ps, const rvp_emitters_t *emitters,
    bool emit_generation, bool emit_bytes, rvp_addr_t op0)
{
	/* XXX it's not strictly necessary for deltops to have any concrete
	 * storage
	 */
	deltops_t deltops;

	ps->ps_emit_generation = emit_generation;
	ps->ps_emit_bytes = emit_bytes;
	ps->ps_emitters = emitters;

	ps->ps_deltop_center = op0;
	ps->ps_deltop_first = op0 -
	    (&deltops.matrix[RVP_OP_BEGIN][RVP_NJMPS / 2] -
	     &deltops.matrix[0][0]);
	ps->ps_deltop_last = ps->ps_deltop_first +
	    (&deltops.matrix[RVP_NOPS - 1][RVP_NJMPS - 1] -
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
reemit_binary_init(const rvp_pstate_t *ps __unused,
    const rvp_trace_header_t *th)
{
	const ssize_t nwritten = write(STDOUT_FILENO, th, sizeof(*th));

	if (nwritten != sizeof(*th))
		err(EXIT_FAILURE, "%s: write", __func__);
}

static void
reemit_binary_nop(const rvp_pstate_t *ps __unused, const rvp_ubuf_t *ub)
{
	const ssize_t nwritten =
	    write(STDOUT_FILENO, &ub->ub_pc, sizeof(ub->ub_pc));

	if (nwritten != sizeof(ub->ub_pc))
		err(EXIT_FAILURE, "%s: write", __func__);
}

static void
reemit_binary_op(const rvp_pstate_t *ps __unused, const rvp_ubuf_t *ub,
    rvp_op_t op, bool is_load __unused, int field_width __unused)
{
	const op_info_t *oi = &op_to_info[op];
	const ssize_t nwritten = write(STDOUT_FILENO, ub, oi->oi_reclen);

	if (nwritten != oi->oi_reclen)
		err(EXIT_FAILURE, "%s: write", __func__);
}

static char *
suffix_string(const rvp_pstate_t *ps, const rvp_ubuf_t *ub, size_t reclen,
    char *buf0, size_t buflen0)
{
	char *buf = buf0;
	size_t buflen = buflen0;
	int i;
	const char *delim = " ";

	if (!ps->ps_emit_bytes)
		return "";

	for (i = 0; i < reclen; i++) {
		int nwritten = snprintf(buf, buflen,
		    "%s%02" PRIx8, delim, (uint8_t)ub->ub_bytes[i]);
		if (nwritten < 0 || nwritten >= buflen) {
			errx(EXIT_FAILURE, "%s.%d: snprintf failed",
			    __func__, __LINE__);
		}
		buf += nwritten;
		buflen -= nwritten;
		delim = ".";
	}
	return buf0;
}

static void
print_nop(const rvp_pstate_t *ps, const rvp_ubuf_t *ub)
{
	prebuf_t prebuf;
	char sufbuf[128];

	printf("%s nop%s\n", preamble_string(ps, prebuf, sizeof(prebuf)),
	    suffix_string(ps, ub, sizeof(ub->ub_pc), sufbuf, sizeof(sufbuf)));
}

static void
rvp_callstack_nearest_frames(const rvp_callstack_t *cs, rvp_addr_t addr,
    const rvp_frame_t **loframep, const rvp_frame_t **hiframep)
{
	int i;

	for (i = 0; i < cs->cs_depth; i++) {
		rvp_frame_t *f = &cs->cs_frame[i];
		if (addr <= f->f_cfa) {
			if (*hiframep == NULL || f->f_cfa < (*hiframep)->f_cfa)
				*hiframep = f;
		}
		if (f->f_cfa < addr) {
			if (*loframep == NULL || (*loframep)->f_cfa < f->f_cfa)
				*loframep = f;
		}
	}
}

static void
rvp_nearest_frames(const rvp_pstate_t *ps, rvp_addr_t addr,
    const rvp_frame_t **loframep, const rvp_frame_t **hiframep)
{
	rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
	rvp_callstack_t *cs = &ts->ts_callstack[ps->ps_idepth];

	*loframep = *hiframep = NULL;
	rvp_callstack_nearest_frames(cs, addr, loframep, hiframep);
}

char *
insnptr_to_symbol_friendly_string(const rvp_pstate_t *ps __unused,
    char *buf, size_t buflen, rvp_addr_t addr)
{
	const int nwritten = snprintf(buf, buflen, "{%#016" PRIxPTR "}", addr);

	if (nwritten < 0 || nwritten >= buflen)
		errx(EXIT_FAILURE, "%s: snprintf failed", __func__);

	return buf;
}

static char *
lastpc_to_string(const rvp_pstate_t *ps, char *buf, size_t buflen)
{
	const rvp_emitters_t *emitters = ps->ps_emitters;

	return (*emitters->insnptr_to_string)(ps, buf, buflen,
	    ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_idepth]);
}

char *
preamble_string(const rvp_pstate_t *ps, char *buf0, size_t buflen0)
{
	char *buf = buf0;
	size_t buflen = buflen0;
	int nwritten = snprintf(buf, buflen,
	    "tid %" PRIu32 ".%" PRIu32 " ", ps->ps_curthread, ps->ps_idepth);

	if (nwritten < 0 || nwritten >= buflen) {
		errx(EXIT_FAILURE, "%s.%d: snprintf failed",
		    __func__, __LINE__);
	}

	buf += nwritten;
	buflen -= nwritten;

	if (ps->ps_emit_generation) {
		rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];

		nwritten = snprintf(buf, buflen, "gen %" PRIu64 " ",
		    ts->ts_generation[ps->ps_idepth]);

		if (nwritten < 0 || nwritten >= buflen) {
			errx(EXIT_FAILURE, "%s.%d: snprintf failed",
			    __func__, __LINE__);
		}
		buf += nwritten;
		buflen -= nwritten;
	}

	lastpc_to_string(ps, buf, buflen);

	return buf0;
}

char *
insnptr_to_simple_string(const rvp_pstate_t *ps __unused,
    char *buf, size_t buflen, rvp_addr_t addr)
{
	const int nwritten = snprintf(buf, buflen, "pc %#016" PRIxPTR, addr);

	if (nwritten < 0 || nwritten >= buflen)
		errx(EXIT_FAILURE, "%s: snprintf failed", __func__);

	return buf;
}

char *
dataptr_to_simple_string(const rvp_pstate_t *ps __unused,
    char *buf, size_t buflen, rvp_addr_t addr)
{
	const int nwritten = snprintf(buf, buflen, "[%#016" PRIxPTR "]", addr);

	if (nwritten < 0 || nwritten >= buflen)
		errx(EXIT_FAILURE, "%s: snprintf failed", __func__);

	return buf;
}

char *
dataptr_to_symbol_friendly_string(const rvp_pstate_t *ps,
    char *buf, size_t buflen, rvp_addr_t addr)
{
	const rvp_frame_t *loframe, *hiframe;
	int nwritten;

	rvp_nearest_frames(ps, addr, &loframe, &hiframe);

	if (loframe != NULL && hiframe != NULL) {
		nwritten = snprintf(buf, buflen, "[%#016" PRIxPTR " : "
		    "%#" PRIxPTR "/%#" PRIxPTR
		    " %#" PRIxPTR "/%#" PRIxPTR "]", addr,
		    loframe->f_pc, loframe->f_cfa,
		    hiframe->f_pc, hiframe->f_cfa);
	} else if (loframe != NULL) {
		nwritten = snprintf(buf, buflen, "[%#016" PRIxPTR " : "
		"%#" PRIxPTR "/%#" PRIxPTR "]", addr,
		loframe->f_pc, loframe->f_cfa);
	} else if (hiframe != NULL) {
		nwritten = snprintf(buf, buflen, "[%#016" PRIxPTR " : "
		"%#" PRIxPTR "/%#" PRIxPTR "]", addr,
		hiframe->f_pc, hiframe->f_cfa);
	} else
		nwritten = snprintf(buf, buflen, "[%#016" PRIxPTR "]", addr);

	if (nwritten < 0 || nwritten >= buflen)
		errx(EXIT_FAILURE, "%s: snprintf failed", __func__);

	return buf;
}

static void
print_op(const rvp_pstate_t *ps, const rvp_ubuf_t *ub, rvp_op_t op,
    bool is_load, int field_width)
{
	const op_info_t *oi = &op_to_info[op];
	const rvp_emitters_t *emitters = ps->ps_emitters;
	char buf[3][MAX(sizeof("{0x0123456789abcdef}"),
			sizeof("[0x0123456789abcdef : "
			       "0x0123456789abcdef/0x0123456789abcdef "
			       "0x0123456789abcdef/0x0123456789abcdef]"))];
	prebuf_t prebuf;
	char sufbuf[128];

	switch (op) {
	case RVP_OP_ATOMIC_LOAD8:
	case RVP_OP_ATOMIC_STORE8:
	case RVP_OP_LOAD8:
	case RVP_OP_STORE8:
		printf("%s %s %#.*" PRIx64 " %s %s%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, field_width,
		    ub->ub_load8_store8.data,
		    is_load ? "<-" : "->",
		    (*emitters->dataptr_to_string)(ps, buf[1], sizeof(buf[1]),
		        ub->ub_load8_store8.addr),
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_ATOMIC_RMW8:
		printf("%s %s %#.*" PRIx64 " <- %s <- %#.*" PRIx64 "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, field_width, ub->ub_rmw8.oval,
		    (*emitters->dataptr_to_string)(ps, buf[1], sizeof(buf[1]),
		        ub->ub_rmw8.addr),
		    field_width, ub->ub_rmw8.nval,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_ATOMIC_RMW4:
		printf("%s %s %#.*" PRIx32 " <- %s <- %#.*" PRIx32 "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, field_width, ub->ub_rmw4.oval,
		    (*emitters->dataptr_to_string)(ps, buf[1], sizeof(buf[1]),
		        ub->ub_rmw4.addr),
		    field_width, ub->ub_rmw4.nval,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_ATOMIC_RMW2:
	case RVP_OP_ATOMIC_RMW1:
		printf("%s %s %#.*" PRIxMAX " <- %s <- %#.*" PRIxMAX "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, field_width,
		    __SHIFTOUT(ub->ub_rmw1_2.onval, __BITS(15, 0)),
		    (*emitters->dataptr_to_string)(ps, buf[1], sizeof(buf[1]),
		        ub->ub_rmw1_2.addr),
		    field_width,
		    __SHIFTOUT(ub->ub_rmw1_2.onval, __BITS(31, 16)),
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
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
		printf("%s %s %#.*" PRIx32
		    " %s %s%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, field_width,
		    ub->ub_load1_2_4_store1_2_4.data,
		    is_load ? "<-" : "->",
		    (*emitters->dataptr_to_string)(ps, buf[1], sizeof(buf[1]),
		        ub->ub_load1_2_4_store1_2_4.addr),
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_BEGIN:
		printf("%s %s generation %" PRIu64 "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, ub->ub_begin.generation,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_SIGDEPTH:
		printf("%s %s -> %" PRIu32 "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, ub->ub_sigdepth.depth,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_COG:
		printf("%s %s -> %" PRIu64 "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, ub->ub_cog.generation,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_ENTERFN:
		printf("%s %s cfa %" PRIxPTR " return %s%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, ub->ub_enterfn.cfa,
		    (*emitters->insnptr_to_string)(ps, buf[1], sizeof(buf[1]),
		        ub->ub_enterfn.callsite),
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_END:
	default:
		printf("%s %s%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)), oi->oi_descr,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_FORK:
	case RVP_OP_JOIN:
	case RVP_OP_SWITCH:
		printf("%s %s tid %" PRIu32 "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, ub->ub_fork_join_switch.tid,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		// TBD create a fledgling rvp_thread_pstate_t on fork?
		break;
	case RVP_OP_ENTERSIG:
		printf(
		    "%s %s signal %" PRIu32
		    " handler %s generation %" PRIu64 "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, ub->ub_entersig.signum,
		    (*emitters->insnptr_to_string)(ps, buf[1], sizeof(buf[1]),
		        ub->ub_entersig.handler),
		    ub->ub_entersig.generation,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_ACQUIRE:
	case RVP_OP_RELEASE:
		printf("%s %s %s%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr,
		    (*emitters->dataptr_to_string)(ps, buf[1], sizeof(buf[1]),
		        ub->ub_acquire_release.addr),
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_SIGDIS:
		printf("%s %s signal %" PRIu32 "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, ub->ub_sigdis.signum,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_SIGEST:
		printf("%s %s signal %" PRIu32
		    " handler %s mask #%" PRIu32 "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr,
		    ub->ub_sigest.signum,
		    (*emitters->insnptr_to_string)(ps, buf[1], sizeof(buf[1]),
		        ub->ub_sigest.handler),
		    ub->ub_sigest.masknum,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_SIGMASKMEMO:
		printf("%s %s #%" PRIu32 " origin %" PRIu32 " bits %#016" PRIx64 "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr,
		    ub->ub_sigmaskmemo.masknum,
		    ub->ub_sigmaskmemo.origin,
		    ub->ub_sigmaskmemo.mask,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_SIGGETMASK:
	case RVP_OP_SIGSETMASK:
	case RVP_OP_SIGBLOCK:
	case RVP_OP_SIGUNBLOCK:
		printf("%s %s #%" PRIu32 "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr, ub->ub_sigmask_access.masknum,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	case RVP_OP_SIGGETSETMASK:
		printf("%s %s #%" PRIu32 " -> #%" PRIu32 "%s\n",
		    preamble_string(ps, prebuf, sizeof(prebuf)),
		    oi->oi_descr,
		    ub->ub_sigmask_rmw.omasknum,
		    ub->ub_sigmask_rmw.masknum,
		    suffix_string(ps, ub, oi->oi_reclen,
		        sufbuf, sizeof(sufbuf)));
		break;
	}
}

/*
 * If the operation code indicates a trace that is longer than `*nfullp`
 * bytes, consume nothing and return the number of bytes that the buffer is
 * short of a full trace.
 *
 * Otherwise, consume the trace at the start of the buffer `ub`, print
 * the trace, shift the bytes of the buffer left by the number of bytes
 * consumed, and return 0.
 */
static size_t
consume_and_print_trace(rvp_pstate_t *ps, rvp_ubuf_t *ub, size_t *nfullp)
{
	const rvp_emitters_t *emitters = ps->ps_emitters;
	rvp_op_t op;
	rvp_addr_t lastpc, pc;
	int jmpvec;
	bool is_load = false;
	int field_width = 0;

	if (pc_is_not_deltop(ps, ub->ub_pc)) {
		ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_idepth] =
		    ub->ub_pc;
		(*emitters->emit_nop)(ps, ub);
		advance(&ub->ub_bytes[0], nfullp, sizeof(ub->ub_pc));
		return 0;
	}
	extract_jmpvec_and_op_from_deltop(ps->ps_deltop_first,
	    ub->ub_pc, &jmpvec, &op);
	const op_info_t * const oi = &op_to_info[op];
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
		ts->ts_generation[ps->ps_idepth] =
		    ub->ub_entersig.generation;
	}
	assert(op != RVP_OP_ENTERSIG || ps->ps_idepth != 0);

	lastpc = ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_idepth];
	pc = lastpc + jmpvec;
	ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_idepth] = pc;

	if (op == RVP_OP_ENTERFN) {
		rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
		rvp_callstack_t *cs = &ts->ts_callstack[ps->ps_idepth];
		rvp_callstack_push(cs, pc, ub->ub_enterfn.cfa);
	} else if (op == RVP_OP_EXITFN) {
		rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
		rvp_callstack_t *cs = &ts->ts_callstack[ps->ps_idepth];
		rvp_callstack_pop(cs);
	}

	switch (op) {
	case RVP_OP_ATOMIC_LOAD8:
	case RVP_OP_ATOMIC_STORE8:
	case RVP_OP_ATOMIC_RMW8:
	case RVP_OP_LOAD8:
	case RVP_OP_STORE8:
		field_width = 16;
		break;
	case RVP_OP_ATOMIC_LOAD4:
	case RVP_OP_ATOMIC_STORE4:
	case RVP_OP_ATOMIC_RMW4:
	case RVP_OP_LOAD4:
	case RVP_OP_STORE4:
		field_width = 8;
		break;
	case RVP_OP_ATOMIC_LOAD2:
	case RVP_OP_ATOMIC_STORE2:
	case RVP_OP_ATOMIC_RMW2:
	case RVP_OP_LOAD2:
	case RVP_OP_STORE2:
		field_width = 4;
		break;
	case RVP_OP_ATOMIC_LOAD1:
	case RVP_OP_ATOMIC_STORE1:
	case RVP_OP_ATOMIC_RMW1:
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
		assert(ps->ps_idepth < __arraycount(ts->ts_generation));
		assert(ts->ts_generation[ps->ps_idepth] <
		    ub->ub_cog.generation);
		ts->ts_generation[ps->ps_idepth] = ub->ub_cog.generation;
		ts->ts_nops[ps->ps_idepth] = 0;
	}

	if (op == RVP_OP_END) {
		rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
		ts->ts_present = false;
	}

	if (op == RVP_OP_SWITCH) {
		ps->ps_curthread = ub->ub_fork_join_switch.tid;
		ps->ps_idepth = 0;
	} else if (op == RVP_OP_EXITSIG) {
		assert(ps->ps_idepth != 0);
	} else if (op == RVP_OP_SIGDEPTH) {
		rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];

		assert(ub->ub_sigdepth.depth <
		    __arraycount(ts->ts_generation));
		ps->ps_idepth = ub->ub_sigdepth.depth;
	}

	advance(&ub->ub_bytes[0], nfullp, oi->oi_reclen);
	return 0;
}

void
rvp_trace_dump_with_emitters(bool emit_generation, bool emit_bytes,
    size_t most_nrecords, const rvp_emitters_t *emitters, int fd)
{
	int cmp;
	rvp_pstate_t ps0;
	rvp_pstate_t *ps = &ps0;
	rvp_trace_header_t th;
 
	/* XXX make a compile-time assertion that this is a little-endian
	 * machine, since we assume that when we read the initial 'begin'
	 * operation, below.
	 */
	const rvp_trace_header_t expected_th = {
		  .th_magic = "RVP_"
		, .th_version = {0, 0, 0, 4}
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
	struct iovec scratch[__arraycount(iov)];

	if ((nread = readallv(fd, iov, scratch, __arraycount(iov), NULL)) == -1)
		err(EXIT_FAILURE, "%s: readallv(header)", __func__);

	if (nread < iovsum(iov, __arraycount(iov))) {
		errx(EXIT_FAILURE,
		    "%s: read %zd bytes, expected %zd (header + 1st deltop + ggen init)",
		    __func__, nread, iovsum(iov, __arraycount(iov)));
	}

	cmp = memcmp(th.th_magic, expected_th.th_magic, sizeof(th.th_magic));
	if (cmp != 0) {
		errx(EXIT_FAILURE, "%s: bad magic %4s", __func__,
		    th.th_magic);
	}
	cmp = memcmp(th.th_version, expected_th.th_version,
	    sizeof(th.th_version));
	if (cmp != 0) {
		errx(EXIT_FAILURE, "%s: unsupported version %d.%d.%d.%d",
		    __func__, th.th_version[0], th.th_version[1],
		    th.th_version[2], th.th_version[3]);
	}
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
	rvp_pstate_init(ps, emitters, emit_generation, emit_bytes, pc0);

	if (emitters->init != NULL)
		(*emitters->init)(ps, &th);

	ub.ub_begin = (rvp_begin_t){.deltop = pc0, .tid = tid};

	size_t nrecords = 0;
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
			if (nrecords == most_nrecords)
				return;
			nshort = consume_and_print_trace(ps, &ub, &nfull);
			if (nshort != 0)
				break;
			++nrecords;
		} while (nfull >= sizeof(ub.ub_pc));
	}
}

void
rvp_trace_dump(const rvp_output_params_t *op, int fd)
{
	const rvp_emitters_t *emitters;
	bool emit_generation = op->op_emit_generation;
	bool emit_bytes = op->op_emit_bytes;
	size_t most_nrecords = op->op_nrecords;
	switch (op->op_type) {
	case RVP_OUTPUT_BINARY:
		emitters = &binary;
		break;
	case RVP_OUTPUT_SYMBOL_FRIENDLY:
		emitters = &symbol_friendly;
		break;
	case RVP_OUTPUT_PLAIN_TEXT:
		emitters = &plain_text;
		break;
	default:
		errx(EXIT_FAILURE, "%s: unknown output type %d", __func__,
		     op->op_type);
	}

	rvp_trace_dump_with_emitters(emit_generation, emit_bytes, most_nrecords,
	    emitters, fd);
}
