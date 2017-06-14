/*
 *	SCCS: @(#)version.c	1.2 (99/09/02)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1998 The Open Group
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

#ifndef lint
static char sccsid1[] = "@(#)version.c	1.2 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)version.c	1.2 99/09/02 TETware release 3.8
NAME:		version.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	July 1998

DESCRIPTION:
	library version strings

	this file in #include'd in apilib/libvers.c and tcm/ckversion.c

	the file that includes this one should define TET_VERSION,
	TET_VERSION_STRINGS and TET_VERSION_STORAGE_CLASS

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1999
	add string describing the Java API support library

************************************************************************/


/* the version strings themselves */
TET_VERSION_STORAGE_CLASS char *TET_VERSION_STRINGS[] = {

#ifdef TET_JAVA_API
	"Java API support",
#else
#  ifdef _WIN32					/* -START-WIN32-CUT- */
#    ifdef TET_SHLIB
	"Shared",
#    else		
	"Static",
#    endif
#  endif					/* -END-WIN32-CUT- */

#  ifdef TET_THREADS
	"Thread-safe",
#  else
	"Single-threaded",
#  endif
#endif

	TET_VERSION,
	(char *) 0
};


