/*
 *	SCCS: @(#)formatter.h	1.2 (02/11/06)
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

SCCS:		@(#)formatter.h	1.2 02/11/06 TETware release 3.8
NAME:		formatter.h
PRODUCT:	TETware Generic Report Writer.
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	19 July 2000

DESCRIPTION:

	Header file for formatter code.

************************************************************************/


#ifndef FORMATTER_H_INCLUDED
#define FORMATTER_H_INCLUDED


/*
 * The formatter structure abstracts the code used to produce different
 * document formats.
 */
struct grw_formatter
{
	/*
	 * Begin a new document.
	 *
	 *	ftr		Formatter.
	 *	output		Path of output file. "-" specifies standard
	 *			output.
	 *	title		Non-visible title of the document,
	 *			e.g. <title> in HTML.
	 */
	void (*begindocument)(grw_formatter_t *ftr, char *output, char *title);


	/*
	 * End the current document.
	 *
	 *	ftr		Formatter.
	 */
	void (*enddocument)(grw_formatter_t *ftr);


	/*
	 * Print a level 1 heading to the current document.
	 *
	 *	ftr		Formatter.
	 *	heading		The heading.
	 */
	void (*printh1)(grw_formatter_t *ftr, char *heading);


	/*
	 * Print a level 2 heading to the current document.
	 *
	 *	ftr		Formatter.
	 *	heading		The heading.
	 */
	void (*printh2)(grw_formatter_t *ftr, char *heading);


	/*
	 * Print a level 3 heading to the current document.
	 *
	 *	ftr		Formatter.
	 *	heading		The heading.
	 */
	void (*printh3)(grw_formatter_t *ftr, char *heading);


	/*
	 * Print some text to the current document.
	 *
	 *	ftr		Formatter.
	 *	options		Options. Only font options apply (GRW_FONT..
	 *			bits). All others are ignored.
	 *	text		Text to be printed.
	 */
	void (*print)(grw_formatter_t *ftr, unsigned long options, char *text);


	/*
	 * Start a table.
	 *
	 *	ftr		Formatter.
	 *	options		Options. Only border (GRW_BORDER_..) options
	 *			apply. All others are ignored.
	 */
	void (*starttable)(grw_formatter_t *ftr, unsigned long options);


	/*
	 * End a table.
	 *
	 *	ftr		Formatter.
	 */
	void (*endtable)(grw_formatter_t *ftr);


	/*
	 * Start a table row.
	 *
	 *	ftr		Formatter.
	 */
	void (*startrow)(grw_formatter_t *ftr);


	/*
	 * End a table row.
	 *
	 *	ftr		Formatter.
	 */
	void (*endrow)(grw_formatter_t *ftr);


	/*
	 * Start a table cell.
	 *
	 *	ftr		Formatter.
	 *	options		Options. Only font (GRW_FONT_..), horizontal
	 *			alignment (GRW_HALIGN_..) and background colour
	 *			(GRW_BGCOLOR_..) options apply. All others are
	 *			ignored.
	 *	class		Class attribute - for use by style sheets.
	 */
	void (*startcell)(grw_formatter_t *ftr, unsigned long options,
		char *class);


	/*
	 * End a table cell.
	 *
	 *	ftr		Formatter.
	 */
	void (*endcell)(grw_formatter_t *ftr);


	/*
	 * Start an unordered list.
	 *
	 *	ftr		Formatter.
	 */
	void (*startulist)(grw_formatter_t *ftr);


	/*
	 * End an unordered list.
	 *
	 *	ftr		Formatter.
	 */
	void (*endulist)(grw_formatter_t *ftr);


	/*
	 * Print a list entry.
	 *
	 *	ftr		Formatter.
	 *	options		Options. Only font options apply (GRW_FONT..
	 *			bits). All others are ignored.
	 *	text		Text of list entry.
	 */
	void (*printlistentry)(grw_formatter_t *ftr, unsigned long options,
		char *text);


	/*
	 * Introduce a line break.
	 *
	 *	ftr		Formatter.
	 */
	void (*breakline)(grw_formatter_t *ftr);


	/*
	 * Print a horizontal line.
	 *
	 *	ftr		Formatter.
	 */
	void (*printhline)(grw_formatter_t *ftr);


	/*
	 * Output file stream.
	 */
	FILE *fp;

	/*
	 * Name of output.
	 */
	char *output;

	/*
	 * State of generated document.
	 */
	unsigned int state;

	/*
	 * Private data.
	 */
	void *pdata;
};


#endif /* FORMATTER_H_INCLUDED */
