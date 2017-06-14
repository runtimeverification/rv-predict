/*
 *	SCCS: @(#)globals.h	1.1 (98/09/01)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1998 The Open Group
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
 *
 */

/************************************************************************

SCCS:   	@(#)globals.h	1.1 98/09/01 TETware release 3.8
NAME:		globals.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	July 1998

DESCRIPTION:
	declarations of global data items not declared in other header
	files

	data items declared in this file are accessed by library routines

	all TETware programs must provide all of these data items

MODIFICATIONS:

************************************************************************/

TET_IMPORT_DATA(char *, tet_progname);	/* my program name */
TET_IMPORT_DATA(int, tet_mypid);	/* my process ID */
TET_IMPORT_DATA(int, tet_myptype);	/* my process type */
TET_IMPORT_DATA(int, tet_mysysid);	/* my system ID */
TET_IMPORT_ARRAY(char, tet_root, [MAXPATH]);
					/* TET_ROOT from the environment */

/* extern function declarations */
TET_IMPORT_FUNC(void, tet_init_globals, PROTOLIST((
	char *, int, int, 
	void (*) PROTOLIST((int, char *, int, char *, char *)),
	void (*) PROTOLIST((int, char *, int, char *, char *))
)));

