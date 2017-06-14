/*
 *      SCCS:  @(#)server_in.h	1.6 (98/08/28) 
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

SCCS:   	@(#)server_in.h	1.6 98/08/28 TETware release 3.8
NAME:		server_in.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	declarations of server-specific functions that may be called by
	inetlib routines

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

extern void tet_ss_tsaccept PROTOLIST((void));
extern int tet_ss_tsafteraccept PROTOLIST((struct ptab *));
TET_EXPORT_FUNC(int, tet_ss_tsconnect, PROTOLIST((struct ptab *)));

