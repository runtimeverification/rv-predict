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
	uintptr_t deltop;
	uint32_t id;
} __packed __aligned(sizeof(uint32_t)) threadswitch_t;

static const rvp_trace_header_t header = {
	  .th_magic = "RVP_"
	, . th_version = 0
	, .th_byteorder = '0' | ('1' << 8) | ('2' << 16) | ('3' << 24)
	, .th_pointer_width = sizeof(uintptr_t)
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
	const char *tracefn = getenv("RVP_TRACE_FILE");

	int fd = open((tracefn != NULL) ? tracefn : "./rvpredict.trace",
	    O_WRONLY|O_CREAT|O_TRUNC, 0600);

	if (fd == -1)
		return -1;

	if (writeall(fd, &header, sizeof(header)) == -1) {
		close(fd);
		return -1;
	}

	return fd;
}

bool
rvp_thread_flush_to_fd(rvp_thread_t *t, int fd, bool trace_switch)
{
	int iovcnt = 0;
	static ssize_t total = 0, lastsw = -1;
	static uint32_t last_tid = 0xffffffff;
	ssize_t nwritten;
	rvp_ring_t *r = &t->t_ring;
	uint32_t *producer = r->r_producer, *consumer = r->r_consumer;
	threadswitch_t threadswitch = {
		  .deltop =
		      (uintptr_t)rvp_vec_and_op_to_deltop(0, RVP_OP_SWITCH)
		, .id = t->t_id
	};
	struct iovec iov[3];

	if (consumer == producer)
		return false;

	if (trace_switch) {
		assert(lastsw < total);
		assert(last_tid != threadswitch.id);
		iov[iovcnt++] = (struct iovec){
			  .iov_base = &threadswitch
			, .iov_len = sizeof(threadswitch)
		};
	}

	if (consumer < producer) {
		iov[iovcnt++] = (struct iovec){
			  .iov_base = consumer
			, .iov_len = (producer - consumer) *
				     sizeof(consumer[0])
		};
	} else {	/* consumer > producer */
		iov[iovcnt++] = (struct iovec){
			  .iov_base = consumer
			, .iov_len = (r->r_last + 1 - consumer) *
				     sizeof(consumer[0])
		};
		iov[iovcnt++] = (struct iovec){
			  .iov_base = r->r_items
			, .iov_len = (producer - r->r_items) *
				     sizeof(r->r_items[0])
		};
	}
	nwritten = writev(fd, iov, iovcnt);
	if (trace_switch)
		lastsw = total + sizeof(threadswitch);
	total += nwritten;
	assert(trace_switch
	    ? (nwritten > sizeof(threadswitch))
	    : (nwritten > 0));

	while (--iovcnt >= 0)
		nwritten -= iov[iovcnt].iov_len;

	assert(nwritten == 0);
	r->r_consumer = producer;
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
		rvp_buf_put_addr(b, pc);
		deltop = rvp_vec_and_op_to_deltop(0, op);
		assert(deltop != NULL);
	}
	rvp_buf_put_addr(b, deltop);
}

void
rvp_ring_put_begin(rvp_ring_t *r, uint32_t tid)
{
	r->r_lastpc = __builtin_return_address(0);
	rvp_buf_t b = RVP_BUF_INITIALIZER;
	rvp_buf_put_addr(&b, rvp_vec_and_op_to_deltop(0, RVP_OP_BEGIN));
	rvp_buf_put(&b, tid);
	rvp_buf_put_addr(&b, r->r_lastpc);
	rvp_ring_put_buf(r, b);
}

