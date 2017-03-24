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
#include <fcntl.h>	/* for open() */
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>     /* for exit() */
#include <string.h>     /* for strdup() */
#include <unistd.h>     /* for close() */

#include <libdwarf.h>
#include <dwarf.h>

static void print_die_data(Dwarf_Debug, Dwarf_Die);

typedef bool (*dwarf_walk_predicate_t)(Dwarf_Debug, Dwarf_Die);

typedef struct _dwarf_walk {
	int stack_height;
	Dwarf_Die stack[16];
	Dwarf_Debug dbg;
	dwarf_walk_predicate_t predicate;
} dwarf_walk_t;

typedef enum _dwarf_type_kind {
	  DTK_QUALIFIER = 0
	, DTK_POINTER
	, DTK_BASE
} dwarf_type_kind_t;

static Dwarf_Die dwarf_walk_first(Dwarf_Debug, dwarf_walk_t *,
    dwarf_walk_predicate_t);
static Dwarf_Die dwarf_walk_next(dwarf_walk_t *);
static Dwarf_Die dwarf_walk_next_in_tree(dwarf_walk_t *);
static char *dwarf_c_typestring(Dwarf_Debug, Dwarf_Die);
static char *dwarf_c_typestring_component(Dwarf_Debug, Dwarf_Die,
    dwarf_type_kind_t *);

static Dwarf_Die
dwarf_walk_first(Dwarf_Debug dbg, dwarf_walk_t *walk,
    dwarf_walk_predicate_t predicate)
{
	walk->stack_height = 0;
	walk->dbg = dbg;
	walk->predicate = predicate;

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

#if 0
		printf("version %hd\n", version_stamp);
#endif

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
		walk->stack_height = 1;
	} else 
		last = walk->stack[walk->stack_height - 1];

	res = dwarf_siblingof(walk->dbg, last, &sibling, &error);
	if (res == DW_DLV_OK) {
		walk->stack[walk->stack_height - 1] = sibling;
	} else if (res == DW_DLV_NO_ENTRY) {
		walk->stack[--walk->stack_height] = NULL;
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
		walk->stack[walk->stack_height++] = child;
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

	if (dwarf_formref(attr, &offset, &error) != DW_DLV_OK) {
		warnx("\n%s: dwarf_formref: %s", __func__, dwarf_errmsg(error));
		return NULL;
	}

	if (dwarf_offdie(dbg, offset, &typedie, &error) != DW_DLV_OK) {
		warnx("\n%s: dwarf_offdie: %s", __func__, dwarf_errmsg(error));
		return NULL;
	}

	return typedie;
}

static char *
dwarf_c_typestring(Dwarf_Debug dbg, Dwarf_Die die)
{
	char *typestr = NULL, *otypestr;
	Dwarf_Die typedie, otypedie;
	dwarf_type_kind_t kind;

	for (otypedie = die, typedie = dwarf_follow_type_to_die(dbg, die);
	     typedie != NULL;
	     otypedie = typedie,
	     typedie = dwarf_follow_type_to_die(dbg, typedie)) {
		if (otypedie != die)
			dwarf_dealloc(dbg, otypedie, DW_DLA_DIE);

		char *component =
		    dwarf_c_typestring_component(dbg, typedie, &kind);
		otypestr = typestr;
		assert(component != NULL);
		if (asprintf(&typestr, "%s %s",
		    (otypestr == NULL) ? "" : otypestr, component) == -1)
			err(EXIT_FAILURE, "%s: asprintf", __func__);
		if (otypestr != NULL)
			free(otypestr);
	}

	if (otypedie != die)
		dwarf_dealloc(dbg, otypedie, DW_DLA_DIE);

	return typestr;
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
	case DW_TAG_base_type:
		res = dwarf_diename(die, &name, &error);
		if (res != DW_DLV_OK) {
			warnx("\n%s: dwarf_diename: %s", __func__,
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
			warnx("\n%s: dwarf_diename: %s", __func__,
			    dwarf_errmsg(error));
			return new_empty_string();
		}
		if (asprintf(&typestr, "enum %s", name) == -1)
			err(EXIT_FAILURE, "%s: asprintf", __func__);
		return typestr;
	case DW_TAG_structure_type:
		res = dwarf_diename(die, &name, &error);
		if (res != DW_DLV_OK) {
			warnx("\n%s: dwarf_diename: %s", __func__,
			    dwarf_errmsg(error));
			return new_empty_string();
		}
		if (asprintf(&typestr, "struct %s", name) == -1)
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
print_op(const Dwarf_Loc *lr)
{
	int res;
	const char *opname;
	unsigned opcode = lr->lr_atom;

	res = dwarf_get_OP_name(opcode, &opname);
	if (res == DW_DLV_OK) {
		printf("%s", opname);
	} else {
		printf("<op %d>", opcode);
	}
	switch (opcode) {
	case DW_OP_bregx:
		printf("(%0" PRIu64 ", %0" PRId64 ")",
		    lr->lr_number, lr->lr_number2);
		break;
	case DW_OP_addr:
		printf("(%#0" PRIx64 ")", lr->lr_number);
		break;
	case DW_OP_regx:
	case DW_OP_piece:
		printf("(%0" PRIu64 ")", lr->lr_number);
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
		printf("(%0" PRId64 ")", lr->lr_number);
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

static void
print_location(Dwarf_Debug dbg, Dwarf_Die die)
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

	if (dwarf_attr(die, DW_AT_location, &loc_attr, &error) != DW_DLV_OK)
		return;

	if (dwarf_formexprloc(loc_attr, &loc_len, &loc_ptr,
	                           &error) == DW_DLV_OK &&
	    dwarf_loclist_from_expr(dbg, loc_ptr, loc_len, &ld,
	                            &nlocdescs, &error) == DW_DLV_OK) {
		int j;
		const char *delim = "";

		printf(" location ");

		assert(nlocdescs == 1);

		assert(ld->ld_lopc < ld->ld_hipc);

#if 0
		printf("pc %0" PRIx64 " - %0" PRIx64 ": ",
		    ld->ld_lopc, ld->ld_hipc);
#endif
		for (j = 0; j < ld->ld_cents; j++) {
			Dwarf_Loc *lr = &ld->ld_s[j];
			printf("%s", delim);
			print_op(lr);
			delim = " ";
		}
#if 0
		for (i = 0; i < nlocdescs; i++)
			dwarf_dealloc(dbg, locdesc[i].ld_s, DW_DLA_LOC_BLOCK);
		dwarf_dealloc(dbg, locdesc, DW_DLA_LOCDESC);
#endif
	} else if ((res = dwarf_loclist_n(loc_attr, &locdescp, &nlocdescs,
	                                  &error)) == DW_DLV_OK) {
		int i, j;

		const char *odelim = "";
		printf(" location [");

		for (i = 0; i < nlocdescs; i++) {
			const char *idelim = "";
			ld = locdescp[i];

			if (ld->ld_lopc == ld->ld_hipc)
				continue;

			printf("%spc %0" PRIx64 " - %0" PRIx64 ": ", odelim,
			    ld->ld_lopc, ld->ld_hipc);
			odelim = ", ";
			for (j = 0; j < ld->ld_cents; j++) {
				Dwarf_Loc *lr = &ld->ld_s[j];
				printf("%s", idelim);
				print_op(lr);
				idelim = " ";
			}
		}
		printf("]");
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
			printf(" location form unknown");
		else {
			printf(" location form %s err %s", formname,
			    dwarf_errmsg(error));
		}
	}
}

static void
print_die_data(Dwarf_Debug dbg, Dwarf_Die die)
{
	char *typename;
	char *name = NULL;
	Dwarf_Error error;
	Dwarf_Half tag = 0;
	const char *tagname = NULL;
	int res;

	res = dwarf_tag(die, &tag, &error);
	if (res != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s: dwarf_tag", __func__);
	}
	printf("tag: %d", tag);
	res = dwarf_get_TAG_name(tag, &tagname);
	if (res == DW_DLV_OK) {
		printf(" %s", tagname);
	}
	res = dwarf_diename(die, &name, &error);
	switch (res) {
	case DW_DLV_ERROR:
	default:
		errx(EXIT_FAILURE, "\n%s: dwarf_diename: %s",
		    __func__, dwarf_errmsg(error));
		break;
	case DW_DLV_NO_ENTRY:
		printf("\n");
		return;
	case DW_DLV_OK:
		printf("  %s", name);
		dwarf_dealloc(dbg, name, DW_DLA_STRING);
		break;
	}

	print_location(dbg, die);

	Dwarf_Attribute lopc_attr, hipc_attr;
	Dwarf_Half lopc_form, hipc_form;
	Dwarf_Addr lopc, hipc;
	Dwarf_Unsigned hipcofs;

	if (dwarf_attr(die, DW_AT_high_pc, &hipc_attr, &error) == DW_DLV_OK &&
	    dwarf_attr(die, DW_AT_low_pc, &lopc_attr, &error) == DW_DLV_OK &&
	    dwarf_whatform(lopc_attr, &lopc_form, &error) == DW_DLV_OK &&
	    dwarf_whatform(hipc_attr, &hipc_form, &error) == DW_DLV_OK) {
		if (dwarf_formaddr(lopc_attr, &lopc, &error) !=
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
			hipc = lopc + hipcofs;
			break;
		case DW_FORM_addr:
			if (dwarf_formaddr(hipc_attr, &hipc, &error) !=
			    DW_DLV_OK) {
				errx(EXIT_FAILURE, "\n%s: dwarf_formaddr: %s",
				    __func__, dwarf_errmsg(error));
			}
			break;
		default:
			errx(EXIT_FAILURE, "%s: unknown hipc form", __func__);
		}
		printf(" pc %0" PRIx64 " - %0" PRIx64, lopc, hipc);
	}

	if ((typename = dwarf_c_typestring(dbg, die)) != NULL) {
		printf(" %s\n", typename);
		free(typename);
	} else
		printf("\n");
}

int 
main(int argc, char **argv)
{
	Dwarf_Debug dbg = 0;
	Dwarf_Die die;
	int fd;
	int res;
	Dwarf_Error error;
	dwarf_walk_t walk;

	if (argc < 2) {
		fd = STDIN_FILENO; /* stdin */
	} else if ((fd = open(argv[1], O_RDONLY)) == -1) {
		err(EXIT_FAILURE, "%s: open(\"%s\")", __func__, argv[1]);
	}

	res = dwarf_init(fd, DW_DLC_READ, NULL, NULL, &dbg, &error);
	if (res != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s: dwarf_init: %s", __func__,
		    dwarf_errmsg(error));
	}

	for (die = dwarf_walk_first(dbg, &walk, NULL);
	     die != NULL;
	     die = dwarf_walk_next(&walk)) {
		print_die_data(dbg, die);
	        dwarf_dealloc(dbg, die, DW_DLA_DIE);
	}

	res = dwarf_finish(dbg, &error);
	if (res != DW_DLV_OK) {
		warnx("%s: dwarf_finish: %s", __func__, dwarf_errmsg(error));
	}
	close(fd);
	return EXIT_SUCCESS;
}
