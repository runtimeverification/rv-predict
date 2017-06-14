
/*
 * Copyright(c) 1996 X/Open Company Ltd.
 *
 * Permissions to use, copy, modify and distribute this software are
 * governed by the terms and conditions set forth in the file COPYRIGHT,
 * located with this software.
 */

/************************************************************************

SCCS:          %W%
PRODUCT:   	TETware 
NAME:		misc.h

PURPOSE:

	Miscellaneous support routines

HISTORY:
	Andrew Josey, X/Open Company Ltd. 4/23/96 Created .


***********************************************************************/

extern int    tetw_opterr , tetw_optind ;
extern char   *tetw_optarg;
extern int optget(int argc, char *const argv[], const char *opts);
