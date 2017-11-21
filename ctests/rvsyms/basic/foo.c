#include <err.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

#include "foo.h"
#include "frame.h"
#include "nbcompat.h"

static char *
dataptr_to_string(char *buf, size_t buflen,
    const void *data, const void *outer_pc, const void *outer_cfa)
{
	int rc;
	const void *inner_pc, *inner_cfa;
	const void *this_pc, *this_cfa;

	(void)innerframe(&inner_pc, &inner_cfa);

	this_pc = program_counter();
	this_cfa = __builtin_dwarf_cfa();	/* == llvm.eh.dwarf.cfa */

	if (bracketed(inner_cfa, this_cfa, data)) {
		rc = snprintf(buf, buflen, "[%p : %p/%p %p/%p]",
		    data, inner_pc, inner_cfa, this_pc, this_cfa);
	} else {
		rc = snprintf(buf, buflen, "[%p : %p/%p %p/%p]",
		    data, outer_pc, outer_cfa, this_pc, this_cfa);
	}
	if (rc < 0 || rc >= buflen)
		errx(EXIT_FAILURE, "%s: snprintf failed", __func__);

	return buf;
}

#define	MATCH_ELT(__e, __n)	{	\
	  .ptr = &(__e)		\
	, .name = __n		\
}

#define	SIMPLE_MATCH_ELT(__e)	MATCH_ELT(__e, #__e)

void
foo(void)
{
	int i;
	xyz_t xyz = {.x = {3, 4, 5}, .y = {6, 7, 8}, .z = {9, 10, 11}};
	xyz_t array[3][4];
	int iarray[7][5][2] = {{{1}}};
	struct {
		struct {
			int a, b;
		} t[13];
	} s[11];
	struct {
		int a;
		struct {
			int x[13];
		} b;
	} p[11];
	char buf[sizeof("[0x0123456789abcdef : 0x0123456789abcdef/0x0123456789abcdef 0x0123456789abcdef/0x0123456789abcdef]")];
	void *cfa = __builtin_dwarf_cfa(); 
	struct {
		const void *ptr;
		const char *name;
	} elt[] = {
		  SIMPLE_MATCH_ELT(xyz.x.p)
		, SIMPLE_MATCH_ELT(array[1][2].y.q)
		, SIMPLE_MATCH_ELT(s[7].t[5].b)
		, SIMPLE_MATCH_ELT(s[7].t[5].a)
		, SIMPLE_MATCH_ELT(p[5].a)
		, SIMPLE_MATCH_ELT(p[5].b.x[9])
		, SIMPLE_MATCH_ELT(iarray[0][0][0])
		, MATCH_ELT(xyz, "xyz.x.p")
		, MATCH_ELT(array[0], "array[0][0].x.p")
		, MATCH_ELT(iarray[0], "iarray[0][0][0]")
		, SIMPLE_MATCH_ELT(cfa)
	};

	for (i = 0; i < __arraycount(elt); i++) {
		printf("%s\n", dataptr_to_string(buf, sizeof(buf),
		    elt[i].ptr, program_counter(), cfa));
		printf("%s at %s;%s\n", elt[i].name, __FILE__, __func__);
	}
}
