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

typedef struct _dwarf_walk_ctx {
	Dwarf_Addr lopc, hipc;
	Dwarf_Cie *cie_list;
	Dwarf_Signed ncies;
	Dwarf_Fde *fde_list;
	Dwarf_Signed nfdes;
	Dwarf_Signed cfa_offset;
	bool have_cfa_offset;
	int subprogram_stkdepth, cu_stkdepth;
	strstack_t symstk;
	int residue;
} dwarf_walk_ctx_t;

typedef struct _dwarf_walk {
	int stack_height;
	struct {
		Dwarf_Die die;
	} stack[16];
	Dwarf_Debug dbg;
	dwarf_walk_ctx_t ctx;
	dwarf_walk_predicate_t predicate;
} dwarf_walk_t;

typedef enum _dwarf_type_kind {
	  DTK_QUALIFIER = 0
	, DTK_POINTER
	, DTK_BASE
} dwarf_type_kind_t;

static ssize_t sizeof_type_die(Dwarf_Die, dwarf_walk_ctx_t *);
static Dwarf_Die dwarf_walk_first(Dwarf_Debug, dwarf_walk_t *,
    dwarf_walk_predicate_t);
static Dwarf_Die dwarf_walk_next(dwarf_walk_t *);
static Dwarf_Die dwarf_walk_next_in_tree(dwarf_walk_t *);
static char *dwarf_c_typestring(Dwarf_Debug, Dwarf_Die, dwarf_walk_ctx_t *);
static char *dwarf_c_typestring_component(Dwarf_Debug, Dwarf_Die,
    dwarf_type_kind_t *);
static void print_die_data(Dwarf_Debug, Dwarf_Die, dwarf_walk_ctx_t *);

static int verbosity = 0;
static bool have_dataptr = false, have_frameptr = false, have_insnptr = false;
static uint64_t dataptr, frameptr, insnptr;

static Dwarf_Die
dwarf_walk_first(Dwarf_Debug dbg, dwarf_walk_t *walk,
    dwarf_walk_predicate_t predicate)
{
	walk->stack_height = 0;
	walk->dbg = dbg;
	walk->predicate = predicate;
	walk->ctx.cie_list = NULL;
	walk->ctx.fde_list = NULL;
	walk->ctx.have_cfa_offset = false;
	walk->ctx.ncies = walk->ctx.nfdes = 0;
	walk->ctx.cu_stkdepth = -1;
	walk->ctx.subprogram_stkdepth = -1;
	strstack_init(&walk->ctx.symstk);

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

	if (walk->stack_height == 0) {
		res = dwarf_next_cu_header(walk->dbg, &cu_header_length,
		    &version_stamp, &abbrev_offset, &address_size,
		    &next_cu_header, &error);

		if (res == DW_DLV_NO_ENTRY)
			return NULL;

		if (verbosity > 0)
			printf("version %hd\n", version_stamp);

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
	} else
		last = walk->stack[walk->stack_height - 1].die;

	res = dwarf_siblingof(walk->dbg, last, &sibling, &error);
	if (res == DW_DLV_OK) {
		walk->stack[walk->stack_height - 1].die = sibling;
	} else if (res == DW_DLV_NO_ENTRY) {
		walk->stack[--walk->stack_height].die = NULL;
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
	} else if (res != DW_DLV_NO_ENTRY) {
		errx(EXIT_FAILURE, "%s: dwarf_child: %s",
		    __func__, dwarf_errmsg(error));
	}
	return last;
}

static char *
new_empty_string(void)
{
	return strdup("");
}

static Dwarf_Die
dwarf_follow_type_to_die(Dwarf_Debug dbg, Dwarf_Die die)
{
	Dwarf_Die typedie;
	Dwarf_Attribute attr;
	Dwarf_Off offset;
	Dwarf_Error error;
	int res;

	res = dwarf_attr(die, DW_AT_type, &attr, &error);
	if (res == DW_DLV_NO_ENTRY) {
		return NULL;
	} else if (res != DW_DLV_OK) {
		errx(EXIT_FAILURE, "\n%s: dwarf_attr: %s",
		    __func__, dwarf_errmsg(error));
	}

	if (dwarf_global_formref(attr, &offset, &error) != DW_DLV_OK) {
		warnx("\n%s: dwarf_formref: %s", __func__, dwarf_errmsg(error));
		return NULL;
	}

	if (dwarf_offdie(dbg, offset, &typedie, &error) != DW_DLV_OK) {
		warnx("\n%s: dwarf_offdie(, %lx, ): %s", __func__, offset, dwarf_errmsg(error));
		return NULL;
	}

	return typedie;
}

static ssize_t
sizeof_type_die(Dwarf_Die die, dwarf_walk_ctx_t *ctx)
{
	Dwarf_Error error;
	Dwarf_Attribute size_attr;
	Dwarf_Half size_form;
	Dwarf_Unsigned size;
	strstack_t *ss = &ctx->symstk;

	if (dwarf_attr(die, DW_AT_byte_size, &size_attr, &error) != DW_DLV_OK) {
		strstack_pushf(ss, " missing size attribute");
		return -1;
	}

	if (dwarf_whatform(size_attr, &size_form, &error) != DW_DLV_OK) {
		strstack_pushf(ss, " cannot fetch size-attribute form");
		return -1;
	}

	switch (size_form) {
	case DW_FORM_data1:
	case DW_FORM_data2:
	case DW_FORM_data4:
	case DW_FORM_data8:
	case DW_FORM_udata:
		if (dwarf_formudata(size_attr, &size, &error) == DW_DLV_OK) {
			strstack_pushf(ss, " size %lu", size);
			break;
		}
		/*FALLTHROUGH*/
	default:
		strstack_pushf(ss, " cannot interpret size attribute");
		return -1;
	}
	return size;
}

static char *
dwarf_c_typestring(Dwarf_Debug dbg, Dwarf_Die die, dwarf_walk_ctx_t *ctx)
{
	char *typestr = NULL, *otypestr;
	Dwarf_Die typedie, otypedie;
	dwarf_type_kind_t kind;
	bool addr_match = false;

	for (otypedie = die, typedie = dwarf_follow_type_to_die(dbg, die);
	     typedie != NULL;
	     otypedie = typedie,
	     typedie = dwarf_follow_type_to_die(dbg, typedie)) {
		if (otypedie != die)
			dwarf_dealloc(dbg, otypedie, DW_DLA_DIE);

		ssize_t size = sizeof_type_die(typedie, ctx);

		if (ctx->residue < size)
			addr_match = true;

		char *component =
		    dwarf_c_typestring_component(dbg, typedie, &kind);
		otypestr = typestr;
		assert(component != NULL);
		if (asprintf(&typestr, "%s%s%s",
		    (otypestr == NULL) ? "" : otypestr, (otypestr == NULL) ? "" : " ", component) == -1)
			err(EXIT_FAILURE, "%s: asprintf", __func__);
		if (otypestr != NULL)
			free(otypestr);
	}

	if (otypedie != die)
		dwarf_dealloc(dbg, otypedie, DW_DLA_DIE);

	return addr_match ? typestr : NULL;
}

static char *
dwarf_c_typestring_component(Dwarf_Debug dbg, Dwarf_Die die, dwarf_type_kind_t *kindp)
{
	int res;
	char *name;
	const char *tagname = NULL;
	Dwarf_Error error;
	Dwarf_Half tag;
	char *tagstr;
	char *typestr;

	*kindp = DTK_BASE;

	res = dwarf_tag(die, &tag, &error);
	if (res != DW_DLV_OK) {
		warnx("\n%s: dwarf_offdie: %s", __func__, dwarf_errmsg(error));
		return new_empty_string();
	}
	switch (tag) {
	case DW_TAG_typedef:
	case DW_TAG_base_type:
		res = dwarf_diename(die, &name, &error);
		if (res != DW_DLV_OK) {
			warnx("\n%s.%d: dwarf_diename: %s", __func__, __LINE__,
			    dwarf_errmsg(error));
			return new_empty_string();
		}
		return strdup(name);
	case DW_TAG_const_type:
		return strdup("const");
	case DW_TAG_array_type:
		return strdup("array of");
	case DW_TAG_enumeration_type:
		res = dwarf_diename(die, &name, &error);
		if (res == DW_DLV_NO_ENTRY) {
			return strdup("enum");
		} else if (res != DW_DLV_OK) {
			warnx("\n%s.%d: dwarf_diename: %s", __func__, __LINE__,
			    dwarf_errmsg(error));
			return new_empty_string();
		}
		if (asprintf(&typestr, "enum %s", name) == -1)
			err(EXIT_FAILURE, "%s: asprintf", __func__);
		return typestr;
	case DW_TAG_structure_type:
		res = dwarf_diename(die, &name, &error);
		if (res == DW_DLV_NO_ENTRY) {
			typestr = strdup("struct");
			if (typestr == NULL)
				err(EXIT_FAILURE, "%s: strdup", __func__);
		} else if (res != DW_DLV_OK) {
			warnx("\n%s.%d: dwarf_diename: %s", __func__, __LINE__,
			    dwarf_errmsg(error));
			return new_empty_string();
		} else if (asprintf(&typestr, "struct %s", name) == -1)
			err(EXIT_FAILURE, "%s: asprintf", __func__);
		return typestr;
	case DW_TAG_pointer_type:
		return strdup("pointer to");
	default:
		if ((res = dwarf_get_TAG_name(tag, &tagname)) != DW_DLV_OK) {
			if (asprintf(&tagstr, "<%hd>", tag) == -1)
				err(EXIT_FAILURE, "%s: asprintf", __func__);
		} else if (asprintf(&tagstr, "<%s>", tagname) == -1)
			err(EXIT_FAILURE, "%s: asprintf", __func__);
		return tagstr;
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
	strstack_t *ss = &ctx->symstk;

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

		if (have_insnptr &&
		    inner.lopc <= insnptr && insnptr < inner.hipc) {
			strstack_pushf(&ctx->symstk,
			    " at pc %0" PRIx64
			    " in pc %0" PRIx64 " - %0" PRIx64, insnptr,
			    inner.lopc, inner.hipc);
			insn_match = true;
		} else {
			strstack_pushf(&ctx->symstk,
			    " in pc %0" PRIx64 " - %0" PRIx64,
			    inner.lopc, inner.hipc);
		}

		if (have_dataptr && ld->ld_cents == 1 &&
			 lr0->lr_atom == DW_OP_addr &&
			 lr0->lr_number <= dataptr) {
			strstack_pushf(ss, " at static 0x%0" PRIx64, dataptr);
			ctx->residue = dataptr - lr0->lr_number;
			return true;
		} else if (insn_match && have_dataptr && have_frameptr &&
			   ctx->have_cfa_offset && ld->ld_cents == 1 &&
			   lr0->lr_atom == DW_OP_fbreg &&
			   frameptr - ctx->cfa_offset +
			   (int64_t)lr0->lr_number <= dataptr) {
			strstack_pushf(ss, " on stack at 0x%0" PRIx64
			    " - %" PRId64 " + %" PRId64 " = 0x%0" PRIx64,
			    frameptr, ctx->cfa_offset, lr0->lr_number, dataptr);
			ctx->residue = dataptr -
			    (frameptr - ctx->cfa_offset +
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

			if (have_insnptr &&
			    inner.lopc <= insnptr && insnptr < inner.hipc) {
				strstack_pushf(ss,
				    " at %0" PRIx64, insnptr);
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
		regnum = lr0->lr_number;
		break;
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
		break;
	default:
		return -1;
	}
	return regnum;
}

static void
print_die_data(Dwarf_Debug dbg, Dwarf_Die die, dwarf_walk_ctx_t *ctx)
{
	char *name = NULL, *typename;
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

	res = dwarf_tag(die, &tag, &error);
	if (res != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s: dwarf_tag", __func__);
	}
	res = dwarf_diename(die, &name, &error);
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
		if (ctx->cu_stkdepth != -1)
			strstack_popto(ss, ctx->cu_stkdepth);

		ctx->cu_stkdepth = strstack_pushf(ss, "%s:", name);
	} else if (tag == DW_TAG_subprogram) {
		Dwarf_Attribute loc_attr;
		Dwarf_Locdesc *ld;
		Dwarf_Signed nlocdescs;
		Dwarf_Ptr loc_ptr;
		Dwarf_Unsigned loc_len;

		if (ctx->subprogram_stkdepth != -1)
			strstack_popto(ss, ctx->subprogram_stkdepth);

		ctx->subprogram_stkdepth = strstack_pushf(ss, "%s::", name);

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
			} else if (!have_insnptr) {
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
				   insnptr, &fde, &lopc, &hipc,
				   &error) != DW_DLV_OK) {
				/* FDE unknown for PC */
				ctx->have_cfa_offset = false;
			} else if (dwarf_get_fde_info_for_cfa_reg3(fde,
				 insnptr, &exprtype, &offset_relevant,
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
		depth = strstack_pushf(ss, "%s", name);
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

	if (check_location(dbg, die, ctx)) {

		if ((typename = dwarf_c_typestring(dbg, die, ctx)) != NULL) {
			strstack_pushf(ss, " %s", typename);
			strstack_fprintf(stdout, ss);
			printf("\n");
			free(typename);
		}
	}

	if (depth != -1)
		strstack_popto(ss, depth);
}

static void __dead
usage(const char *progname)
{
	fprintf(stderr, "usage: %s [-v] [object file]\n", progname);
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

	while ((ch = getopt(argc, argv, "d:f:i:v")) != -1) {
		switch (ch) {
		case 'v':
			verbosity++;
			break;
		case 'f':
			if (have_frameptr) {
				errx(EXIT_FAILURE,
				    "%s: too many frame pointers", __func__);
			}
			have_frameptr = true;
			rc = sscanf(optarg, "0x%" SCNx64 "%n", &frameptr, &ofs);
			if (rc != 1 || optarg[ofs] != '\0') {
				errx(EXIT_FAILURE,
				    "%s: malformed frame pointer", __func__);
			}
			break;
		case 'i':
			if (have_insnptr) {
				errx(EXIT_FAILURE,
				    "%s: too many instruction pointers",
				    __func__);
			}
			have_insnptr = true;
			rc = sscanf(optarg, "0x%" SCNx64 "%n", &insnptr, &ofs);
			if (rc != 1 || optarg[ofs] != '\0') {
				errx(EXIT_FAILURE,
				    "%s: malformed instruction pointer",
				    __func__);
			}
			break;
		case 'd':
			if (have_dataptr) {
				errx(EXIT_FAILURE,
				    "%s: too many data pointers", __func__);
			}
			have_dataptr = true;
			rc = sscanf(optarg, "0x%" SCNx64 "%n", &dataptr, &ofs);
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

	if (argc < 1) {
		fd = STDIN_FILENO; /* stdin */
	} else if ((fd = open(argv[0], O_RDONLY)) == -1) {
		err(EXIT_FAILURE, "%s: open(\"%s\")", __func__, argv[0]);
	}

	rc = dwarf_init(fd, DW_DLC_READ, NULL, NULL, &dbg, &error);
	if (rc != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s: dwarf_init: %s", __func__,
		    dwarf_errmsg(error));
	}

	for (die = dwarf_walk_first(dbg, &walk, NULL);
	     die != NULL;
	     die = dwarf_walk_next(&walk)) {
		print_die_data(dbg, die, &walk.ctx);
	        dwarf_dealloc(dbg, die, DW_DLA_DIE);
	}

	rc = dwarf_finish(dbg, &error);
	if (rc != DW_DLV_OK) {
		warnx("%s: dwarf_finish: %s", __func__, dwarf_errmsg(error));
	}
	close(fd);
	return EXIT_SUCCESS;
}
