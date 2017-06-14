/*
 *	SCCS: @(#)restab.h	1.1 (96/08/15)
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

SCCS:   	@(#)restab.h	1.1 (96/08/15) TETware release 3.8
NAME:		restab.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	a header file for use in conjunction with the restab functions

MODIFICATIONS:

************************************************************************/

/* structure of an entry in the results code table */
struct restab {
	char *rt_name;		/* result name */
	int rt_code;		/* result code */
	int rt_abrt;		/* abort flag */
};

/* the results code table and number of entries in it */
extern struct restab *tet_restab;
extern int tet_nrestab;

