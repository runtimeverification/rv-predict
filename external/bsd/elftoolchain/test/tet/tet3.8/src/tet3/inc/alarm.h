/*
 *	SCCS: @(#)alarm.h	1.5 (99/09/02)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1996 X/Open Company Limited
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

SCCS:   	@(#)alarm.h	1.5 99/09/02 TETware release 3.8
NAME:		alarm.h
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	Sept 1996 (extracted from dtthr.h)

DESCRIPTION:
	thread-safe alarm functions

	requires prior inclusion of <signal.h> and "dtthr.h"

	note that alarms are not implemented on WIN32 platforms

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., February 1998
	Use TETware-specific macros to access threads functions and
	data items.

************************************************************************/

#ifndef _WIN32		/* -WIN32-CUT-LINE- */

/* structure for tet_set_alarm() and tet_clr_alarm() */

struct alrmaction {
	unsigned int waittime;
	struct sigaction sa;
	sigset_t mask;
#ifdef TET_THREADS
	tet_thread_t join_tid;
	tet_cond_t *cvp;
#endif
};

/* per-thread alarm flag */

#ifdef TET_THREADS
   extern int *tet_thr_alarm_flag();
#  define tet_alarm_flag	(*tet_thr_alarm_flag())
#else
   extern int tet_alarm_flag;
#endif

/* extern function declarations */

extern void tet_catch_alarm PROTOLIST((int));
extern int tet_set_alarm PROTOLIST((struct alrmaction *, struct alrmaction *));
extern int tet_clr_alarm PROTOLIST((struct alrmaction *));

#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */

