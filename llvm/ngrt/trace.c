#include <assert.h>
#include <sys/types.h>	/* for open(2) */
#include <sys/stat.h>	/* for open(2) */
#include <fcntl.h>	/* for open(2) */
#include <sys/uio.h>	/* for writev(2) */
#include <stdlib.h>	/* for getenv(3) */

#include "access.h"
#include "nbcompat.h"
#include "notimpl.h"
#include "ring.h"
#include "rvpint.h"
#include "thread.h"
#include "trace.h"
#include "tracefmt.h"

static __section(".text") deltops_t deltops = { .matrix = { { 0 } } };

typedef struct _threadswitch {
	rvp_addr_t deltop;
	uint32_t id;
} __packed __aligned(sizeof(uint32_t)) threadswitch_t;

static const rvp_trace_header_t header = {
	  .th_magic = "RVP_"
	, . th_version = {0, 0, 0, 3}
	, .th_byteorder = '0' | ('1' << 8) | ('2' << 16) | ('3' << 24)
	, .th_pointer_width = sizeof(rvp_addr_t)
	, .th_data_width = sizeof(uint32_t)
};

static ssize_t
writeall(int fd, const void *buf, size_t nbytes)
{
	ssize_t nwritten, nleft;
	const char *next;

	for (next = buf, nleft = nbytes;  
	     nleft != 0;
	     next += nwritten, nleft -= nwritten) {
		if ((nwritten = write(fd, next, nleft)) == -1) {
			return -1;
		}
	}
	return nwritten;
}

int
rvp_trace_open(void)
{
	const char *envfile = getenv("RVP_TRACE_FILE");
	const char *tracefn = (envfile != NULL) ? envfile : "./rvpredict.trace";

	int fd = open(tracefn, O_WRONLY|O_CREAT|O_TRUNC, 0600);

	if (fd == -1)
		err(EXIT_FAILURE, "%s: open(\"%s\")", __func__, tracefn);

	if (writeall(fd, &header, sizeof(header)) == -1)
		err(EXIT_FAILURE, "%s: open(\"%s\")", __func__, tracefn);

	return fd;
}

bool
rvp_ring_flush_to_fd(rvp_ring_t *r, int fd, rvp_lastctx_t *lc)
{
	ssize_t nwritten;
	uint32_t idepth0, idepth1;
	rvp_fork_join_switch_t threadswitch = {
		  .deltop =
		      (rvp_addr_t)rvp_vec_and_op_to_deltop(0, RVP_OP_SWITCH)
		, .tid = r->r_tid
	};
	rvp_sigdepth_t sigdepth = {
		  .deltop =
		      (rvp_addr_t)rvp_vec_and_op_to_deltop(0, RVP_OP_SIGDEPTH)
		, .depth = r->r_idepth
	};
	struct iovec iov[20] = {
		  [0] = (struct iovec){
			  .iov_base = &threadswitch
			, .iov_len = sizeof(threadswitch)
		}
		, [1] = (struct iovec){
			  .iov_base = &sigdepth
			, .iov_len = sizeof(sigdepth)
		}
	};
	struct iovec *iovp = &iov[0];

	if (lc == NULL)
		;
	else if (lc->lc_tid != r->r_tid) {
		iovp++; /* emit 'switch' to r->r_tid; that will
			 * reset the reader's interrupt depth to 0.
			 */

		if (r->r_idepth != 0) {
			iovp++; /* emit 'sigdepth' to r->r_idepth */
		}
	} else if (lc->lc_idepth != r->r_idepth) {
		iov[0] = iov[1];
		iovp++; /* emit 'sigdepth' to r->r_idepth */
	}
	const struct iovec *first_ring_iov, *iiov,
	    *lastiov = &iov[__arraycount(iov)];

	first_ring_iov = iovp;

	idepth0 = idepth1 = (lc == NULL) ? 0 : r->r_idepth;
	(void)rvp_ring_get_iovs(r, NULL, &iovp, lastiov, &idepth0);

	if (iovp == first_ring_iov)
		return false;

	nwritten = writev(fd, iov, iovp - &iov[0]);
	if (nwritten == -1)
		err(EXIT_FAILURE, "%s: writev", __func__);

	for (iiov = &iov[0]; iiov < first_ring_iov; iiov++)
		nwritten -= iiov->iov_len;

	assert(nwritten > 0);

	const struct iovec *check_iov = first_ring_iov;
	assert(rvp_ring_discard_iovs(r, NULL, &check_iov, iovp, &idepth1) <= 0);
	assert(idepth0 == idepth1);

	if (lc != NULL) {
		lc->lc_tid = r->r_tid;
		lc->lc_idepth = idepth0;
	} else
		assert(idepth0 == 0);

	return true;
}

deltop_t *
rvp_vec_and_op_to_deltop(int jmpvec, rvp_op_t op)
{
	deltop_t *deltop =
	    &deltops.matrix[__arraycount(deltops.matrix) / 2 + jmpvec][op];

	if (deltop < &deltops.matrix[0][0] ||
		     &deltops.matrix[RVP_NJMPS - 1][RVP_NOPS - 1] < deltop)
		return NULL;
	
	return deltop;
}

void
rvp_buf_put_pc_and_op(rvp_buf_t *b, const char **lastpcp, const char *pc,
    rvp_op_t op)
{
	int jmpvec = pc - *lastpcp;
	deltop_t *deltop;

	deltop = rvp_vec_and_op_to_deltop(jmpvec, op);

	*lastpcp = pc;

	if (deltop == NULL) {
		rvp_buf_put_voidptr(b, pc);
		deltop = rvp_vec_and_op_to_deltop(0, op);
		assert(deltop != NULL);
	}
	rvp_buf_put_voidptr(b, deltop);
}

void
rvp_ring_put_begin(rvp_ring_t *r, uint32_t tid, uint64_t generation)
{
	rvp_buf_t b = RVP_BUF_INITIALIZER;
	rvp_buf_put_voidptr(&b, rvp_vec_and_op_to_deltop(0, RVP_OP_BEGIN));
	rvp_buf_put(&b, tid);
	rvp_buf_put_u64(&b, generation);
	rvp_ring_put_buf(r, b);
}

void
rvp_buf_put_cog(rvp_buf_t *b, uint64_t generation)
{
	rvp_buf_put_voidptr(b, rvp_vec_and_op_to_deltop(0, RVP_OP_COG));
	rvp_buf_put_u64(b, generation);
}
