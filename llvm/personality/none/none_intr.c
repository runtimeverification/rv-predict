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

#define none_personality_name __rvpredict_intr_personality_name
#define none_init __rvpredict_intr_personality_init
#define none_reinit __rvpredict_intr_personality_reinit

const char none_personality_name[] = "none";

void
none_init(void)
{
}

void
none_reinit(void)
{
}
