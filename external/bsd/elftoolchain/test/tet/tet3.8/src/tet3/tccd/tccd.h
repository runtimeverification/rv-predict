/*
 *      SCCS:  @(#)tccd.h	1.11 (03/03/26) 
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

SCCS:   	@(#)tccd.h	1.11 03/03/26 TETware release 3.8
NAME:		tccd.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	declarations of tccd-specific functions not declared in other
	header files

MODIFICATIONS:
		Denis McConalogue, UniSoft Limited, August 1993
		added prototypes for op_rcopy()

************************************************************************/

/* extern function declarations */
extern void config_cleanup PROTOLIST((void));
extern void etdead PROTOLIST((struct ptab *));
extern void logent PROTOLIST((char *, char *));
extern void logerror PROTOLIST((int, char *, int, char *, char *));
extern void loginit PROTOLIST((void));
extern void op_access PROTOLIST((struct ptab *));
extern void op_cfname PROTOLIST((struct ptab *));
extern void op_chdir PROTOLIST((struct ptab *));
extern void op_config PROTOLIST((struct ptab *));
extern void op_exec PROTOLIST((struct ptab *));
extern void op_ftime PROTOLIST((struct ptab *));
extern void op_kill PROTOLIST((struct ptab *));
extern void op_lockfile PROTOLIST((struct ptab *));
extern void op_mkalldirs PROTOLIST((struct ptab *));
extern void op_mkdir PROTOLIST((struct ptab *));
extern void op_mksdir PROTOLIST((struct ptab *));
extern void op_mktmpdir PROTOLIST((struct ptab *));
extern void op_putenv PROTOLIST((struct ptab *));
extern void op_rcopy PROTOLIST((struct ptab *));
extern void op_rcvconf PROTOLIST((struct ptab *));
extern void op_rmalldirs PROTOLIST((struct ptab *));
extern void op_rmdir PROTOLIST((struct ptab *));
extern void op_rxfile PROTOLIST((struct ptab *));
extern void op_setconf PROTOLIST((struct ptab *));
extern void op_sharelock PROTOLIST((struct ptab *));
extern void op_sndconf PROTOLIST((struct ptab *));
extern void op_time PROTOLIST((struct ptab *));
extern void op_tsfiles PROTOLIST((struct ptab *));
extern void op_tsinfo PROTOLIST((struct ptab *));
extern void op_unlink PROTOLIST((struct ptab *));
extern void op_utime PROTOLIST((struct ptab *));
extern void op_wait PROTOLIST((struct ptab *));
extern int ss_tsargproc PROTOLIST((char *, char *));
extern void ss_tsinitb4fork PROTOLIST((void));
extern int ss_tslogon PROTOLIST((void));
extern int tetrootset PROTOLIST((char *));
extern void ts_forkdaemon PROTOLIST((void));
extern void ts_logstart PROTOLIST((void));

