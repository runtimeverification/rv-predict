/*
 *      SCCS:  @(#)sptab.h	1.1 (97/06/02) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1997 X/Open Company Limited
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

SCCS:   	@(#)sptab.h	1.1 97/06/02 TETware release 3.8
NAME:		sptab.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1997

DESCRIPTION:
	xresd server-specific process table data description

MODIFICATIONS:

************************************************************************/

/* xresd server-specific process table data - pointed to by ptab.pt_sdata */
struct sptab {
	long sp_xrid;		/* XRID associated with this TCM */
};

