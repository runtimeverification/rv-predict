/*
 *      SCCS:  @(#)tptab_xt.h	1.3 (96/08/15) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1993 X/Open Company Limited
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

SCCS:   	@(#)tptab_xt.h	1.3 96/08/15 TETware release 3.8
NAME:		tptab_in.h
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, Unisoft Ltd.
DATE CREATED:	April 1993

DESCRIPTION:
	definition of XTI transport-specific server process table data

MODIFICATIONS:

************************************************************************/


/* structure of the XTI transport process table data element -
	pointed to by ptab.pt_tdata */

struct tptab {
	int tp_fd;			/* endpoint file descriptor	   */
	struct netbuf tp_call; 		/* address of connected peer */
	char *tp_buf;			/* ptr to i/o buffer */
	char *tp_ptr;			/* current buffer position for i/o */
	int tp_len;			/* size of i/o buffer */
	int tp_cnt;			/* i/o count remaining */
};
