#include <err.h>
#include <errno.h>
#include <inttypes.h>	/* for PRId32 */
#include <limits.h>	/* for _POSIX_RTSIG_MAX */
#include <signal.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>	/* for timer_create(2) */

#include "func.h"
#include "init.h"
#include "intr.h"	/* for prototypes */
#include "ring.h"	/* for rvp_ring_t */
#include "thread.h"	/* for rvp_ring_for_curthr() */
#include "rvpredict_intr.h"
#include "nbcompat.h"

static void __dead
noimpl_abort(const char *fname)
{
	warnx("Interrupt personality %s has no %s implementation.",
	    __rvpredict_intr_personality_name, fname);
	abort();
}

__weak_alias(__rvpredict_intr_personality_splhigh, default_splhigh)
static int __used
default_splhigh(void)
{
	noimpl_abort("splhigh");
}

__weak_alias(__rvpredict_intr_personality_splx, default_splx)
static void __used
default_splx(int level)
{
	noimpl_abort("splx");
}

__weak_alias(__rvpredict_intr_personality_fire_all, default_fire_all)
static void __used
default_fire_all(void)
{
	noimpl_abort("fire_all");
}

__weak_alias(__rvpredict_intr_personality_reinit, default_reinit)
static void __used
default_reinit(void)
{
	noimpl_abort("reinit");
}

__weak_alias(__rvpredict_intr_personality_init, default_init)
static void __used
default_init(void)
{
	noimpl_abort("init");
}

__weak_alias(__rvpredict_intr_personality_disable, default_disable)
static void __used
default_disable(void)
{
	noimpl_abort("disable");
}

__weak_alias(__rvpredict_intr_personality_enable, default_enable)
static void __used
default_enable(void)
{
	noimpl_abort("enable");
}
