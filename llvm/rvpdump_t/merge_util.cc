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
// #include <unistd.h>	/* for read(2) */

#include "io.h"	/* for __arraycount, offsetof */
#include "nbcompat.h"	/* for __arraycount, offsetof */
#include "reader.h"
#include "op_to_info.h"
#include "merge_util.h"

rvp_trace_header_t expected_trace_header = {
		  th_magic : {'R', 'V', 'P', '_'}
		, th_version : {0, 0, 0, 3}
		, th_byteorder : '0' | ('1' << 8) | ('2' << 16) | ('3' << 24)
		, th_pointer_width : sizeof(rvp_addr_t)
		, th_data_width : sizeof(uint32_t)
		, th_pad1 : { 0 }
	};

typedef char prebuf_t[
    sizeof("tid 4294967295.4294967295 18446744073709551615 ") +
    MAX(sizeof("{0x0123456789abcdef}"), sizeof("pc 0x0123456789abcdef"))];

static void reemit_binary_init(const rvp_pstate_t *,
    const rvp_trace_header_t *);
static void reemit_binary_nop(const rvp_pstate_t *, const rvp_ubuf_t *);
static void reemit_binary_op(const rvp_pstate_t *, const rvp_ubuf_t *, rvp_op_t,
    bool, int);

static const rvp_emitters_t binary = {
	  .init = reemit_binary_init
	, .emit_nop = reemit_binary_nop
	, .emit_op = reemit_binary_op
	, .dataptr_to_string = NULL
	, .insnptr_to_string = NULL
};

static void
extract_jmpvec_and_op_from_deltop(rvp_addr_t deltop0,
    rvp_addr_t pc, int *jmpvecp, rvp_op_t *opp)
{
	/* XXX it's not strictly necessary for deltops to have any concrete
	 * storage
	 */
	deltops_t deltops = {};

	int row = (pc - deltop0) / __arraycount(deltops.matrix[0]);
	int jmpvec = row - RVP_NJMPS / 2;

	rvp_op_t op = static_cast<rvp_op_t> ((pc - deltop0) -
	    (&deltops.matrix[row][0] - &deltops.matrix[0][0]));

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

	ps->ps_thread = static_cast<rvp_thread_pstate_t*>(calloc(ps->ps_nthreads, sizeof(rvp_thread_pstate_t)));
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
	deltops_t deltops = {};

	ps->ps_emit_generation = emit_generation;
	ps->ps_emit_bytes = emit_bytes;
	ps->ps_emitters = emitters;

	ps->ps_deltop_center = op0;
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
consume_and_print_trace(rvp_pstate_t *ps, rvp_ubuf_t *ub, size_t *nfullp, uint64_t *gen)
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

		*gen = ub->ub_cog.generation;

	} else if (op == RVP_OP_ENTERSIG) {
		rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
		ts->ts_generation[ps->ps_idepth] =
		    ub->ub_entersig.generation;
	}
	assert(op != RVP_OP_ENTERSIG || ps->ps_idepth != 0);

	lastpc = ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_idepth];
	pc = lastpc + jmpvec;
	ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_idepth] = pc;

	// if (op == RVP_OP_ENTERFN) {
	// 	rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
	// } else if (op == RVP_OP_EXITFN) {
	// 	rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
	// }

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

	if(op != RVP_OP_SWITCH){
		(*emitters->emit_op)(ps, ub, op, is_load, field_width);	
	}
	

	if (op == RVP_OP_COG) {
		rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
		assert(ps->ps_idepth < __arraycount(ts->ts_generation));
		assert(ts->ts_generation[ps->ps_idepth] <
		    ub->ub_cog.generation);
		ts->ts_generation[ps->ps_idepth] = ub->ub_cog.generation;
		ts->ts_nops[ps->ps_idepth] = 0;

		*gen = ub->ub_cog.generation;
	}

	if (op == RVP_OP_END) {
		rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];
		ts->ts_present = false;
	}

	// if (op == RVP_OP_SWITCH) {
	// 	ps->ps_curthread = ub->ub_fork_join_switch.tid;
	// 	ps->ps_idepth = 0;
	// } else if (op == RVP_OP_EXITSIG) {
	// 	assert(ps->ps_idepth != 0);
	// } else if (op == RVP_OP_SIGDEPTH) {
	// 	rvp_thread_pstate_t *ts = &ps->ps_thread[ps->ps_curthread];

	// 	assert(ub->ub_sigdepth.depth <
	// 	    __arraycount(ts->ts_generation));
	// 	ps->ps_idepth = ub->ub_sigdepth.depth;
	// }

	advance(&ub->ub_bytes[0], nfullp, oi->oi_reclen);
	return 0;
}

uint64_t
rvp_trace_dump_with_emitters_until_cog(uint32_t tid, bool emit_generation, bool emit_bytes, const rvp_emitters_t *emitters, int fd, rvp_pstate_t *ps, rvp_ubuf_t* ub)
{

	ssize_t nread;

	size_t nrecords = 0;
	size_t nfull = sizeof(ub->ub_begin);
	ssize_t nshort = 0;
	uint64_t gen = UINT_MAX;
	for (;;) {
		nread = read(fd, &(ub->ub_bytes[nfull]), sizeof(*ub) - nfull);
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
		if (nfull < sizeof(ub->ub_pc)) {
			errx(EXIT_FAILURE, "%s: short read (pc/deltop)",
			    __func__);
		}
		do {
			// if (nrecords == most_nrecords)
			// 	return -1;
			nshort = consume_and_print_trace(ps, ub, &nfull, &gen);
			if (nshort != 0)
				break;
			++nrecords;
		} while (nfull >= sizeof(ub->ub_pc));

		if(gen < UINT_MAX){
			return gen;
		}
	}
	return -1;
}

uint64_t
rvp_trace_dump_until_cog(int fd, uint32_t tid, rvp_pstate_t *ps, rvp_ubuf_t *ub)
{
	bool emit_generation = true;
	bool emit_bytes = true;
	// size_t most_nrecords = op->op_nrecords;
	return rvp_trace_dump_with_emitters_until_cog(tid, emit_generation, emit_bytes, &binary, fd, ps, ub);
}

size_t
read_begin(rvp_pstate_t *ps, rvp_ubuf_t *ub, size_t *nfullp, uint64_t *gen)
{
	// const rvp_emitters_t *emitters = ps->ps_emitters;
	rvp_op_t op;
	rvp_addr_t lastpc, pc;
	int jmpvec;

	if (pc_is_not_deltop(ps, ub->ub_pc)) {
		ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_idepth] =
		    ub->ub_pc;
		// (*emitters->emit_nop)(ps, ub);
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
	assert(op == RVP_OP_BEGIN);
	rvp_pstate_begin_thread(ps, ub->ub_begin.tid, ub->ub_begin.generation);
	*gen = ub->ub_cog.generation;


	lastpc = ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_idepth];
	pc = lastpc + jmpvec;
	ps->ps_thread[ps->ps_curthread].ts_lastpc[ps->ps_idepth] = pc;

	advance(&ub->ub_bytes[0], nfullp, oi->oi_reclen);
	return 0;
}

uint64_t
rvp_read_header(int fd, uint32_t tid, rvp_pstate_t *ps, rvp_ubuf_t *ub)
{
	rvp_addr_t pc0;
	uint32_t tid_unused;
	uint64_t generation;
	ssize_t nread;
	rvp_trace_header_t th;

	int cmp;
	
	/* XXX make a compile-time assertion that this is a little-endian
	 * machine, since we assume that when we read the initial 'begin'
	 * operation, below.
	 */

	const struct iovec iov[] = {
		  { .iov_base = &th, .iov_len = sizeof(th) }
		, { .iov_base = &pc0, .iov_len = sizeof(pc0) }
		, { .iov_base = &tid_unused, .iov_len = sizeof(tid_unused) }
		, { .iov_base = &generation, .iov_len = sizeof(generation) }
	};
	struct iovec scratch[__arraycount(iov)];

	if ((nread = readallv(fd, iov, scratch, __arraycount(iov))) == -1)
		err(EXIT_FAILURE, "%s: readallv(header)", __func__);

	if (nread < iovsum(iov, __arraycount(iov))) {
		errx(EXIT_FAILURE,
		    "%s: read %zd bytes, expected %zd (header + 1st deltop + ggen init)",
		    __func__, nread, iovsum(iov, __arraycount(iov)));
	}

	cmp = memcmp(th.th_magic, expected_trace_header.th_magic, sizeof(th.th_magic));
	if (cmp != 0) {
		errx(EXIT_FAILURE, "%s: bad magic %4s", __func__,
		    th.th_magic);
	}
	cmp = memcmp(th.th_version, expected_trace_header.th_version,
	    sizeof(th.th_version));
	if (cmp != 0) {
		errx(EXIT_FAILURE, "%s: unsupported version %d.%d.%d.%d",
		    __func__, th.th_version[0], th.th_version[1],
		    th.th_version[2], th.th_version[3]);
	}
	if (th.th_byteorder != expected_trace_header.th_byteorder)
		errx(EXIT_FAILURE, "%s: unsupported byteorder", __func__);
	if (th.th_pointer_width != expected_trace_header.th_pointer_width)
		errx(EXIT_FAILURE, "%s: unsupported pointer width", __func__);
	if (th.th_data_width != expected_trace_header.th_data_width)
		errx(EXIT_FAILURE, "%s: unsupported data width", __func__);

	rvp_pstate_t ps0;
	ps = &ps0;

	const rvp_emitters_t *emitters = ps->ps_emitters;
	bool emit_generation = true;
	bool emit_bytes = true;

	rvp_pstate_init(ps, emitters, emit_generation, emit_bytes, pc0);

	if (emitters->init != NULL)
		(*emitters->init)(ps, &expected_trace_header);

	ub->ub_begin = (rvp_begin_t){.deltop = pc0, .tid = tid};

	// Now read until you see a begin statement.
	// ssize_t nread;

	size_t nfull = sizeof(ub->ub_begin);
	ssize_t nshort = 0;
	uint64_t gen = UINT_MAX;
	nread = read(fd, &(ub->ub_bytes[nfull]), sizeof(*ub) - nfull);
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
	// if (nfull == 0)
	// 	break;
	if (nfull < sizeof(ub->ub_pc)) {
		errx(EXIT_FAILURE, "%s: short read (pc/deltop)",
		    __func__);
	}
	do {
		nshort = read_begin(ps, ub, &nfull, &gen);
		if (nshort != 0)
			break;
	} while (nfull >= sizeof(ub->ub_pc));

	if(gen == UINT_MAX){
		errx(EXIT_FAILURE, "%s: expected begin event in %d.\n", __func__, tid);
	}
	return gen;
}