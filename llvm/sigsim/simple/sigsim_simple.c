#include "nbcompat.h"
#include "rvpsignal.h"

#define simple_sigsim_init rvp_sigsim_init
#define simple_sigsim_raise_all_in_mask rvp_sigsim_raise_all_in_mask
#define simple_sigsim_establish rvp_sigsim_establish
#define simple_sigsim_disestablish rvp_sigsim_disestablish
#define simple_sigsim_name rvp_sigsim_name

const char simple_sigsim_name[] = "simple";

static const char varname[] = "RVP_SIGSIM_CONTEXTS";

static bool before, after;

_Atomic uint64_t established = 0;

void
simple_sigsim_init(void)
{
	const char *v = getenv(varname);

	if (v == NULL || strcmp(v, "both") == 0) {
		before = after = true;
		before = after = true;
	} else if (strcmp(v, "before") == 0) {
		before = true;
		after = false;
	} else if (strcmp(v, "after") == 0) {
		before = false;
		after = true;
	} else if (strcmp(v, "neither") == 0) {
		before = after = false;
	} else
		errx(EXIT_FAILURE, "Unexpected %s setting \"%s\"", varname, v);
}

void
simple_sigsim_establish(int signo)
{
	const uint64_t bit = __BIT(signo_to_bitno(signo));
	rvp_thread_t *t = rvp_thread_for_curthr();

	atomic_fetch_or(&established, bit);

	if ((t->t_intrmask & bit) == 0)
		raise(signo);
}

void
simple_sigsim_disestablish(int signo)
{
	const uint64_t bit = __BIT(signo_to_bitno(signo));
	rvp_thread_t *t = rvp_thread_for_curthr();

	if ((t->t_intrmask & bit) != 0)
		raise(signo);

	atomic_fetch_and(&established, ~bit);
}

void
simple_sigsim_raise_all_in_mask(
    rvp_sigsim_context_t ctx, uint64_t masked)
{
	uint64_t bit;
	int bitno;

	if ((ctx == RVP_SIGSIM_BEFORE_MASKCHG && !before) ||
	    (ctx == RVP_SIGSIM_AFTER_MASKCHG && !after))
		return;

	for (bitno = 0; bitno < 64; bitno++) {
		if ((masked & established & __BIT(bitno)) != 0)
			raise(bitno_to_signo(bitno));
	}
}
