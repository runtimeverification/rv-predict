/*
 *      SCCS:  @(#)tptab_in.h	1.5 (96/09/30) 
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

SCCS:   	@(#)tptab_in.h	1.5 96/09/30 TETware release 3.8
NAME:		tptab_in.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	definition of INET transport-specific server process table data

MODIFICATIONS:

************************************************************************/


/* structure of the INET transport-specific process table data element -
	pointed to by ptab.pt_tdata */
struct tptab {
	SOCKET tp_sd;			/* socket descriptor */
	struct sockaddr_in tp_sin;	/* address of connected peer */
	char *tp_buf;			/* ptr to i/o buffer */
	char *tp_ptr;			/* current buffer position for i/o */
	int tp_len;			/* size of i/o buffer */
	int tp_cnt;			/* i/o count remaining */
};

