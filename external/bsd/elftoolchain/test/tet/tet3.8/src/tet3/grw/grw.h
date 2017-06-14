/*
 *	SCCS: @(#)grw.h	1.2 (02/11/06)
 *
 * Copyright (c) 2000 The Open Group
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * Motif, OSF/1, UNIX and the "X" device are registered trademarks and
 * IT DialTone and The Open Group are trademarks of The Open Group in
 * the US and other countries.
 *
 * X/Open is a trademark of X/Open Company Limited in the UK and other
 * countries.
 */

/************************************************************************

SCCS:		@(#)grw.h	1.2 02/11/06 TETware release 3.8
NAME:		grw.h
PRODUCT:	TETware Generic Report Writer.
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	19 July 2000

DESCRIPTION:

	Header file for Generic Report Writer.

************************************************************************/


#ifndef GRW_H_INCLUDED
#define GRW_H_INCLUDED


/* Exit status values */
#define GRW_ES_OK		0	/* Successful execution */
#define GRW_ES_ERROR		1	/* Error during execution */
#define GRW_ES_USAGE		2	/* Usage error */

/* Font options for grw_formatter functions */
#define GRW_FONT_MASK		0x0000000fL
#define GRW_FONT_SHIFT		0
#define GRW_FONT_BOLD		(0x1L << GRW_FONT_SHIFT)
#define GRW_FONT_ITALIC		(0x2L << GRW_FONT_SHIFT)
#define GRW_FONT_CODE		(0x4L << GRW_FONT_SHIFT)

/* Border options for grw_formatter functions */
#define GRW_BORDER_MASK		0x000000f0L
#define GRW_BORDER_SHIFT	4
#define GRW_BORDER_NONE		(0x1L << GRW_BORDER_SHIFT)
#define GRW_BORDER_NARROW	(0x2L << GRW_BORDER_SHIFT)

/* Horizontal alignment options for grw_formatter functions */
#define GRW_HALIGN_MASK		0x00000f00L
#define GRW_HALIGN_SHIFT	8
#define GRW_HALIGN_LEFT		(0x1L << GRW_HALIGN_SHIFT)
#define GRW_HALIGN_RIGHT	(0x2L << GRW_HALIGN_SHIFT)
#define GRW_HALIGN_CENTER	(0x3L << GRW_HALIGN_SHIFT)

/* Names for (HTML) class attributes - to support style sheets */
#define GRW_CLASS_SUCCESS	"success"
#define GRW_CLASS_FAILURE	"failure"
#define GRW_CLASS_NEUTRAL	"neutral"
#define GRW_CLASS_NOBORDER	"noborder"
#define GRW_CLASS_H1		"heading1"
#define GRW_CLASS_H2		"heading2"

/* Number of elements in an array. Must be a statically allocated array. */
#define GRW_NELEM(x)		((int)(sizeof(x)/sizeof((x)[0])))


/*
 * Some opaque types.
 */
typedef struct grw_formatter grw_formatter_t;
typedef struct grw_svector grw_svector_t;


/*
 * Function prototypes.
 */

/* formatter.c */
grw_formatter_t * grw_createformatter(
	void (*begindocument)(grw_formatter_t *ftr,
	char *output, char *title),
	void (*enddocument)(grw_formatter_t *ftr),
	void (*printh1)(grw_formatter_t *ftr, char *heading),
	void (*printh2)(grw_formatter_t *ftr, char *heading),
	void (*printh3)(grw_formatter_t *ftr, char *heading),
	void (*print)(grw_formatter_t *ftr, unsigned long options, char *text),
	void (*starttable)(grw_formatter_t *ftr, unsigned long options),
	void (*endtable)(grw_formatter_t *ftr),
	void (*startrow)(grw_formatter_t *ftr),
	void (*endrow)(grw_formatter_t *ftr),
	void (*startcell)(grw_formatter_t *ftr, unsigned long options,
		char *class),
	void (*endcell)(grw_formatter_t *ftr),
	void (*startulist)(grw_formatter_t *ftr),
	void (*endulist)(grw_formatter_t *ftr),
	void (*printlistentry)(grw_formatter_t *ftr, unsigned long options,
	char *text), void (*breakline)(grw_formatter_t *ftr),
	void (*printhline)(grw_formatter_t *ftr),
	void *pdata);
void grw_begindocument(grw_formatter_t *ftr, char *output, char *title);
void grw_enddocument(grw_formatter_t *ftr);
void grw_printh1(grw_formatter_t *ftr, char *fmt, ...);
void grw_printh2(grw_formatter_t *ftr, char *fmt, ...);
void grw_printh3(grw_formatter_t *ftr, char *fmt, ...);
void grw_print(grw_formatter_t *ftr, unsigned long options, char *fmt, ...);
void grw_starttable(grw_formatter_t *ftr, unsigned long options);
void grw_endtable(grw_formatter_t *ftr);
void grw_startrow(grw_formatter_t *ftr);
void grw_endrow(grw_formatter_t *ftr);
void grw_printcell(grw_formatter_t *ftr, unsigned long options, char *class,
	char *fmt, ...);
void grw_startcell(grw_formatter_t *ftr, unsigned long options, char *class);
void grw_endcell(grw_formatter_t *ftr);
void grw_startulist(grw_formatter_t *ftr);
void grw_endulist(grw_formatter_t *ftr);
void grw_printlistentry(grw_formatter_t *ftr, unsigned long options, char *fmt,
	...);
void grw_breakline(grw_formatter_t *ftr);
void grw_printhline(grw_formatter_t *ftr);

/* getopt.c */
int grw_getopt(int argc, char **argv, char *optstring);

/* handler.c */
void grw_beginjournal(char *journal, char *content, struct grw_formatter *ftr,
	char *output);
void grw_handleline(int tag, char *f1a, char *f1b, char *f1c, char *f1d,
	char *f1e, char *f2);
void grw_endjournal(void);

/* html.c */
grw_formatter_t * grw_createhtmlformatter(int usecolor, char *stylesheet);

/* parser.c */
void grw_parsejournal(char *journal, char *content, struct grw_formatter *ftr,
	char *output);

/* svector.c */
grw_svector_t * grw_createsvector(int allocincrement);
int grw_getsize(grw_svector_t *vec);
char ** grw_getentries(grw_svector_t *vec);
char * grw_findentry(grw_svector_t *vec, char *entry);
char * grw_addentry(grw_svector_t *vec, char *entry);
void grw_setentry(grw_svector_t *vec, int idx, char *entry);
void grw_insertentry(grw_svector_t *vec, int idx, char *entry);
void grw_emptysvector(grw_svector_t *vec);
void grw_freesvector(grw_svector_t *vec);

/* text.c */
grw_formatter_t * grw_createtextformatter(int pagewidth);

/* utils.c */
void * grw_malloc(size_t size);
void * grw_realloc(void *ptr, size_t size);
char * grw_strdup(char *s);
void grw_err(char *fmt, ...);
void grw_fatal(char *fmt, ...);
void grw_usage(void);
int grw_getnerrs(void);
char * grw_namebase(char *path);
void grw_setprogname(char *progname);
void grw_setjournal(char *journal);
void grw_setlinenumber(int linenumber);
int grw_atoi(char *string, int *result);


#endif /* GRW_H_INCLUDED */
