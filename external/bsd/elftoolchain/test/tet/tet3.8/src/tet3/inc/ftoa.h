/*
 *      SCCS:  @(#)ftoa.h	1.6 (96/11/04) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 *
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

/************************************************************************

SCCS:   	@(#)ftoa.h	1.6 96/11/04 TETware release 3.8
NAME:		ftoa.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1992

DESCRIPTION:
	a header file for use with tet_f2a()

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., August 1996
	added support for re-usable buffer pool

************************************************************************/


/* flag names structure */
struct flags {
	int fl_value;		/* flag value */
	char *fl_name;		/* flag name */
};

#define NFBUF	2		/* no of times tet_f2a() may be called before
				   re-using the buffers */

/* extern function declarations */
extern char *tet_f2a PROTOLIST((int, struct flags *, int));

