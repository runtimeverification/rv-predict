/*
 *      SCCS:  @(#)sigmap.c	1.6 (96/11/04) 
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

#ifndef lint
static char sccsid[] = "@(#)sigmap.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sigmap.c	1.6 96/11/04 TETware release 3.8
NAME:		sigmap.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	map of local signal numbers to DTET signal numbers

	the signal names in the map were collected from <signal.h> files
	on several machines; however, your mileage may vary

	the common signals (0 -> 15) have the same value on most systems;
	the local and DTET signal numbers are the same for these signals and
	the code in tet_mapsignal() and tet_unmapsignal() is most efficient
	when this is the case

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., January 1994
	only support POSIX.1 signals when _POSIX_SOURCE is defined
	(mainly for the FIFO implementation)
	this is because, on some systems, a definition of the form

		#define xx yy

	is visible when _POSIX_SOURCE is defined, where xx is a non-posix
	signal and yy is not defined

************************************************************************/

#ifndef _POSIX_SOURCE
#define INCLUDE_ALL_SIGS
#endif

#include <signal.h>
#include "dtmac.h"
#include "sigmap.h"


struct sigmap tet_sigmap[] = {

	{ 0,		0 },

#ifdef SIGHUP
	{ SIGHUP,	1 },
#endif

#ifdef SIGINT
	{ SIGINT,	2 },
#endif

#ifdef SIGQUIT
	{ SIGQUIT,	3 },
#endif

#ifdef SIGILL
	{ SIGILL,	4 },
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGTRAP
	{ SIGTRAP,	5 },
#endif
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGIOT
	{ SIGIOT,	6 },
#endif
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGEMT
	{ SIGEMT,	7 },
#endif
#endif

#ifdef SIGFPE
	{ SIGFPE,	8 },
#endif

#ifdef SIGKILL
	{ SIGKILL,	9 },
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGBUS
	{ SIGBUS,	10 },
#endif
#endif

#ifdef SIGSEGV
	{ SIGSEGV,	11 },
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGSYS
	{ SIGSYS,	12 },
#endif
#endif

#ifdef SIGPIPE
	{ SIGPIPE,	13 },
#endif

#ifdef SIGALRM
	{ SIGALRM,	14 },
#endif

#ifdef SIGTERM
	{ SIGTERM,	15 },
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGURG
	{ SIGURG,	16 },
#endif
#endif

#ifdef SIGSTOP
	{ SIGSTOP,	17 },
#endif

#ifdef SIGTSTP
	{ SIGTSTP,	18 },
#endif

#ifdef SIGCONT
	{ SIGCONT,	19 },
#endif

#ifdef SIGCHLD
	{ SIGCHLD,	20 },
#endif

#ifdef SIGTTIN
	{ SIGTTIN,	21 },
#endif

#ifdef SIGTTOU
	{ SIGTTOU,	22 },
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGIO
	{ SIGIO,	23 },
#endif
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGXCPU
	{ SIGXCPU,	24 },
#endif
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGXFSZ
	{ SIGXFSZ,	25 },
#endif
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGVTALRM
	{ SIGVTALRM,	26 },
#endif
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGPROF
	{ SIGPROF,	27 },
#endif
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGWINCH
	{ SIGWINCH,	28 },
#endif
#endif

#ifdef SIGUSR1
	{ SIGUSR1,	30 },
#endif

#ifdef SIGUSR2
	{ SIGUSR2,	31 },
#endif

#ifdef SIGABRT
	{ SIGABRT,	1001 },
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGCLD
	{ SIGCLD,	1002 },
#endif
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGLOST
	{ SIGLOST,	1003 },
#endif
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGPOLL
	{ SIGPOLL,	1004 },
#endif
#endif

#ifdef INCLUDE_ALL_SIGS
#ifdef SIGPWR
	{ SIGPWR,	1005 }
#endif
#endif

};

int tet_Nsigmap = sizeof tet_sigmap / sizeof tet_sigmap[0];

