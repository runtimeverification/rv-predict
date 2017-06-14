/*
 *	SCCS: @(#)tet_jrnl.h	1.6 (97/12/23)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1996 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

/*
 * Copyright 1990 Open Software Foundation (OSF)
 * Copyright 1990 Unix International (UI)
 * Copyright 1990 X/Open Company Limited (X/Open)
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of OSF, UI or X/Open not be used in 
 * advertising or publicity pertaining to distribution of the software 
 * without specific, written prior permission.  OSF, UI and X/Open make 
 * no representations about the suitability of this software for any purpose.  
 * It is provided "as is" without express or implied warranty.
 *
 * OSF, UI and X/Open DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, 
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO 
 * EVENT SHALL OSF, UI or X/Open BE LIABLE FOR ANY SPECIAL, INDIRECT OR 
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF 
 * USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR 
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
 * PERFORMANCE OF THIS SOFTWARE.
 *
 */

/************************************************************************

SCCS:          @(#)tet_jrnl.h	1.6 97/12/23 TETware release 3.8
NAME:          tet_jrnl.h
PRODUCT:       TETware
AUTHOR:        OSF Validation & SQA
DATE CREATED:  14 May 1991
CONTENTS:

MODIFICATIONS:

	"TET Rework"
	David G. Sawyer, UniSoft Ltd,  July 1991.

	DTET development - this file is derived from TET Release 1.10
	David G. Sawyer
	John-Paul Leyland
	UniSoft Ltd, June 1992

	Andrew Dingwall, UniSoft Ltd., August 1996
	added defines for TETware

	Andrew Dingwall, UniSoft Ltd., March 1997
	added definition of TET_JNL_UNAME

	Andrew Dingwall, UniSoft Ltd., December 1997
	protect against multiple inclusion

************************************************************************/

#ifndef TET_JRNL_H_INCLUDED
#define TET_JRNL_H_INCLUDED

/* max length of a journal line */
#define TET_JNL_LEN		512


/*
**	journal line identifiers
*/

/* TCC startup message */
#define TET_JNL_TCC_START	0

/* Uname line */
#define TET_JNL_UNAME		5

/* EXEC test case start */
#define TET_JNL_INVOKE_TC	10

/* TCM startup message */
#define TET_JNL_TCM_START	15

/* configuration report start */
#define TET_JNL_CFG_START	20

/* configuration variable assignment */
#define TET_JNL_CFG_VALUE	30

/* configuration report end */
#define TET_JNL_CFG_END		40

/* TCC error message */
#define TET_JNL_TC_MESSAGE	50

/* scenario information line */
#define TET_JNL_SCEN_OUT	70

/* EXEC test case end */
#define TET_JNL_TC_END		80

/* test case aborted by user interrupt */
#define TET_USER_ABORT		90

/* captured output from a test case */
#define TET_JNL_CAPTURED_OUTPUT	100

/* BUILD start and end */
#define TET_JNL_BUILD_START	110
#define TET_JNL_BUILD_END	130

/* test purpose start and end */
#define TET_JNL_TP_START	200
#define TET_JNL_TP_RESULT	220

/* CLEAN start and end */
#define TET_JNL_CLEAN_START	300
#define TET_JNL_CLEAN_OUTPUT	310
#define TET_JNL_CLEAN_END	320

/* invocable component start and end */
#define TET_JNL_IC_START	400
#define TET_JNL_IC_END		410

/* TCM/API error message */
#define TET_JNL_TCM_INFO	510

/* test case information (from tet_infoline() et al) */
#define TET_JNL_TC_INFO		520

/* PARALLEL start and end */
#define TET_JNL_PRL_START	600
#define TET_JNL_PRL_END		620

/* SEQUENTIAL start and end */
#define TET_JNL_SEQ_START	630
#define TET_JNL_SEQ_END		640
/* reserved for future use	650 */
/* reserved for future use	660 */

/* VARIABLE start and end */
#define TET_JNL_VAR_START	670
#define TET_JNL_VAR_END		680

/* REPEAT start and end */
#define TET_JNL_RPT_START	700
#define TET_JNL_RPT_END		720

/* TIMED_LOOP start and end */
#define TET_JNL_TLOOP_START	730
#define TET_JNL_TLOOP_END	740

/* RANDOM start and end */
#define TET_JNL_RND_START	750
#define TET_JNL_RND_END		760

/* REMOTE start and end */
#define TET_JNL_RMT_START	800
#define TET_JNL_RMT_END		820

/* DISTRIBUTED start and end */
#define TET_JNL_DIST_START	830
#define TET_JNL_DIST_END	840

/* TCC end */
#define TET_JNL_TCC_END		900


/* END status - may be used in field 2.2 of a BUILD, EXEC and CLEAN end line */
#define TET_ESTAT_EXEC_FAILED	-1	/* exec failed */
#define TET_ESTAT_TIMEOUT	-2	/* test case or tool timed out */
#define TET_ESTAT_LOCK		-3	/* could not get required locks */
#define TET_ESTAT_ERROR		-4	/* general error code (usually build
					   or clean tool not defined) */
#endif /* TET_JRNL_H_INCLUDED */

