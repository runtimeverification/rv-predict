/*
 *	SCCS: @(#)dtcc.h	1.2 (96/11/04)
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

/************************************************************************

SCCS:   	@(#)dtcc.h	1.2 96/11/04 TETware release 3.8
NAME:		dtcc.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	dtcc functions not declared in other header files

MODIFICATIONS:

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

/* extern function declarations */
extern int ts_tsinfo2bs PROTOLIST((char *, char *));
extern int ts_stserver PROTOLIST((struct ptab *, char **));
extern int ts_needdist PROTOLIST((void));
extern int ts_tccinit PROTOLIST((void));
extern int ts_tsinfolen PROTOLIST((void));

#endif /* !TET_LITE */	/* -END-LITE-CUT- */

