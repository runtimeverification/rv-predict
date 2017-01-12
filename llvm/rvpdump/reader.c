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

#include "nbcompat.h"	/* for __arraycount */
#include "tracefmt.h"
#include "reader.h"

typedef struct {
	uintptr_t deltop;
	uint32_t tid;
} __packed __aligned(sizeof(uint32_t)) begin_fork_join_switch_t;

typedef struct {
	uintptr_t deltop;
	uintptr_t addr;
	uint32_t data;
} __packed __aligned(sizeof(uint32_t)) load4_store4_t;

typedef struct {
	uintptr_t deltop;
	uintptr_t addr;
	uint64_t data;
} __packed __aligned(sizeof(uint32_t)) load8_store8_t;

typedef struct {
	uintptr_t deltop;
	uintptr_t addr;
	uint32_t data;
} __packed __aligned(sizeof(uint32_t)) atomic_load4_store4_t;

typedef struct {
	uintptr_t deltop;
	uintptr_t addr;
	uint64_t data;
} __packed __aligned(sizeof(uint32_t)) atomic_load8_store8_t;

typedef struct {
	uintptr_t deltop;
} __packed __aligned(sizeof(uint32_t)) end_enterfn_exitfn_t;

typedef struct {
	size_t oi_reclen;
	const char *oi_descr;
} op_info_t;

#define OP_INFO_INIT(__ty, __descr) {.oi_reclen = sizeof(__ty), .oi_descr = __descr}

static const op_info_t op_to_info[RVP_NOPS] = {
	  [RVP_OP_ENTERFN] = OP_INFO_INIT(end_enterfn_exitfn_t, "enter function")
	, [RVP_OP_EXITFN] = OP_INFO_INIT(end_enterfn_exitfn_t, "exit function")

	, [RVP_OP_BEGIN] = OP_INFO_INIT(begin_fork_join_switch_t,
					"begin thread")
	, [RVP_OP_SWITCH] = OP_INFO_INIT(begin_fork_join_switch_t,
					 "switch thread")
	, [RVP_OP_FORK] = OP_INFO_INIT(begin_fork_join_switch_t, "fork thread")
	, [RVP_OP_JOIN] = OP_INFO_INIT(begin_fork_join_switch_t, "join thread")

	, [RVP_OP_LOAD4] = OP_INFO_INIT(load4_store4_t, "load 4")
	, [RVP_OP_STORE4] = OP_INFO_INIT(load4_store4_t, "store 4")
	, [RVP_OP_LOAD8] = OP_INFO_INIT(load8_store8_t, "load 8")
	, [RVP_OP_STORE8] = OP_INFO_INIT(load8_store8_t, "store 8")

	, [RVP_OP_ATOMIC_LOAD4] = OP_INFO_INIT(load4_store4_t, "atomic load 4")
	, [RVP_OP_ATOMIC_STORE4] = OP_INFO_INIT(load4_store4_t, "atomic store 4")
	, [RVP_OP_ATOMIC_LOAD8] = OP_INFO_INIT(atomic_load8_store8_t, "atomic load 8")
	, [RVP_OP_ATOMIC_STORE8] = OP_INFO_INIT(atomic_load8_store8_t, "atomic store 8")
};

typedef struct _rvp_call {
	uintptr_t	*cs_funcs;
	int		cs_depth;
} rvp_callstack_t;

/* parse state: per-thread */
typedef struct _rvp_thread_pstate {
	uintptr_t	ts_lastpc;
	rvp_callstack_t	ts_callstack;
	bool		ts_present;
} rvp_thread_pstate_t;

/* parse state: global */
typedef struct _rvp_pstate {
	rvp_thread_pstate_t	*ps_thread;
	uint32_t		ps_nthreads;
	uintptr_t		ps_deltop_first, ps_deltop_last;
	uint32_t		ps_curthread;
} rvp_pstate_t;

static void
extract_jmpvec_and_op_from_deltop(uintptr_t deltop0,
    uintptr_t pc, int *jmpvecp, rvp_op_t *opp)
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
	int i;
	rvp_thread_pstate_t *ts;

	assert(tid >= onthreads);

	for (ps->ps_nthreads = MAX(8, onthreads);
	     tid >= ps->ps_nthreads;
	     ps->ps_nthreads *= 2)
		;	/* do nothing */

	ps->ps_thread = malloc(sizeof(rvp_thread_pstate_t) * ps->ps_nthreads);
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
	ts = &ps->ps_thread[tid];
	assert(!ts->ts_present);
	ts->ts_lastpc = ps->ps_deltop_first;
	ts->ts_callstack = (rvp_callstack_t){NULL, 0};
	ts->ts_present = true;
	if (othread != NULL)
		free(othread);
}

static void
rvp_pstate_begin_thread(rvp_pstate_t *ps, uint32_t tid)
{
	if (ps->ps_nthreads <= tid) {
		rvp_pstate_extend_threads_over(ps, tid, ps->ps_thread,
		    ps->ps_nthreads);
	}
	ps->ps_curthread = tid;
}

static void
rvp_pstate_init(rvp_pstate_t *ps, uintptr_t op0, uint32_t tid)
{
	/* XXX it's not strictly necessary for deltops to have any concrete
	 * storage
	 */
	deltops_t deltops;

	ps->ps_deltop_first = op0 -
	    (&deltops.matrix[RVP_NJMPS / 2][RVP_OP_BEGIN] -
	     &deltops.matrix[0][0]);
	ps->ps_deltop_last = ps->ps_deltop_first +
	    (&deltops.matrix[RVP_NJMPS - 1][RVP_NOPS - 1] -
	     &deltops.matrix[0][0]);
	ps->ps_thread = NULL;
	ps->ps_nthreads = 0;

	rvp_pstate_begin_thread(ps, tid);
}

static size_t
iovsum(const struct iovec *iov, int iovcnt)
{
	int i;
	size_t sum = 0;

	for (i = 0; i < iovcnt; i++)
		sum += iov[i].iov_len;
	
	return sum;
}

static inline bool
pc_is_not_deltop(rvp_pstate_t *ps, uintptr_t pc)
{
	return pc < ps->ps_deltop_first || ps->ps_deltop_last < pc;
}

void
rvp_trace_dump(int fd)
{
	rvp_pstate_t ps0;
	rvp_pstate_t *ps = &ps0;
	rvp_trace_header_t th;
	const rvp_trace_header_t expected_th = {
		  .th_magic = "RVP_"
		, .th_version = 0
		, .th_byteorder = '0' | ('1' << 8) | ('2' << 16) | ('3' << 24)
		, .th_pointer_width = sizeof(uintptr_t)
		, .th_data_width = sizeof(uint32_t)
		, .th_pad1 = { 0 }
	};
	uintptr_t pc0;
	uint32_t tid;
	ssize_t nread;
	union {
		uintptr_t un_pc;
		begin_fork_join_switch_t un_begin_fork_join_switch;
		load4_store4_t un_load4_store4;
		load8_store8_t un_load8_store8;
		char un_bytes[4096];
	} un;
	const struct iovec iov[] = {
		  { .iov_base = &th, .iov_len = sizeof(th) }
		, { .iov_base = &pc0 , .iov_len = sizeof(pc0) }
		, { .iov_base = &tid , .iov_len = sizeof(tid) }
	};

	if ((nread = readv(fd, iov, __arraycount(iov))) == -1)
		err(EXIT_FAILURE, "%s: readv(header)", __func__);

	if (nread < iovsum(iov, __arraycount(iov))) {
		errx(EXIT_FAILURE, "%s: short read (header + 1st deltop)",
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

	rvp_pstate_init(ps, pc0, tid);

	un.un_begin_fork_join_switch = (begin_fork_join_switch_t){.deltop = pc0,
								  .tid = tid};
	size_t nfull = sizeof(un.un_begin_fork_join_switch);
	for (;;) {
		rvp_op_t op;
		int jmpvec;

		nread = read(fd, (char *)&un + nfull, sizeof(un) - nfull);
		if (nread == -1) {
			err(EXIT_FAILURE, "%s: read failed (pc/deltop)",
			    __func__);
		}
		nfull += nread;
		if (nfull == 0)
			break;
		if (nfull < sizeof(un.un_pc)) {
			errx(EXIT_FAILURE, "%s: short read (pc/deltop)",
			    __func__);
		}
		do {
			if (pc_is_not_deltop(ps, un.un_pc)) {
				ps->ps_thread[tid].ts_lastpc = un.un_pc;
				printf("jump to %016" PRIxPTR "\n", un.un_pc);
				advance(&un.un_bytes[0], &nfull,
				    sizeof(un.un_pc));
				continue;
			}
			extract_jmpvec_and_op_from_deltop(ps->ps_deltop_first,
			    un.un_pc, &jmpvec, &op);
			const op_info_t *oi  = &op_to_info[op];
			if (oi->oi_descr == NULL) {
				errx(EXIT_FAILURE, "unknown op %d\n", op);
				return;
			}
			if (nfull < oi->oi_reclen)
				break;
			printf("%s (%016" PRIxPTR ", reclen %zu)\n",
			    oi->oi_descr, un.un_pc, oi->oi_reclen);
			advance(&un.un_bytes[0], &nfull, oi->oi_reclen);
			ps->ps_thread[tid].ts_lastpc += jmpvec;
		} while (nfull < sizeof(un.un_pc));
	}
}
