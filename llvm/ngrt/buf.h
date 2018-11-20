#ifndef _RVP_BUF_H_
#define _RVP_BUF_H_

#include <assert.h>
#include <stdint.h>	/* for uint32_t */

#include "nbcompat.h"	/* for __arraycount() */
#include "tracefmt.h"	/* for rvp_op_t */
#include "deltops.h"	/* for rvp_vec_and_op_to_deltop */

typedef struct _rvp_cursor {
	uint32_t *c_producer;
	uint32_t * const c_last;
	uint32_t * const c_first;
} rvp_cursor_t;

#define RVP_BUF_NITEMS 20

typedef struct {
	unsigned int b_nwords;
	uint32_t b_word[RVP_BUF_NITEMS];
} rvp_buf_t;

#define RVP_BUF_INITIALIZER	(rvp_buf_t){ .b_nwords = 0 }

static inline void
rvp_buf_put(rvp_buf_t *b, uint32_t item)
{
	assert(b->b_nwords < __arraycount(b->b_word));
	b->b_word[b->b_nwords++] = item;
}

static inline void
rvp_buf_put_buf(rvp_buf_t *db, const rvp_buf_t *sb)
{
	unsigned int i;

	assert(db->b_nwords + sb->b_nwords <= __arraycount(db->b_word));
	for (i = 0; i < sb->b_nwords; i++)
		db->b_word[db->b_nwords++] = sb->b_word[i];
}

static inline void
rvp_buf_put_addr(rvp_buf_t *b, rvp_addr_t addr)
{
	unsigned int i;
	union {
		rvp_addr_t uaddr;
		uint32_t u32[sizeof(rvp_addr_t) / sizeof(uint32_t)];
	} addru = {.uaddr = addr};

	for (i = 0; i < __arraycount(addru.u32); i++) {
		rvp_buf_put(b, addru.u32[i]);
	}
}

static inline void
rvp_buf_put_voidptr(rvp_buf_t *b, const void *addr)
{
	rvp_buf_put_addr(b, (rvp_addr_t)addr);
}

static inline void
rvp_cursor_put(rvp_cursor_t *c, uint32_t item)
{
#if 0	// TBD diagnostic code here: don't walk past consumer pointer!
	assert(b->b_nwords < __arraycount(b->b_word));
#endif
	*c->c_producer = item;
	if (__predict_false(c->c_producer == c->c_last))
		c->c_producer = c->c_first;
	else
		c->c_producer++;
}

static inline void
rvp_cursor_put_addr(rvp_cursor_t *c, rvp_addr_t addr)
{
	unsigned int i;
	union {
		rvp_addr_t uaddr;
		uint32_t u32[sizeof(rvp_addr_t) / sizeof(uint32_t)];
	} addru = {.uaddr = addr};

	for (i = 0; i < __arraycount(addru.u32); i++) {
		rvp_cursor_put(c, addru.u32[i]);
	}
}

static inline void
rvp_cursor_put_voidptr(rvp_cursor_t *c, const void *addr)
{
	rvp_cursor_put_addr(c, (rvp_addr_t)addr);
}

static inline void
rvp_cursor_put_u64(rvp_cursor_t *c, uint64_t val)
{
	union {
		uint64_t u64;
		uint32_t u32[2];
	} valu = {.u64 = val};

	rvp_cursor_put(c, valu.u32[0]);
	rvp_cursor_put(c, valu.u32[1]);
}

static inline void
rvp_cursor_put_pc_and_op(rvp_cursor_t *c, const char **lastpcp, const char *pc,
    rvp_op_t op)
{
	int jmpvec = pc - *lastpcp;
	deltop_t *deltop;

	deltop = rvp_vec_and_op_to_deltop(jmpvec, op);

	*lastpcp = pc;

	if (__predict_false(deltop == NULL)) {
		rvp_cursor_put_voidptr(c, pc);
		deltop = rvp_vec_and_op_to_deltop(0, op);
		assert(deltop != NULL);
	}
	rvp_cursor_put_voidptr(c, deltop);
}

void rvp_cursor_put_cog(rvp_cursor_t *, uint64_t);

static inline void
rvp_buf_put_u64(rvp_buf_t *b, uint64_t val)
{
	union {
		uint64_t u64;
		uint32_t u32[2];
	} valu = {.u64 = val};

	rvp_buf_put(b, valu.u32[0]);
	rvp_buf_put(b, valu.u32[1]);
}

static inline void
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

void rvp_buf_put_cog(rvp_buf_t *, uint64_t);

#endif /* _RVP_BUF_H_ */
