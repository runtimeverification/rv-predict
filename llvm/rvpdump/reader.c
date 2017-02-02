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
#include "legacy.h"
#include "reader.h"

typedef union {
	uintptr_t ub_pc;
	rvp_begin_t ub_begin;
	rvp_fork_join_switch_t ub_fork_join_switch;
	rvp_load1_2_4_store1_2_4_t ub_load1_2_4_store1_2_4;
	rvp_acquire_release_t ub_acquire_release;
	rvp_load8_store8_t ub_load8_store8;
	rvp_cog_t ub_cog;
	char ub_bytes[4096];
} rvp_ubuf_t;

typedef struct {
	size_t oi_reclen;
	const char *oi_descr;
} op_info_t;

#define OP_INFO_INIT(__ty, __descr) {.oi_reclen = sizeof(__ty), .oi_descr = __descr}

static const op_info_t op_to_info[RVP_NOPS] = {
	  [RVP_OP_ENTERFN] = OP_INFO_INIT(rvp_end_enterfn_exitfn_t,
	     "enter function")
	, [RVP_OP_EXITFN] = OP_INFO_INIT(rvp_end_enterfn_exitfn_t,
	    "exit function")

	, [RVP_OP_BEGIN] = OP_INFO_INIT(rvp_begin_t, "begin thread")
	, [RVP_OP_COG] = OP_INFO_INIT(rvp_cog_t, "change of generation")
	, [RVP_OP_END] = OP_INFO_INIT(rvp_end_enterfn_exitfn_t, "end thread")
	, [RVP_OP_SWITCH] = OP_INFO_INIT(rvp_fork_join_switch_t,
					 "switch thread")
	, [RVP_OP_FORK] = OP_INFO_INIT(rvp_fork_join_switch_t,
	    "fork thread")
	, [RVP_OP_JOIN] = OP_INFO_INIT(rvp_fork_join_switch_t,
	    "join thread")

	, [RVP_OP_LOAD1] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "load 1")
	, [RVP_OP_STORE1] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "store 1")
	, [RVP_OP_LOAD2] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "load 2")
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
};

typedef struct _rvp_call {
	uintptr_t	*cs_funcs;
	int		cs_depth;
} rvp_callstack_t;

struct _rvp_pstate;
typedef struct _rvp_pstate rvp_pstate_t;

typedef struct _rvp_emitters {
	void (*emit_jump)(const rvp_pstate_t *, uintptr_t);
	void (*emit_op)(const rvp_pstate_t *, const rvp_ubuf_t *, rvp_op_t,
	    bool, int);
} rvp_emitters_t;

/* parse state: per-thread */
typedef struct _rvp_thread_pstate {
	uintptr_t	ts_lastpc;
	rvp_callstack_t	ts_callstack;
	bool		ts_present;
	uint64_t	ts_generation;
} rvp_thread_pstate_t;

/* parse state: global */
typedef struct _rvp_pstate {
	rvp_thread_pstate_t	*ps_thread;
	uint32_t		ps_nthreads;
	uintptr_t		ps_deltop_first, ps_deltop_last;
	uint32_t		ps_curthread;
	const rvp_emitters_t	*ps_emitters;
} rvp_pstate_t;

static void emit_no_jump(const rvp_pstate_t *, uintptr_t);
static void emit_legacy_op(const rvp_pstate_t *, const rvp_ubuf_t *, rvp_op_t,
    bool, int);

static void print_jump(const rvp_pstate_t *, uintptr_t);
static void print_op(const rvp_pstate_t *, const rvp_ubuf_t *, rvp_op_t,
    bool, int);

static const rvp_emitters_t plain_text = {
	  .emit_jump = print_jump
	, .emit_op = print_op
};

static const rvp_emitters_t legacy_binary = {
	  .emit_jump = emit_no_jump
	, .emit_op = emit_legacy_op
};

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
rvp_pstate_begin_thread(rvp_pstate_t *ps, uint32_t tid, uint64_t generation)
{
	if (ps->ps_nthreads <= tid) {
		rvp_pstate_extend_threads_over(ps, tid, ps->ps_thread,
		    ps->ps_nthreads);
	}
	ps->ps_curthread = tid;
	ps->ps_thread[tid].ts_generation = generation;
}

static void
rvp_pstate_init(rvp_pstate_t *ps, const rvp_emitters_t *emitters, uintptr_t op0,
    uint32_t tid, uint64_t generation)
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

	rvp_pstate_begin_thread(ps, tid, generation);
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

static void
emit_no_jump(const rvp_pstate_t *ps, uintptr_t pc)
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
	case RVP_OP_ATOMIC_LOAD8:
	case RVP_OP_ATOMIC_LOAD4:
	case RVP_OP_ATOMIC_LOAD2:
	case RVP_OP_ATOMIC_LOAD1:
		return READ; // ATOMIC_READ
	case RVP_OP_LOAD8:
	case RVP_OP_LOAD4:
	case RVP_OP_LOAD2:
	case RVP_OP_LOAD1:
		return READ;
	case RVP_OP_ATOMIC_STORE8:
	case RVP_OP_ATOMIC_STORE4:
	case RVP_OP_ATOMIC_STORE2:
	case RVP_OP_ATOMIC_STORE1:
		return WRITE; // ATOMIC_WRITE
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

static uint32_t
compress_pc(uint64_t pc)
{
	static int ninitialized = 0;
	static uint32_t lasthi[2];
	uint32_t newhi, newlo;
	int i;

	newhi = (uint32_t)(pc >> 32);
	newlo = (uint32_t)(pc & 0xffffffff);
	if ((pc & 0x80000000) != 0)
		errx(EXIT_FAILURE, "%s: bit 31 is set", __func__);

	for (i = 0; i < ninitialized; i++) {
		if (newhi != lasthi[i])
			continue;
		if (i != 0)
			return newlo | 0x80000000;
		else
			return newlo;
	}
	if (ninitialized == __arraycount(lasthi)) {
		errx(EXIT_FAILURE, "%s: too many distinct high words",
		    __func__);
	}
	lasthi[ninitialized++] = newhi;
	if (ninitialized > 1)
		return newlo | 0x80000000;
	else
		return newlo;
}

static void
emit_legacy_op(const rvp_pstate_t *ps, const rvp_ubuf_t *ub, rvp_op_t op,
    bool is_load, int field_width)
{
	static uint64_t gid = 0;
	legacy_event_t ev;
	int lop = rvp_op_to_legacy_op(op);

	ev.gid = ++gid;	// XXX XXX XXX
	ev.tid = ps->ps_curthread;
	// id is "statement id"
	ev.id = compress_pc(ps->ps_thread[ps->ps_curthread].ts_lastpc);

	if (lop < 0)
		return;

	assert(lop <= UINT8_MAX);

	ev.type = lop;

	switch (op) {
	case RVP_OP_ATOMIC_LOAD8:
	case RVP_OP_ATOMIC_STORE8:
	case RVP_OP_LOAD8:
	case RVP_OP_STORE8:
		ev.value = ub->ub_load8_store8.data;
		ev.addr = ub->ub_load8_store8.addr;
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
		ev.addr = ub->ub_load1_2_4_store1_2_4.addr;
		break;
	default:
		errx(EXIT_FAILURE, "%s: conversion unknown", __func__);
	case RVP_OP_FORK:
	case RVP_OP_JOIN:
		ev.value = ub->ub_fork_join_switch.tid;
		break;
	case RVP_OP_ENTERFN:
	case RVP_OP_EXITFN:
		break;
	case RVP_OP_ACQUIRE:
	case RVP_OP_RELEASE:
		ev.addr = ub->ub_acquire_release.addr;
		break;
	}
	if (write(STDOUT_FILENO, &ev, sizeof(ev)) == -1)
		err(EXIT_FAILURE, "%s: write", __func__);
}

static void
print_jump(const rvp_pstate_t *ps, uintptr_t pc)
{ 
	printf("tid %" PRIu32 " pc %#016" PRIxPTR " jump\n",
	    ps->ps_curthread, pc);
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
		printf("tid %" PRIu32 " pc %#016" PRIxPTR
		    " %s %#.*" PRIx64 " %s [%#016" PRIxPTR "]\n",
		    ps->ps_curthread,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc, oi->oi_descr,
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
		printf("tid %" PRIu32 " pc %#016" PRIxPTR
		    " %s %#.*" PRIx32 " %s [%#016" PRIxPTR "]\n",
		    ps->ps_curthread,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc, oi->oi_descr,
		    field_width,
		    ub->ub_load1_2_4_store1_2_4.data,
		    is_load ? "<-" : "->",
		    ub->ub_load1_2_4_store1_2_4.addr);
		break;
	case RVP_OP_COG:
		printf(
		    "tid %" PRIu32 " pc %#016" PRIxPTR " %s"
		    " generation %" PRIu64 "\n",
		    ps->ps_curthread, ps->ps_thread[ps->ps_curthread].ts_lastpc,
		    oi->oi_descr, ub->ub_cog.generation);
		break;
	case RVP_OP_END:
	default:
		printf("tid %" PRIu32 " pc %#016" PRIxPTR " %s\n",
		    ps->ps_curthread,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc, oi->oi_descr);
		break;
	case RVP_OP_FORK:
	case RVP_OP_JOIN:
	case RVP_OP_SWITCH:
		printf(
		    "tid %" PRIu32 " pc %#016" PRIxPTR " %s tid %" PRIu32 "\n",
		    ps->ps_curthread, ps->ps_thread[ps->ps_curthread].ts_lastpc,
		    oi->oi_descr, ub->ub_fork_join_switch.tid);
		// TBD create a fledgling rvp_thread_pstate_t on fork?
		break;
	case RVP_OP_ACQUIRE:
	case RVP_OP_RELEASE:
		printf("tid %" PRIu32 " pc %#016" PRIxPTR
		    " %s [%#016" PRIxPTR "]\n",
		    ps->ps_curthread,
		    ps->ps_thread[ps->ps_curthread].ts_lastpc, oi->oi_descr,
		    ub->ub_acquire_release.addr);
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
	uintptr_t lastpc;
	int jmpvec;
	bool is_load = false;
	int field_width = 0;

	if (pc_is_not_deltop(ps, ub->ub_pc)) {
		ps->ps_thread[ps->ps_curthread].ts_lastpc = ub->ub_pc;
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
	lastpc = ps->ps_thread[ps->ps_curthread].ts_lastpc;
	if (op == RVP_OP_BEGIN) {
		rvp_pstate_begin_thread(ps, ub->ub_begin.tid,
		    ub->ub_begin.generation);
	}
	ps->ps_thread[ps->ps_curthread].ts_lastpc = lastpc + jmpvec;
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

	if (op == RVP_OP_SWITCH)
		ps->ps_curthread = ub->ub_fork_join_switch.tid;

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
		, .th_pointer_width = sizeof(uintptr_t)
		, .th_data_width = sizeof(uint32_t)
		, .th_pad1 = { 0 }
	};
	uintptr_t pc0;
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

	ub.ub_begin = (rvp_begin_t){.deltop = pc0, .tid = tid};

	size_t nfull = sizeof(ub.ub_begin);
	size_t nshort = 0;
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
