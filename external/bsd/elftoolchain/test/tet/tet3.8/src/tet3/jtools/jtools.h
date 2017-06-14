/*
 * @(#)jtools.h	1.1 (99/09/02)
 *
 * (C) Copyright 1997, 1999 X/Open Company, Ltd.
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

SCCS:		@(#)jtools.h	1.1 99/09/02 TETware release 3.8
NAME:		jtools.h
PRODUCT:	TETware
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	9 July 1999

DESCRIPTION:
	Header file for jtools - has prototypes and defines used by
	the Java Enabled TETware tools.

************************************************************************/

#ifndef JTOOLS_H_INCLUDED
#define JTOOLS_H_INCLUDED

#define MAXPATH		1024

#define OP_SETCLASSPATH	0x01
#define OP_SETLIBPATH	0x02
#define OP_SWAPDIRSEP	0x04

void jt_tool(char *cmd, int ops, int argc, char **argv, char *suffix);
void jt_execvp(char *file, char **argv);
void jt_err(char *progname, char *fmt, ...);
int jt_putenv(char *string);

#endif /* JTOOLS_H_INCLUDED */
