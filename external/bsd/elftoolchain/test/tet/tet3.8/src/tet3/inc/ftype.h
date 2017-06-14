/*
 *	SCCS: @(#)ftype.h	1.1 (03/03/26)
 *
 *	The Open Group, Reading, England
 *
 * Copyright (c) 2003 The Open Group
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

SCCS:   	@(#)ftype.h	1.1 03/03/26 TETware release 3.8
NAME:		ftype.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, The Open Group
DATE CREATED:	March 2003

DESCRIPTION:
	a header file for use with the filetype functions

MODIFICATIONS:

************************************************************************/


/* structure to describe a file type */
struct tet_ftype {
	char *ft_suffix;		/* file name suffix */
	int ft_ftype;			/* file type (ASCII or binary) */
};

/* file type values */
#define TET_FT_ASCII		1	/* ASCII file type */
#define TET_FT_BINARY		2	/* binary file type */


/* extern function declarations */
extern int tet_addftype PROTOLIST((char *, int));
extern struct tet_ftype *tet_getftbysuffix PROTOLIST((char *));
extern struct tet_ftype *tet_getftent PROTOLIST((void));
extern int tet_getftype PROTOLIST((char *));
extern void tet_setftent PROTOLIST((void));

