/*
 *      SCCS:  @(#)syncd.h	1.7 (98/09/01) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Limited
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

SCCS:   	@(#)syncd.h	1.7 98/09/01 TETware release 3.8
NAME:		syncd.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	declarations of syncd-specific functions not declared in other
	header files

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., January 1994
	enhancements for FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., October 1994
	added support for sync message data


************************************************************************/

/* extern function declarations */
extern void op_async PROTOLIST((struct ptab *));
extern void op_snget PROTOLIST((struct ptab *));
extern void op_snrm PROTOLIST((struct ptab *));
extern void op_snsys PROTOLIST((struct ptab *));
extern void op_usync PROTOLIST((struct ptab *));
extern void stdead PROTOLIST((struct ptab *));
extern void stloop PROTOLIST((void));
extern int ss_tsargproc PROTOLIST((char *, char *));
extern void ss_tsinitb4fork PROTOLIST((void));

#ifndef NOTRACE
extern char *stflags PROTOLIST((int));
extern char *smflags PROTOLIST((int));
#endif /* !NOTRACE */

