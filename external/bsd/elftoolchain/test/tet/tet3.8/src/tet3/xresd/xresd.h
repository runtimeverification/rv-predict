/*
 *      SCCS:  @(#)xresd.h	1.8 (98/09/01) 
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

SCCS:   	@(#)xresd.h	1.8 98/09/01 TETware release 3.8
NAME:		xresd.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	declarations of extern xresd-specific functions not declared in
	other header files

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, August 1993
	added prototype for op_xrclose() function

	Andrew Dingwall, UniSoft Ltd., January 1994
	enhancements for FIFO transport interface

************************************************************************/

extern void op_cfname PROTOLIST((struct ptab *));
extern void op_rcfname PROTOLIST((struct ptab *));
extern void op_codesf PROTOLIST((struct ptab *));
extern void op_tfclose PROTOLIST((struct ptab *));
extern void op_tfopen PROTOLIST((struct ptab *));
extern void op_tfwrite PROTOLIST((struct ptab *));
extern void op_icend PROTOLIST((struct ptab *));
extern void op_icstart PROTOLIST((struct ptab *));
extern void op_result PROTOLIST((struct ptab *));
extern void op_tpend PROTOLIST((struct ptab *));
extern void op_tpstart PROTOLIST((struct ptab *));
extern void op_xres PROTOLIST((struct ptab *));
extern void op_xropen PROTOLIST((struct ptab *));
extern void op_xrclose PROTOLIST((struct ptab *));
extern void op_xrsend PROTOLIST((struct ptab *));
extern void op_xrsys PROTOLIST((struct ptab *));
extern int ss_tsargproc PROTOLIST((char *, char *));
extern void ss_tsinitb4fork PROTOLIST((void));
extern void tfdead PROTOLIST((struct ptab *));
extern void xtdead PROTOLIST((struct ptab *));
extern void xtloop PROTOLIST((void));

