/*
 *	SCCS: @(#)mutexes.c	1.4 (98/08/28)
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

#ifndef lint
static char sccsid[] = "@(#)mutexes.c	1.4 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)mutexes.c	1.4 98/08/28 TETware release 3.8
NAME:		mutex definitions and handling functions
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	July 1996
SYNOPSIS:

	mutex_t	tet_top_mtx;
	mutex_t tet_thrtab_mtx;
	mutex_t tet_thrwait_mtx;
	mutex_t tet_alarm_mtx;
	mutex_t tet_sigalrm_mtx;

	void	tet_mtx_init(void);
	void	tet_mtx_destroy(void);
	void	tet_mtx_lock(void);
	void	tet_mtx_unlock(void);

DESCRIPTION:

	Tet_mtx_init() and tet_mtx_destroy() are used by the TCM to
	initialise/destroy/reinitialise all mutexes between test
	purposes and before/after the startup and cleanup functions.

	Tet_mtx_lock() and tet_mtx_unlock() are used to lock/unlock
	all mutexes (e.g. before a fork1()).  The locking is done in
	a specific order.  If any code needs to have more than one
	mutex locked at the same time it must lock them in the
	same order as tet_mtx_lock().

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., February 1998
	Use TETware-specific macros to access threads functions and
	data items.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtthr.h"
#include "apilib.h"

tet_mutex_t tet_top_mtx;	/* used in tet_api_lock() */
tet_mutex_t tet_thrtab_mtx;	/* mutex for thread table */
tet_mutex_t tet_thrwait_mtx;	/* used in tet_cln_threads() */
tet_mutex_t tet_sigalrm_mtx;	/* used in tet_set_alarm() etc. */
tet_mutex_t tet_alarm_mtx;	/* used in tet_set_alarm() etc. */

TET_IMPORT void tet_mtx_init()
{
	/* initialise all mutexes */
	TET_MUTEX_INIT(&tet_top_mtx);
	TET_MUTEX_INIT(&tet_thrtab_mtx);
	TET_MUTEX_INIT(&tet_thrwait_mtx);
	TET_MUTEX_INIT(&tet_sigalrm_mtx);
	TET_MUTEX_INIT(&tet_alarm_mtx);
}

TET_IMPORT void tet_mtx_destroy()
{
	/* destroy all mutexes */

	TET_MUTEX_DESTROY(&tet_top_mtx);
	TET_MUTEX_DESTROY(&tet_thrtab_mtx);
	TET_MUTEX_DESTROY(&tet_thrwait_mtx);
	TET_MUTEX_DESTROY(&tet_sigalrm_mtx);
	TET_MUTEX_DESTROY(&tet_alarm_mtx);
}

void
tet_mtx_lock()
{
	/* lock all mutexes IN THE CORRECT ORDER */

	TET_MUTEX_LOCK(&tet_top_mtx);	/* must be first */

	TET_MUTEX_LOCK(&tet_thrtab_mtx);
	TET_MUTEX_LOCK(&tet_thrwait_mtx);	/* after thrtab_mtx */

	TET_MUTEX_LOCK(&tet_sigalrm_mtx);
	TET_MUTEX_LOCK(&tet_alarm_mtx);	/* after sigalrm_mtx */
}

void
tet_mtx_unlock()
{
	/* unlock all mutexes */

	TET_MUTEX_UNLOCK(&tet_alarm_mtx);
	TET_MUTEX_UNLOCK(&tet_sigalrm_mtx);

	TET_MUTEX_UNLOCK(&tet_thrwait_mtx);
	TET_MUTEX_UNLOCK(&tet_thrtab_mtx);

	TET_MUTEX_UNLOCK(&tet_top_mtx);
}
