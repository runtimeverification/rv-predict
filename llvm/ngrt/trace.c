#include <assert.h>
#include <sys/types.h>	/* for open(2) */
#include <sys/stat.h>	/* for open(2) */
#include <fcntl.h>	/* for open(2) */
#include <sys/uio.h>	/* for writev(2) */

#include "access.h"
#include "nbcompat.h"
#include "notimpl.h"
#include "ring.h"
#include "rvpint.h"
#include "thread.h"
#include "trace.h"
#include "tracefmt.h"

static __section(".text") deltop_t deltops[RVP_NJMPS][RVP_NOPS] = { { 0 } };

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
	int fd;

	if ((fd = open("/dev/null", O_WRONLY|O_CREAT)) == -1)
		return -1;

	if (writeall(fd, &header, sizeof(header)) == -1) {
		close(fd);
		return -1;
	}

	return fd;
}

void
rvp_thread_flush_to_fd(rvp_thread_t *t, int fd)
{
	ssize_t nwritten;
	rvp_ring_t *r = &t->t_ring;
	uint32_t *producer = r->r_producer, *consumer = r->r_consumer;
	threadswitch_t threadswitch = {
		  .deltop =
		      (uintptr_t)rvp_vec_and_op_to_deltop(0, RVP_OP_SWITCH)
		, .id = t->t_id
	};

	if (consumer == producer) {
		return;
	}

	if (consumer < producer) {
		const struct iovec iov[2] = {
			{
				  .iov_base = &threadswitch
				, .iov_len = sizeof(threadswitch)
			}, {
				  .iov_base = consumer
				, .iov_len = (producer - consumer) *
				             sizeof(consumer[0])
			}
		};
		nwritten = writev(fd, iov, __arraycount(iov));
		assert(nwritten == iov[0].iov_len + iov[1].iov_len);
		r->r_consumer = producer;
		return;
	}
	/* producer < consumer */
	const struct iovec iov[3] = {
		{
			  .iov_base = &threadswitch
			, .iov_len = sizeof(threadswitch)
		}, {
			  .iov_base = consumer
			, .iov_len = (r->r_last - consumer) *
				     sizeof(consumer[0])
		}, {
			  .iov_base = r->r_items
			, .iov_len = (producer - r->r_items) *
				     sizeof(consumer[0])
		}
	};
	nwritten = writev(fd, iov, __arraycount(iov));
	assert(nwritten == iov[0].iov_len + iov[1].iov_len + iov[2].iov_len);
	r->r_consumer = producer;
}

void
rvp_ring_put_addr(rvp_ring_t *r, const void *addr)
{
	int i;
	union {
		uintptr_t uaddr;
		uint32_t u32[sizeof(uintptr_t) / sizeof(uint32_t)];
	} addru = {.uaddr = (uintptr_t)addr};

	for (i = 0; i < __arraycount(addru.u32); i++) {
		rvp_ring_put(r, addru.u32[0]);
	}
}

deltop_t *
rvp_vec_and_op_to_deltop(int jmpvec, rvp_op_t op)
{
	deltop_t *deltop = &deltops[__arraycount(deltops) / 2 + jmpvec][op];

	if (deltop < &deltops[0][0] ||
		    &deltops[RVP_NJMPS - 1][RVP_NOPS - 1] < deltop)
		return NULL;
	
	return deltop;
}

void
rvp_ring_put_pc_and_op(rvp_ring_t *r, const char *pc, rvp_op_t op)
{
	int jmpvec = pc - r->r_lastpc;
	deltop_t *deltop;

	deltop = rvp_vec_and_op_to_deltop(jmpvec, op);

	r->r_lastpc = pc;

	if (deltop == NULL) {
		rvp_ring_put_addr(r, pc);
		deltop = rvp_vec_and_op_to_deltop(0, op);
		assert(deltop != NULL);
	}
	rvp_ring_put_addr(r, deltop);
}

void
rvp_ring_put_begin(rvp_ring_t *r, uint32_t id)
{
	r->r_lastpc = __builtin_return_address(1);
	rvp_ring_put_addr(r, rvp_vec_and_op_to_deltop(0, RVP_OP_BEGIN));
	rvp_ring_put_addr(r, r->r_lastpc);
	rvp_ring_put(r, id);
}

void
rvp_ring_put_u64(rvp_ring_t *r, uint64_t val)
{
	union {
		uint64_t u64;
		uint32_t u32[2];
	} valu = {.u64 = val};

	rvp_ring_put(r, valu.u32[0]);
	rvp_ring_put(r, valu.u32[1]);
}
