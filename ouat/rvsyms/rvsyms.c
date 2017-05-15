/*
 * Copyright (c) 2016 Runtime Verification, Inc.
 * All rights reserved.
 */
/*
 * Copyright (c) 2009, David Anderson.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * * Neither the name of the example nor the
 *   names of its contributors may be used to endorse or promote products
 *   derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY David Anderson ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL David Anderson BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
/* simplereader.c
 * This is an example of code reading dwarf .debug_info.
 * It is kept as simple as possible to expose essential features.
 * It does not do all possible error reporting or error handling.

 * To use, try
 *     make
 *     ./simplereader simplereader
 */
#include <assert.h>
#include <err.h>
#include <errno.h>
#include <inttypes.h>
#include <fcntl.h>	/* for open() */
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>     /* for exit() */
#include <string.h>     /* for strdup() */
#include <unistd.h>     /* for close() */

#include <libdwarf.h>
#include <dwarf.h>

#include "nbcompat.h"
#include "strstk.h"

typedef bool (*dwarf_walk_predicate_t)(Dwarf_Debug, Dwarf_Die);

typedef struct _dwarf_walk_params {
	Dwarf_Debug dbg;
	dwarf_walk_predicate_t predicate;
	bool print_address, print_regex,
	    have_dataptr, have_frameptr, have_insnptr;
	uint64_t dataptr, frameptr, insnptr;
} dwarf_walk_params_t;

typedef struct _dwarf_walk_ctx {
	Dwarf_Addr lopc, hipc;
	Dwarf_Cie *cie_list;
	Dwarf_Signed ncies;
	Dwarf_Fde *fde_list;
	Dwarf_Signed nfdes;
	Dwarf_Signed cfa_offset;
	bool print_address, print_regex,
	    have_cfa_offset, have_dataptr, have_frameptr, have_insnptr;
	int subprogram_stkdepth, cu_stkdepth;
	strstack_t symstk;
	strstack_t locstk;
	uint64_t residue;
	uint64_t dataptr, frameptr, insnptr;
} dwarf_walk_ctx_t;

typedef struct _dwarf_walk {
	int stack_height;
	struct {
		Dwarf_Die die;
		int strstk_depth;
	} stack[16];
	Dwarf_Debug dbg;
	dwarf_walk_ctx_t ctx;
	dwarf_walk_predicate_t predicate;
	bool cu_by_cu;	// `true` if this is a CU-by-CU walk at the top of the
			// debug information; `false` if this is a walk of
			// some subtree rooted at a DIE
	int *pushp;
} dwarf_walk_t;

typedef enum _dwarf_type_kind {
	  DTK_QUALIFIER = 0
	, DTK_POINTER
	, DTK_TYPEDEF
	, DTK_BASE
	, DTK_ARRAY
	, DTK_UNKNOWN
	, DTK_OTHER
	, DTK_ENUM
} dwarf_type_kind_t;

static Dwarf_Die dwarf_walk_first(dwarf_walk_t *, const dwarf_walk_params_t *);
static Dwarf_Die dwarf_walk_next(dwarf_walk_t *);
static Dwarf_Die dwarf_walk_next_in_tree(dwarf_walk_t *);
static Dwarf_Die dwarf_aggregate_or_base_type(Dwarf_Debug, Dwarf_Die);
static dwarf_type_kind_t dwarf_type_kind(Dwarf_Die);
static void print_die_data(Dwarf_Debug, Dwarf_Die, dwarf_walk_ctx_t *);
static bool walk_members(Dwarf_Debug, Dwarf_Die, dwarf_walk_ctx_t *);
static bool walk_elements(Dwarf_Debug, Dwarf_Die, dwarf_walk_ctx_t *);
static ssize_t dwarf_array_type_size(Dwarf_Debug, Dwarf_Die);

static Dwarf_Die
dwarf_walk_first(dwarf_walk_t *walk, const dwarf_walk_params_t *params)
{
	walk->stack_height = 0;
	walk->dbg = params->dbg;
	walk->predicate = params->predicate;
	walk->ctx.print_address = params->print_address;
	walk->ctx.print_regex = params->print_regex;
	walk->ctx.have_dataptr = params->have_dataptr;
	walk->ctx.have_frameptr = params->have_frameptr;
	walk->ctx.have_insnptr = params->have_insnptr;
	walk->ctx.dataptr = params->dataptr;
	walk->ctx.frameptr = params->frameptr;
	walk->ctx.insnptr = params->insnptr;
	walk->ctx.cie_list = NULL;
	walk->ctx.fde_list = NULL;
	walk->ctx.have_cfa_offset = false;
	walk->ctx.ncies = walk->ctx.nfdes = 0;
	walk->ctx.cu_stkdepth = -1;
	walk->ctx.subprogram_stkdepth = -1;
	walk->cu_by_cu = true;
	strstack_init(&walk->ctx.symstk);
	strstack_init(&walk->ctx.locstk);

	return dwarf_walk_next(walk);
}

static Dwarf_Die
dwarf_walk_next(dwarf_walk_t *walk)
{
	Dwarf_Die next;

	while ((next = dwarf_walk_next_in_tree(walk)) != NULL) {
		if (walk->predicate == NULL ||
		    (*walk->predicate)(walk->dbg, next))
			break;
	}
	return next;
}

static Dwarf_Die
dwarf_walk_next_in_tree(dwarf_walk_t *walk)
{
	int res;
	Dwarf_Error error;
	Dwarf_Die child, last, sibling;
	Dwarf_Unsigned cu_header_length;
	Dwarf_Half version_stamp;
	Dwarf_Off abbrev_offset;
	Dwarf_Half address_size;
	Dwarf_Unsigned next_cu_header;
 
 	if (walk->pushp != NULL) {
		*walk->pushp = strstack_depth(&walk->ctx.symstk);
		walk->pushp = NULL;
	}

	while (walk->stack_height > 0 &&
	    walk->stack[walk->stack_height - 1].die == NULL) {
		walk->stack_height--;
		if (walk->stack_height > 0) {
			strstack_popto(&walk->ctx.symstk,
			    walk->stack[walk->stack_height - 1].strstk_depth);
		} else {
			strstack_popto(&walk->ctx.symstk, 0);
		}
	}

	if (walk->stack_height == 0) {
		if (!walk->cu_by_cu)
			return NULL;
		res = dwarf_next_cu_header(walk->dbg, &cu_header_length,
		    &version_stamp, &abbrev_offset, &address_size,
		    &next_cu_header, &error);

		if (res == DW_DLV_NO_ENTRY)
			return NULL;

		if (res != DW_DLV_OK) {
			errx(EXIT_FAILURE,
			    "%s: dwarf_next_cu_header level %d: %s",
			    __func__, walk->stack_height, dwarf_errmsg(error));
		}
		/* The CU will have a single sibling, a compilation-unit DIE. */
		res = dwarf_siblingof(walk->dbg, NULL, &last, &error);
		if (res == DW_DLV_NO_ENTRY)
			return NULL;
		if (res != DW_DLV_OK) {
			errx(EXIT_FAILURE,
			    "%s: dwarf_siblingof level %d: %s",
			    __func__, walk->stack_height, dwarf_errmsg(error));
		}
		walk->ctx.lopc = walk->ctx.hipc = 0;
		walk->stack_height = 1;
		walk->pushp = &walk->stack[walk->stack_height - 1].strstk_depth;
		walk->stack[walk->stack_height - 1].die = NULL;
	} else {
		last = walk->stack[walk->stack_height - 1].die;
		strstack_popto(&walk->ctx.symstk,
		    walk->stack[walk->stack_height - 1].strstk_depth);
	}

	res = dwarf_siblingof(walk->dbg, last, &sibling, &error);
	if (res == DW_DLV_OK) {
		walk->stack[walk->stack_height - 1].die = sibling;
	} else if (res == DW_DLV_NO_ENTRY) {
		walk->stack[walk->stack_height - 1].die = NULL;
	} else {
		errx(EXIT_FAILURE, "%s: dwarf_siblingof: %s",
		    __func__, dwarf_errmsg(error));
	}

	res = dwarf_child(last, &child, &error);
	if (res == DW_DLV_OK) {
		if (walk->stack_height == __arraycount(walk->stack)) {
			errx(EXIT_FAILURE, "%s: stack overflow",
			    __func__);
		}
		walk->stack[walk->stack_height++].die = child;
		walk->pushp = &walk->stack[walk->stack_height - 1].strstk_depth;
	} else if (res != DW_DLV_NO_ENTRY) {
		errx(EXIT_FAILURE, "%s: dwarf_child: %s",
		    __func__, dwarf_errmsg(error));
	}
	return last;
}

static Dwarf_Die
dwarf_follow_attr_to_die(Dwarf_Debug dbg, Dwarf_Die die, Dwarf_Half which)
{
	Dwarf_Die attrdie;
	Dwarf_Attribute attr;
	Dwarf_Off offset;
	Dwarf_Error error;
	int res;

	res = dwarf_attr(die, which, &attr, &error);
	if (res == DW_DLV_NO_ENTRY)
		return NULL;
	if (res != DW_DLV_OK) {
		errx(EXIT_FAILURE, "\n%s: dwarf_attr: %s",
		    __func__, dwarf_errmsg(error));
	}

	if (dwarf_global_formref(attr, &offset, &error) != DW_DLV_OK) {
		warnx("\n%s: dwarf_formref: %s", __func__, dwarf_errmsg(error));
		return NULL;
	}

	if (dwarf_offdie(dbg, offset, &attrdie, &error) != DW_DLV_OK) {
		warnx("\n%s: dwarf_offdie(, %lx, ): %s", __func__, offset, dwarf_errmsg(error));
		return NULL;
	}

	return attrdie;
}

static Dwarf_Die
dwarf_follow_spec_to_die(Dwarf_Debug dbg, Dwarf_Die die)
{
	return dwarf_follow_attr_to_die(dbg, die, DW_AT_specification);
}

static Dwarf_Die
dwarf_follow_type_to_die(Dwarf_Debug dbg, Dwarf_Die die)
{
	return dwarf_follow_attr_to_die(dbg, die, DW_AT_type);
}

static bool
get_unsigned_attribute(Dwarf_Die die, Dwarf_Half attrid, Dwarf_Unsigned *unp)
{
	Dwarf_Error error;
	Dwarf_Attribute attr;
	Dwarf_Half form;

	if (dwarf_attr(die, attrid, &attr, &error) != DW_DLV_OK) {
#if 0
		warnx("%s: missing size attribute", __func__);
#endif
		return false;
	}

	if (dwarf_whatform(attr, &form, &error) != DW_DLV_OK) {
#if 0
		warnx("%s: cannot fetch size-attribute form", __func__);
#endif
		return false;
	}

	switch (form) {
	case DW_FORM_data1:
	case DW_FORM_data2:
	case DW_FORM_data4:
	case DW_FORM_data8:
	case DW_FORM_udata:
		return dwarf_formudata(attr, unp, &error) == DW_DLV_OK;
	default:
#if 0
		warnx("%s: cannot interpret attribute", __func__);
#endif
		return false;
	}
}

static ssize_t
dwarf_type_size(Dwarf_Debug dbg, Dwarf_Die die, Dwarf_Die *typediep)
{
	Dwarf_Die typedie, otypedie;
	ssize_t size = -1;

	for (otypedie = die, typedie = die;
	     typedie != NULL;
	     otypedie = typedie,
	     typedie = dwarf_follow_type_to_die(dbg, typedie)) {
		Dwarf_Unsigned tmpsize;
		Dwarf_Error error;
		Dwarf_Half tag;

		if (get_unsigned_attribute(typedie, DW_AT_byte_size,
		    &tmpsize)) {
			size = tmpsize;
			break;
		} else if (dwarf_tag(typedie, &tag, &error) != DW_DLV_OK) {
			errx(EXIT_FAILURE, "%s: dwarf_tag: %s", __func__,
			    dwarf_errmsg(error));
		} else if (tag == DW_TAG_array_type) {
			size = dwarf_array_type_size(dbg, typedie);
			break;
		} else if (tag == DW_TAG_pointer_type) {
			Dwarf_Half addr_size;
			if (dwarf_get_address_size(dbg, &addr_size,
			    &error) != DW_DLV_OK) {
				errx(EXIT_FAILURE,
				    "%s: dwarf_get_address_size: %s", __func__,
				    dwarf_errmsg(error));
			}
			size = addr_size;
			break;
		}

		if (otypedie != die)
			dwarf_dealloc(dbg, otypedie, DW_DLA_DIE);
	}

	if (typediep != NULL && size == -1) {
		*typediep = NULL;
		dwarf_dealloc(dbg, typedie, DW_DLA_DIE);
	} else if (typediep != NULL)
		*typediep = typedie;

	if (otypedie != die)
		dwarf_dealloc(dbg, otypedie, DW_DLA_DIE);

	return size;
}

static Dwarf_Die
dwarf_aggregate_or_base_type(Dwarf_Debug dbg, Dwarf_Die die)
{
	Dwarf_Die arraydie = NULL, typedie, otypedie;
	dwarf_type_kind_t kind;

	for (otypedie = die, typedie = dwarf_follow_type_to_die(dbg, die);
	     typedie != NULL;
	     otypedie = typedie,
	     typedie = dwarf_follow_type_to_die(dbg, typedie)) {
		if (otypedie != die && otypedie != arraydie)
			dwarf_dealloc(dbg, otypedie, DW_DLA_DIE);

		kind = dwarf_type_kind(typedie);

		if (kind == DTK_POINTER)
			arraydie = typedie;
		if (kind == DTK_ARRAY)
			arraydie = typedie;
	}

	if (otypedie == die)
		return NULL;
	else
		return (arraydie != NULL) ? arraydie : otypedie;
}

static dwarf_type_kind_t
dwarf_type_kind(Dwarf_Die die)
{
	int res;
	Dwarf_Error error;
	Dwarf_Half tag;

	res = dwarf_tag(die, &tag, &error);
	if (res != DW_DLV_OK) {
		warnx("\n%s: dwarf_tag: %s", __func__, dwarf_errmsg(error));
		return DTK_UNKNOWN;
	}
	switch (tag) {
	case DW_TAG_base_type:
		return DTK_BASE;
		/*FALLTHROUGH*/
	case DW_TAG_typedef:
		return DTK_TYPEDEF;
	case DW_TAG_const_type:
		return DTK_QUALIFIER;
	case DW_TAG_array_type:
		return DTK_ARRAY;
	case DW_TAG_enumeration_type:
		return DTK_ENUM;
	case DW_TAG_pointer_type:
		return DTK_POINTER;
	case DW_TAG_structure_type:
	default:
		return DTK_OTHER;
	}
}

static void
print_op(const Dwarf_Loc *lr, dwarf_walk_ctx_t *ctx)
{
	int res;
	const char *opname;
	unsigned opcode = lr->lr_atom;
	strstack_t *ss = &ctx->symstk;

	res = dwarf_get_OP_name(opcode, &opname);
	if (res == DW_DLV_OK) {
		strstack_pushf(ss, "%s", opname);
	} else {
		strstack_pushf(ss, "<op %d>", opcode);
	}
	switch (opcode) {
	case DW_OP_bregx:
		strstack_pushf(ss, "(%0" PRIu64 ", %0" PRId64 ")",
		    lr->lr_number, lr->lr_number2);
		break;
	case DW_OP_addr:
		strstack_pushf(ss, "(%#0" PRIx64 ")", lr->lr_number);
		break;
	case DW_OP_regx:
	case DW_OP_piece:
		strstack_pushf(ss, "(%0" PRIu64 ")", lr->lr_number);
		break;
	case DW_OP_breg0: case DW_OP_breg1: case DW_OP_breg2:
	case DW_OP_breg3: case DW_OP_breg4: case DW_OP_breg5:
	case DW_OP_breg6: case DW_OP_breg7: case DW_OP_breg8:
	case DW_OP_breg9: case DW_OP_breg10:
	case DW_OP_breg11: case DW_OP_breg12: case DW_OP_breg13:
	case DW_OP_breg14: case DW_OP_breg15: case DW_OP_breg16:
	case DW_OP_breg17: case DW_OP_breg18: case DW_OP_breg19:
	case DW_OP_breg20: case DW_OP_breg21: case DW_OP_breg22:
	case DW_OP_breg23: case DW_OP_breg24: case DW_OP_breg25:
	case DW_OP_breg26: case DW_OP_breg27: case DW_OP_breg28:
	case DW_OP_breg29: case DW_OP_breg30: case DW_OP_breg31:
	case DW_OP_fbreg:
		strstack_pushf(ss, "(%0" PRId64 ")", lr->lr_number);
		break;
	case DW_OP_reg0:
	case DW_OP_reg1:
	case DW_OP_reg2:
	case DW_OP_reg3:
	case DW_OP_reg4:
	case DW_OP_reg5:
	case DW_OP_reg6:
	default:
		break;
	}
}

static bool
check_line(Dwarf_Debug dbg, Dwarf_Die die, dwarf_walk_ctx_t *ctx,
    char **filenamep, unsigned int *linep, int *colp)
{
	Dwarf_Addr addr, paddr;
	Dwarf_Line *line;
	Dwarf_Signed nlines;
	Dwarf_Error err;
	int i;
	Dwarf_Unsigned lineno, plineno;
	Dwarf_Signed colno;
	char *filename, *pfilename = NULL;

	if (!ctx->have_insnptr)
		return false;

	if (dwarf_srclines(die, &line, &nlines, &err) != DW_DLV_OK)
		return false;

	for (i = 0; i < nlines; i++) {
		if (dwarf_lineaddr(line[i], &addr, &err) != DW_DLV_OK) {
			errx(EXIT_FAILURE, "%s: dwarf_lineaddr: %s", __func__,
			    dwarf_errmsg(err));
		}
		if (dwarf_lineno(line[i], &lineno, &err) != DW_DLV_OK) {
			errx(EXIT_FAILURE, "%s: dwarf_lineno: %s", __func__,
			    dwarf_errmsg(err));
		}
		if (dwarf_lineoff(line[i], &colno, &err) != DW_DLV_OK) {
			errx(EXIT_FAILURE, "%s: dwarf_lineoff: %s", __func__,
			    dwarf_errmsg(err));
		}
		if (dwarf_linesrc(line[i], &filename, &err) != DW_DLV_OK) {
			errx(EXIT_FAILURE, "%s: dwarf_linesrc: %s", __func__,
			    dwarf_errmsg(err));
		}
		if (addr == ctx->insnptr)
			break;
		if (paddr < ctx->insnptr && ctx->insnptr < addr) {
			addr = paddr;
			filename = pfilename;
			lineno = plineno;
			colno = -1;
			break;
		}
		paddr = addr;
		pfilename = filename;
		plineno = lineno;
	}
	dwarf_srclines_dealloc(dbg, line, nlines);
	if (i < nlines) {
		*filenamep = filename;
		*linep = lineno;
		*colp = colno;
		return true;
	}
	return false;
}

static bool
check_location(Dwarf_Debug dbg, Dwarf_Die die, dwarf_walk_ctx_t *ctx)
{
	Dwarf_Error error;
	Dwarf_Attribute loc_attr;
	Dwarf_Locdesc *ld;
	Dwarf_Locdesc **locdescp;
	Dwarf_Signed nlocdescs;
	int res;
	Dwarf_Half loc_form;
	Dwarf_Ptr loc_ptr;
	Dwarf_Unsigned loc_len;
	strstack_t *ss = &ctx->locstk;

	if (dwarf_attr(die, DW_AT_location, &loc_attr, &error) != DW_DLV_OK)
		return false;

	if (dwarf_formexprloc(loc_attr, &loc_len, &loc_ptr,
	                           &error) == DW_DLV_OK &&
	    dwarf_loclist_from_expr(dbg, loc_ptr, loc_len, &ld,
	                            &nlocdescs, &error) == DW_DLV_OK) {
		int j;
		bool insn_match = false;
		const char *delim = "at ";
		struct {
			Dwarf_Addr lopc, hipc;
		} inner;

		assert(nlocdescs == 1);

		inner.lopc = ld->ld_lopc;
		inner.hipc = ld->ld_hipc;

		assert(inner.lopc < inner.hipc);

		inner.lopc = MAX(ctx->lopc, inner.lopc);
		inner.hipc = MIN(ctx->hipc, inner.hipc);

		Dwarf_Loc *lr0 = &ld->ld_s[0];

		if (ctx->have_insnptr &&
		    inner.lopc <= ctx->insnptr && ctx->insnptr < inner.hipc) {
			strstack_pushf(&ctx->locstk,
			    " at pc %0" PRIx64
			    " in pc %0" PRIx64 " - %0" PRIx64, ctx->insnptr,
			    inner.lopc, inner.hipc);
			insn_match = true;
		} else {
			strstack_pushf(&ctx->locstk,
			    " in pc %0" PRIx64 " - %0" PRIx64,
			    inner.lopc, inner.hipc);
		}

		if (ctx->have_dataptr && ld->ld_cents == 1 &&
			 lr0->lr_atom == DW_OP_addr &&
			 lr0->lr_number <= ctx->dataptr) {
			strstack_pushf(ss, " at static 0x%0" PRIx64,
			    ctx->dataptr);
			ctx->residue = ctx->dataptr - lr0->lr_number;
			return true;
		} else if (insn_match && ctx->have_dataptr &&
			   ctx->have_frameptr &&
			   ctx->have_cfa_offset && ld->ld_cents == 1 &&
			   lr0->lr_atom == DW_OP_fbreg &&
			   ctx->frameptr - ctx->cfa_offset +
			   (int64_t)lr0->lr_number <= ctx->dataptr) {
			strstack_pushf(ss, " on stack at 0x%0" PRIx64
			    " - %" PRId64 " + %" PRId64 " = 0x%0" PRIx64,
			    ctx->frameptr, ctx->cfa_offset, lr0->lr_number,
			    ctx->dataptr);
			ctx->residue = ctx->dataptr -
			    (ctx->frameptr - ctx->cfa_offset +
			     (int64_t)lr0->lr_number);
			return true;
		} else for (j = 0; j < ld->ld_cents; j++) {
			Dwarf_Loc *lr = &ld->ld_s[j];
			strstack_pushf(ss, "%s", delim);
			print_op(lr, ctx);
			delim = " or at";
		}
#if 0
		for (i = 0; i < nlocdescs; i++)
			dwarf_dealloc(dbg, locdesc[i].ld_s, DW_DLA_LOC_BLOCK);
		dwarf_dealloc(dbg, locdesc, DW_DLA_LOCDESC);
#endif
	} else if ((res = dwarf_loclist_n(loc_attr, &locdescp, &nlocdescs,
	                                  &error)) == DW_DLV_OK) {
		int i, j;

		const char *odelim = " ";

		for (i = 0; i < nlocdescs; i++) {
			ld = locdescp[i];
			struct {
				Dwarf_Addr lopc, hipc;
			} inner;
			bool insn_match = false;

			inner.lopc = ld->ld_lopc;
			inner.hipc = ld->ld_hipc;

			if (inner.lopc == inner.hipc)
				continue;

			if (inner.lopc < ctx->lopc || ctx->hipc < inner.lopc) {
				inner.lopc += ctx->lopc;
				inner.hipc += ctx->lopc;
			}

			strstack_pushf(ss, "%s", odelim);
			odelim = ", ";

			strstack_pushf(ss, " pc");

			if (ctx->have_insnptr &&
			    inner.lopc <= ctx->insnptr &&
			    ctx->insnptr < inner.hipc) {
				strstack_pushf(ss,
				    " at %0" PRIx64, ctx->insnptr);
				insn_match = true;
			}

			strstack_pushf(ss, " in %0" PRIx64 " - %0" PRIx64 ": ",
			    inner.lopc, inner.hipc);

			for (j = 0; j < ld->ld_cents; j++) {
				Dwarf_Loc *lr = &ld->ld_s[j];
				strstack_pushf(ss, " ");
				print_op(lr, ctx);
			}
		}
		assert(nlocdescs > 0);
#if 0
		for (i = 0; i < nlocdescs; i++) {
			if (locdescp[i]->ld_cents > 0) {
				dwarf_dealloc(dbg, locdescp[i]->ld_s,
				    DW_DLA_LOC_BLOCK);
			}
			dwarf_dealloc(dbg, locdescp[i], DW_DLA_LOCDESC);
		}
		dwarf_dealloc(dbg, locdescp, DW_DLA_LIST);
#endif
	} else {
		const char *formname;

		Dwarf_Error nerror;
		if (dwarf_whatform(loc_attr, &loc_form, &nerror) != DW_DLV_OK ||
		    dwarf_get_FORM_name(loc_form, &formname) != DW_DLV_OK)
			strstack_pushf(ss, "unknown");
		else {
			strstack_pushf(ss, "unknown form %s err %s", formname,
			    dwarf_errmsg(error));
		}
	}
	return false;
}

static int
locdesc_to_regnum(Dwarf_Locdesc *ld)
{
	int regnum = 0;
	Dwarf_Loc *lr0;

	if (ld->ld_cents != 1)
		return -1;

	lr0 = &ld->ld_s[0];

	switch (lr0->lr_atom) {
	/* In each of the following cases, the relationship
	 * between DW_OP_fbreg(ofs) and the CFA is
	 * straightforward.
	 */
	case DW_OP_regx:
		return lr0->lr_number;
	case DW_OP_reg31:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg30:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg29:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg28:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg27:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg26:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg25:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg24:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg23:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg22:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg21:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg20:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg19:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg18:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg17:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg16:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg15:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg14:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg13:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg12:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg11:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg10:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg9:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg8:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg7:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg6:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg5:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg4:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg3:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg2:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg1:
		regnum++;	/*FALLTHROUGH*/
	case DW_OP_reg0:
		return regnum;
	default:
		return -1;
	}
}

static ssize_t
dwarf_array_type_size(Dwarf_Debug dbg, Dwarf_Die typedie)
{
	int res;
	Dwarf_Die child, sibling;
	Dwarf_Error error;
	Dwarf_Half tag;
	Dwarf_Unsigned count;
	ssize_t eltsz, totalsz;

	res = dwarf_child(typedie, &child, &error);
	if (res == DW_DLV_NO_ENTRY)
		return -1;

	if (res != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s: dwarf_child: %s",
		    __func__, dwarf_errmsg(error));
	}

	eltsz = dwarf_type_size(dbg, dwarf_follow_type_to_die(dbg, typedie),
	    NULL);

	if (eltsz < 0)
		return -1;

	totalsz = eltsz;

	for (;;) {
		res = dwarf_tag(child, &tag, &error);
		if (res != DW_DLV_OK) {
			errx(EXIT_FAILURE, "%s.%d: dwarf_tag %s", __func__,
			    __LINE__, dwarf_errmsg(error));
		}
		if (tag != DW_TAG_subrange_type)
			goto next;

		/* TBD handle lower/upper bound */
		if (!get_unsigned_attribute(child, DW_AT_count, &count)) {
			errx(EXIT_FAILURE, "%s: no count attribute",
			    __func__);
		}

		/* TBD try for the subrange's DW_AT_byte_size, which
		 * overrides the type's size
		 */

		totalsz *= count;

next:
		res = dwarf_siblingof(dbg, child, &sibling, &error);
		if (res == DW_DLV_NO_ENTRY)
			break;

		if (res != DW_DLV_OK) {
			errx(EXIT_FAILURE, "%s: dwarf_siblingof: %s",
			    __func__, dwarf_errmsg(error));
		}
		child = sibling;
	}

	return totalsz;
}

static bool
walk_elements(Dwarf_Debug dbg, Dwarf_Die typedie, dwarf_walk_ctx_t *ctx)
{
	int res;
	Dwarf_Die child, sibling;
	Dwarf_Error error;
	Dwarf_Half tag;
	Dwarf_Die elementtypedie;
	Dwarf_Unsigned count;
	ssize_t element_size;
	strstack_t *ss = &ctx->symstk;
	Dwarf_Unsigned dim[10];
	int i, idx, ndims = 0;

	res = dwarf_child(typedie, &child, &error);
	if (res == DW_DLV_NO_ENTRY)
		return false;

	if (res != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s: dwarf_child: %s",
		    __func__, dwarf_errmsg(error));
	}

	element_size = dwarf_type_size(dbg, dwarf_follow_type_to_die(dbg, typedie), &elementtypedie);

	if (element_size < 0)
		goto next;

	for (;;) {
		res = dwarf_tag(child, &tag, &error);
		if (res != DW_DLV_OK) {
			errx(EXIT_FAILURE, "%s.%d: dwarf_tag %s", __func__,
			    __LINE__, dwarf_errmsg(error));
		}
		if (tag != DW_TAG_subrange_type)
			goto next;

		/* TBD handle lower/upper bound */
		if (!get_unsigned_attribute(child, DW_AT_count, &count)) {
			errx(EXIT_FAILURE, "%s: no count attribute",
			    __func__);
		}

		if (ndims++ == __arraycount(dim)) {
			errx(EXIT_FAILURE, "%s: array dimensions > %zu",
			    __func__, __arraycount(dim));
		}

		/* TBD try for the subrange's DW_AT_byte_size, which
		 * overrides the type's size
		 */

		dim[ndims - 1] = count;

next:
		res = dwarf_siblingof(dbg, child, &sibling, &error);
		if (res == DW_DLV_NO_ENTRY)
			break;

		if (res != DW_DLV_OK) {
			errx(EXIT_FAILURE, "%s: dwarf_siblingof: %s",
			    __func__, dwarf_errmsg(error));
		}
		child = sibling;
	}

	/* TBD handle column-major arrays.  This works for row-major arrays.
	 */
	for (i = ndims - 2; i >= 0; i--)
		dim[i] *= dim[i + 1];

	for (i = 0; i < ndims; i++) {
		dim[i] *= element_size;
	}

	for (i = 0; i < ndims; i++) {
		ssize_t sz;

		sz = (i < ndims - 1) ? dim[i + 1] : element_size;

		if (ctx->residue >= dim[i])
			return false;

		idx = ctx->residue / sz;
		ctx->residue -= idx * sz;
		strstack_pushf(ss, "[%d]", idx);
	}

	res = dwarf_tag(elementtypedie, &tag, &error);
	if (res != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s.%d: dwarf_tag %s", __func__,
		    __LINE__, dwarf_errmsg(error));
	}
	if (tag == DW_TAG_structure_type)
		(void)walk_members(dbg, elementtypedie, ctx);
	else if (tag == DW_TAG_array_type)
		(void)walk_elements(dbg, elementtypedie, ctx);
	return true;
}

static bool
walk_members(Dwarf_Debug dbg, Dwarf_Die typedie, dwarf_walk_ctx_t *ctx)
{
	int res;
	Dwarf_Die child, sibling;
	Dwarf_Error error;
	Dwarf_Half tag;
	Dwarf_Die membertypedie;
	Dwarf_Unsigned member_location;
	ssize_t member_size;
	strstack_t *ss = &ctx->symstk;
	char *name = NULL;

	res = dwarf_child(typedie, &child, &error);
	if (res == DW_DLV_NO_ENTRY)
		return false;

	if (res != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s: dwarf_child: %s",
		    __func__, dwarf_errmsg(error));
	}

	for (;;) {
		res = dwarf_tag(child, &tag, &error);
		if (res != DW_DLV_OK) {
			errx(EXIT_FAILURE, "%s.%d: dwarf_tag %s", __func__,
			    __LINE__, dwarf_errmsg(error));
		}
		if (tag != DW_TAG_member)
			goto next;

		if (!get_unsigned_attribute(child,
		    DW_AT_data_member_location, &member_location))
			goto next;

		res = dwarf_diename(child, &name, &error);
		if (res != DW_DLV_OK)
			goto next;

		member_size = dwarf_type_size(dbg, dwarf_follow_type_to_die(dbg, child), &membertypedie);

		if (member_size < 0)
			goto next;

		if (ctx->residue < member_location ||
		    member_location + member_size <= ctx->residue)
			goto next;

		strstack_pushf(ss, ".%s", name);

		ctx->residue -= member_location;

		res = dwarf_tag(membertypedie, &tag, &error);
		if (res != DW_DLV_OK) {
			errx(EXIT_FAILURE, "%s.%d: dwarf_tag %s", __func__,
			    __LINE__, dwarf_errmsg(error));
		}
		if (tag == DW_TAG_structure_type)
			(void)walk_members(dbg, membertypedie, ctx);
		else if (tag == DW_TAG_array_type)
			(void)walk_elements(dbg, membertypedie, ctx);
		return true;
next:
		res = dwarf_siblingof(dbg, child, &sibling, &error);
		if (res == DW_DLV_NO_ENTRY)
			return false;

		if (res != DW_DLV_OK) {
			errx(EXIT_FAILURE, "%s: dwarf_siblingof: %s",
			    __func__, dwarf_errmsg(error));
		}
		child = sibling;
	}
	return false;
}

static void
print_die_data(Dwarf_Debug dbg, Dwarf_Die die, dwarf_walk_ctx_t *ctx)
{
	char *name;
	Dwarf_Error error;
	Dwarf_Half lopc_form, hipc_form, tag = 0;
	int res;
	Dwarf_Attribute lopc_attr, hipc_attr;
	struct {
		Dwarf_Addr lopc, hipc;
	} inner;
	Dwarf_Unsigned hipcofs;
	strstack_t *ss = &ctx->symstk;
	int depth = -1;
	Dwarf_Half typetag;
	Dwarf_Die typedie;
	ssize_t size;
	unsigned int line;
	int column;
	char *filename;

	res = dwarf_tag(die, &tag, &error);
	if (res != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s: dwarf_tag", __func__);
	}
	if ((res = dwarf_diename(die, &name, &error)) == DW_DLV_NO_ENTRY) {
		Dwarf_Die specdie;

		if (tag != DW_TAG_subprogram ||
		    (specdie = dwarf_follow_spec_to_die(dbg, die)) == NULL)
			return;
		res = dwarf_diename(specdie, &name, &error);
		dwarf_dealloc(dbg, specdie, DW_DLA_DIE);
	}
	switch (res) {
	case DW_DLV_ERROR:
	default:
		errx(EXIT_FAILURE, "\n%s.%d: dwarf_diename: %s",
		    __func__, __LINE__, dwarf_errmsg(error));
		break;
	case DW_DLV_NO_ENTRY:
		return;
	case DW_DLV_OK:
#if 0
		printf(" %s", name);
		dwarf_dealloc(dbg, name, DW_DLA_STRING);
#endif
		break;
	}

	if (tag == DW_TAG_compile_unit) {
#if 0
		if (ctx->cu_stkdepth != -1) {
			strstack_popto(ss, ctx->cu_stkdepth);
			if (ctx->subprogram_stkdepth > ctx->cu_stkdepth)
				ctx->subprogram_stkdepth = -1;
		}
#endif

		if (!ctx->have_insnptr || ctx->have_dataptr ||
		    ctx->have_frameptr)
			ctx->cu_stkdepth = strstack_pushf(ss, "%s", name);
	} else if (tag == DW_TAG_subprogram) {
		Dwarf_Attribute loc_attr;
		Dwarf_Locdesc *ld;
		Dwarf_Signed nlocdescs;
		Dwarf_Ptr loc_ptr;
		Dwarf_Unsigned loc_len;

#if 0
		if (ctx->subprogram_stkdepth != -1)
			strstack_popto(ss, ctx->subprogram_stkdepth);
#endif

		ctx->subprogram_stkdepth = strstack_pushf(ss, ";%s", name);

		if (dwarf_attr(die, DW_AT_frame_base, &loc_attr, &error) != DW_DLV_OK) {
			/* no frame base */
		} else if (dwarf_formexprloc(loc_attr, &loc_len, &loc_ptr,
			     &error) != DW_DLV_OK ||
			 dwarf_loclist_from_expr(dbg, loc_ptr, loc_len, &ld,
			     &nlocdescs, &error) != DW_DLV_OK) {
			/* malformed frame base */
		} else if (nlocdescs != 1) {
			/* too few/many frame-base location descriptors */
		} else if (ld->ld_cents != 1) {
			/* too few/many frame-base location ops */
		} else {
			int regnum;
			Dwarf_Fde fde;
			Dwarf_Addr lopc, hipc;
			Dwarf_Small exprtype;
			Dwarf_Signed offset_relevant;
			Dwarf_Signed offset, fde_regnum;
			Dwarf_Addr row_pc;
			Dwarf_Ptr block_ptr;
			Dwarf_Loc *lr0 = &ld->ld_s[0];

			if (lr0->lr_atom == DW_OP_call_frame_cfa) {
				ctx->have_cfa_offset = true;
				ctx->cfa_offset = 0;
			} else if ((regnum = locdesc_to_regnum(ld)) == -1) {
				ctx->have_cfa_offset = false;
			} else if (!ctx->have_insnptr) {
				ctx->have_cfa_offset = false;
			} else if (ctx->fde_list == NULL &&
			    dwarf_get_fde_list(dbg, &ctx->cie_list,
				&ctx->ncies, &ctx->fde_list,
				&ctx->nfdes, &error) != DW_DLV_OK &&
			    dwarf_get_fde_list_eh(dbg, &ctx->cie_list,
				&ctx->ncies, &ctx->fde_list,
				&ctx->nfdes, &error) != DW_DLV_OK) {
				/* could not retrieve FDEs */
				ctx->have_cfa_offset = false;
			} else if (dwarf_get_fde_at_pc(ctx->fde_list,
				   ctx->insnptr, &fde, &lopc, &hipc,
				   &error) != DW_DLV_OK) {
				/* FDE unknown for PC */
				ctx->have_cfa_offset = false;
			} else if (dwarf_get_fde_info_for_cfa_reg3(fde,
				 ctx->insnptr, &exprtype, &offset_relevant,
				 &fde_regnum, &offset, &block_ptr,
				 &row_pc, &error) != DW_DLV_OK) {
				/* cannot get CFA FDE info */
				ctx->have_cfa_offset = false;
			} else if (exprtype != DW_EXPR_OFFSET) {
				/* expression type not understood */
				ctx->have_cfa_offset = false;
			} else if (fde_regnum == regnum &&
				   offset_relevant == 1) {
				ctx->cfa_offset = offset;
				ctx->have_cfa_offset = true;
			} else if (fde_regnum == regnum) {
				ctx->cfa_offset = 0;
				ctx->have_cfa_offset = true;
			} else {
				/* CFA register mismatch */
				ctx->have_cfa_offset = false;
			}
		}
#if 0
		for (i = 0; i < nlocdescs; i++)
			dwarf_dealloc(dbg, locdesc[i].ld_s, DW_DLA_LOC_BLOCK);
		dwarf_dealloc(dbg, locdesc, DW_DLA_LOCDESC);
#endif
	} else {
		depth = strstack_pushf(ss, ";;%s", name);
	}

	dwarf_dealloc(dbg, name, DW_DLA_STRING);

	if (dwarf_attr(die, DW_AT_high_pc, &hipc_attr, &error) == DW_DLV_OK &&
	    dwarf_attr(die, DW_AT_low_pc, &lopc_attr, &error) == DW_DLV_OK &&
	    dwarf_whatform(lopc_attr, &lopc_form, &error) == DW_DLV_OK &&
	    dwarf_whatform(hipc_attr, &hipc_form, &error) == DW_DLV_OK) {
		if (dwarf_formaddr(lopc_attr, &inner.lopc, &error) !=
		    DW_DLV_OK) {
			errx(EXIT_FAILURE, "\n%s: dwarf_formaddr: %s",
			    __func__, dwarf_errmsg(error));
		}
		switch (hipc_form) {
		case DW_FORM_data1:
		case DW_FORM_data2:
		case DW_FORM_data4:
		case DW_FORM_data8:
		case DW_FORM_udata:
			if (dwarf_formudata(hipc_attr, &hipcofs, &error) !=
			    DW_DLV_OK) {
				errx(EXIT_FAILURE, "\n%s: dwarf_formudata: %s",
				    __func__, dwarf_errmsg(error));
			}
			inner.hipc = inner.lopc + hipcofs;
			break;
		case DW_FORM_addr:
			if (dwarf_formaddr(hipc_attr, &inner.hipc, &error) !=
			    DW_DLV_OK) {
				errx(EXIT_FAILURE, "\n%s: dwarf_formaddr: %s",
				    __func__, dwarf_errmsg(error));
			}
			break;
		default:
			errx(EXIT_FAILURE, "%s: unknown hipc form", __func__);
		}
		if (tag == DW_TAG_compile_unit) {
			ctx->lopc = inner.lopc;
			ctx->hipc = inner.hipc;
		}
	}

	if (tag == DW_TAG_compile_unit &&
	    ctx->have_insnptr && !ctx->have_dataptr &&
	    !ctx->have_frameptr && check_line(dbg, die, ctx, &filename, &line,
	        &column)) {
		strstack_pushf(ss, "%s:%d", filename, line);
		if (column != -1)
			strstack_pushf(ss, ":%d", column);
	} else if (tag == DW_TAG_subprogram && ctx->have_insnptr &&
	    !ctx->have_dataptr && !ctx->have_frameptr) {
		if (inner.lopc <= ctx->insnptr && ctx->insnptr <= inner.hipc) {
			if (ctx->print_regex)
				strstack_pushf(ss, " {0x0*%" PRIx64 "}", ctx->insnptr);
			else if (ctx->print_address)
				strstack_pushf(ss, " 0x%" PRIx64, ctx->insnptr);
			strstack_fprintf(stdout, ss);
			printf("\n");
		}
	} else if (check_location(dbg, die, ctx) &&
	    (typedie = dwarf_aggregate_or_base_type(dbg, die)) != NULL &&
	    (size = dwarf_type_size(dbg, typedie, NULL)) > 0 &&
	    ctx->residue < size) {

		/* if typedie is non-NULL and a _structure_type
		 * or _array_type, find the member or index;
		 * recurse.
		 */
		if (typedie == NULL ||
		    dwarf_tag(typedie, &typetag, &error) != DW_DLV_OK)
			;
		else if (typetag == DW_TAG_structure_type &&
			 !walk_members(dbg, typedie, ctx))
			;
		else if (typetag == DW_TAG_array_type &&
			 !walk_elements(dbg, typedie, ctx))
			;
		else {
			if (!ctx->have_dataptr)
				; // do nothing
			else if (ctx->print_address)
				strstack_pushf(ss, " 0x%" PRIx64, ctx->dataptr);
			else if (!ctx->print_regex)
				; // do nothing
			else if (ctx->have_insnptr && ctx->have_frameptr) {
				// a little too lenient, this matches
				// [... : .../... .../... .../...]
				// [... : .../... .../...]
				// [... : .../...]
				strstack_pushf(ss, ";;\\[0x0*%" PRIx64
				    " : \\(0x[0-9a-f]\\+\\/0x[0-9a-f]\\+ \\)"
				    "\\?0x0*%" PRIx64 "\\/0x0*%" PRIx64
				    "\\( 0x[0-9a-f]\\+\\/0x[0-9a-f]\\+\\)\\?"
				    "\\]",
				    ctx->dataptr, ctx->insnptr, ctx->frameptr);
			} else {
				strstack_pushf(ss, ";;[0x0*%" PRIx64 "]",
				    ctx->dataptr);
			}
			strstack_fprintf(stdout, ss);
			printf("\n");
		}
	}

	if (depth != -1)
		strstack_popto(ss, depth);
}

static void __dead
usage(const char *progname)
{
	fprintf(stderr, "usage: %s [-a | -r] "
	    "[-d data address] "
	    "[-f DWARF Canonical Frame Address (CFA) ] "
	    "[-i instruction pointer] object-file\n", progname);
	exit(EXIT_FAILURE);
}

int 
main(int argc, char **argv)
{
	Dwarf_Debug dbg = 0;
	Dwarf_Die die;
	int ch, fd, ofs, rc;
	Dwarf_Error error;
	dwarf_walk_t walk;
	const char *progname = argv[0];
	dwarf_walk_params_t clparams = {
		  .have_dataptr = false
		, .have_frameptr = false
		, .have_insnptr = false
		, .print_address = false
		, .print_regex = false
		, .predicate = NULL};

	while ((ch = getopt(argc, argv, "ad:f:i:r")) != -1) {
		switch (ch) {
		case 'r':
			clparams.print_regex = true;
			break;
		case 'a':
			clparams.print_address = true;
			break;
		case 'f':
			if (clparams.have_frameptr) {
				errx(EXIT_FAILURE,
				    "%s: too many frame pointers", __func__);
			}
			clparams.have_frameptr = true;
			rc = sscanf(optarg, "0x%" SCNx64 "%n",
			    &clparams.frameptr, &ofs);
			if (rc != 1 || optarg[ofs] != '\0') {
				errx(EXIT_FAILURE,
				    "%s: malformed frame pointer", __func__);
			}
			break;
		case 'i':
			if (clparams.have_insnptr) {
				errx(EXIT_FAILURE,
				    "%s: too many instruction pointers",
				    __func__);
			}
			clparams.have_insnptr = true;
			rc = sscanf(optarg, "0x%" SCNx64 "%n",
			    &clparams.insnptr, &ofs);
			if (rc != 1 || optarg[ofs] != '\0') {
				errx(EXIT_FAILURE,
				    "%s: malformed instruction pointer",
				    __func__);
			}
			break;
		case 'd':
			if (clparams.have_dataptr) {
				errx(EXIT_FAILURE,
				    "%s: too many data pointers", __func__);
			}
			clparams.have_dataptr = true;
			rc = sscanf(optarg, "0x%" SCNx64 "%n",
			    &clparams.dataptr, &ofs);
			if (rc != 1 || optarg[ofs] != '\0') {
				errx(EXIT_FAILURE,
				    "%s: malformed data pointer", __func__);
			}
			break;
		default:
			usage(progname);
		}
	}

	argc -= optind;
	argv += optind;

	if (argc < 1)
		usage(progname);

	if (clparams.print_address && clparams.print_regex) {
		fprintf(stderr, "-r and -a are mutually exclusive\n");
		usage(progname);
	}

	if ((fd = open(argv[0], O_RDONLY)) == -1)
		err(EXIT_FAILURE, "%s: open(\"%s\")", __func__, argv[0]);

	rc = dwarf_init(fd, DW_DLC_READ, NULL, NULL, &dbg, &error);
	if (rc != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s: dwarf_init: %s", __func__,
		    dwarf_errmsg(error));
	}

	clparams.dbg = dbg;

	dwarf_walk_params_t params[2] = {clparams, clparams};
	int nparams = 0;
	int paramidx = 0;
	bool replenish = true;

	if (clparams.have_dataptr || clparams.have_insnptr) {
		nparams = 1;
		replenish = false;
	}

	for (paramidx = 0;; paramidx++) {
		char lbuf[sizeof("[0x0123456789abcdef : 0x0123456789abcdef/0x0123456789abcdef 0x0123456789abcdef/0x0123456789abcdef]\n")];

		if (paramidx < nparams)
			;	// do nothing
		else if (!replenish)
			break;
		else if (fgets(lbuf, sizeof(lbuf), stdin) == NULL)
			break;
		else if (sscanf(lbuf,
		    "[0x%" SCNx64 " : 0x%" SCNx64 "/0x%" SCNx64
		    " 0x%" SCNx64 "/0x%" SCNx64 "]\n",
		    &params[0].dataptr,
		    &params[0].insnptr, &params[0].frameptr,
		    &params[1].insnptr, &params[1].frameptr) == 5) {
			params[1].dataptr = params[0].dataptr;
			params[0].have_dataptr = params[1].have_dataptr =
			    params[0].have_insnptr = params[1].have_insnptr =
			    params[0].have_frameptr = params[1].have_frameptr =
			    true;
			paramidx = 0;
			nparams = 2;
		} else if (sscanf(lbuf,
		    "[0x%" SCNx64 " : 0x%" SCNx64 "/0x%" SCNx64 "]\n",
		    &params[0].dataptr,
		    &params[0].insnptr, &params[0].frameptr) == 3) {
			params[0].have_dataptr = params[0].have_insnptr =
			    params[0].have_frameptr = true;
			paramidx = 0;
			nparams = 1;
		} else if (sscanf(lbuf, "[0x%" SCNx64 "]\n",
		    &params[0].dataptr) == 1) {
			params[0].have_dataptr = true;
			paramidx = 0;
			nparams = 1;
		} else if (sscanf(lbuf, "{0x%" SCNx64 "}\n",
		    &params[0].insnptr) == 1) {
			params[0].have_insnptr = true;
			params[0].have_dataptr = params[0].have_frameptr =
			    false;
			paramidx = 0;
			nparams = 1;
		} else {
			errx(EXIT_FAILURE, "%s: syntax error: `%s`", __func__,
			    lbuf);
		}
		for (die = dwarf_walk_first(&walk, &params[paramidx]);
		     die != NULL;
		     die = dwarf_walk_next(&walk)) {
			print_die_data(dbg, die, &walk.ctx);
			dwarf_dealloc(dbg, die, DW_DLA_DIE);
		}
	}

	rc = dwarf_finish(dbg, &error);
	if (rc != DW_DLV_OK) {
		warnx("%s: dwarf_finish: %s", __func__, dwarf_errmsg(error));
	}
	close(fd);
	return EXIT_SUCCESS;
}
