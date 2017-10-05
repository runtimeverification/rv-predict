#ifndef _RVP_BUF_H_
#define _RVP_BUF_H_

#include <assert.h>
#include <stdint.h>	/* for uint32_t */
#include <string.h>

#include "nbcompat.h"	/* for __arraycount() */
#include "tracefmt.h"	/* for rvp_op_t */

typedef struct {
	unsigned int b_nwords;
	uint32_t b_word[];
} rvp_generic_buf_t;

static rvp_generic_buf_t rvp_generic_buf_t_example;

// The type for a rvp_generic_buf_t with 'capacity' entries in the buffer.
#define RVP_BUF_TYPE(capacity) \
union { \
	rvp_generic_buf_t buf; \
	char filler[sizeof(rvp_generic_buf_t) + \
	    (capacity) * sizeof(rvp_generic_buf_t_example.b_word[0])]; \
}

typedef RVP_BUF_TYPE(12) rvp_buf_t;

#define RVP_BUF_GENERIC_INITIALIZER(type)	(type){ .buf.b_nwords = 0 }
#define RVP_BUF_INITIALIZER	RVP_BUF_GENERIC_INITIALIZER(rvp_buf_t)

#define RVP_BUF_CAPACITY(b) \
	((__arraycount((b).filler) - sizeof(rvp_generic_buf_t)) \
		/ sizeof((b).buf.b_word[0]))

static inline void
rvp_buf_generic_put(rvp_generic_buf_t *b, uint32_t item, unsigned int capacity)
{
	assert(b->b_nwords < capacity);
	b->b_word[b->b_nwords++] = item;
}

#define rvp_buf_put(b, item) \
rvp_buf_generic_put(&((b)->buf), item, RVP_BUF_CAPACITY(*(b)))

static inline void
rvp_buf_generic_put_addr(rvp_generic_buf_t *b, rvp_addr_t addr, unsigned int capacity)
{
	unsigned int i;
	union {
		rvp_addr_t uaddr;
		uint32_t u32[sizeof(rvp_addr_t) / sizeof(uint32_t)];
	} addru = {.uaddr = addr};

	for (i = 0; i < __arraycount(addru.u32); i++) {
		rvp_buf_generic_put(b, addru.u32[i], capacity);
	}
}

#define rvp_buf_put_addr(b, item) \
rvp_buf_generic_put_addr(&((b)->buf), item, RVP_BUF_CAPACITY(*(b)))

static inline void
rvp_buf_generic_put_voidptr(rvp_generic_buf_t *b, const void *addr, unsigned int capacity)
{
	rvp_buf_generic_put_addr(b, (rvp_addr_t)addr, capacity);
}

#define rvp_buf_put_voidptr(b, item) \
rvp_buf_generic_put_voidptr(&((b)->buf), item, RVP_BUF_CAPACITY(*(b)))

static inline void
rvp_buf_generic_put_u64(rvp_generic_buf_t *b, uint64_t val, unsigned int capacity)
{
	union {
		uint64_t u64;
		uint32_t u32[2];
	} valu = {.u64 = val};

	rvp_buf_generic_put(b, valu.u32[0], capacity);
	rvp_buf_generic_put(b, valu.u32[1], capacity);
}

#define rvp_buf_put_u64(b, item) \
rvp_buf_generic_put_u64(&((b)->buf), item, RVP_BUF_CAPACITY(*(b)))

static inline void
rvp_buf_generic_put_string(rvp_generic_buf_t *b, const char* str, unsigned int capacity)
{
	union {
		uint32_t u32;
		char c[4];
	} value = {.u32 = 0};
	int filled = 0;

	rvp_buf_generic_put(b, strlen(str), capacity);

	for (; *str; str++) {
		value.c[filled] = *str;
		if (filled == 3) {
			filled = 0;
			rvp_buf_generic_put(b, value.u32, capacity);
			value.u32 = 0;
		} else {
			filled++;
		}
	}
	if (filled > 0) {
		rvp_buf_generic_put(b, value.u32, capacity);
	}
}

#define rvp_buf_put_string(b, item) \
rvp_buf_generic_put_string(&((b)->buf), item, RVP_BUF_CAPACITY(*(b)))

void rvp_buf_put_pc_and_op(rvp_buf_t *, const char **, const char *, rvp_op_t);
void rvp_buf_put_cog(rvp_buf_t *, uint64_t);

#endif /* _RVP_BUF_H_ */
