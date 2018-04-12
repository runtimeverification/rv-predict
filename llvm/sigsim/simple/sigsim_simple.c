#include "nbcompat.h"
#include "rvpsignal.h"

#define simple_sigsim_raise_all_in_mask rvp_sigsim_raise_all_in_mask
#define simple_sigsim_establish rvp_sigsim_establish
#define simple_sigsim_disestablish rvp_sigsim_disestablish
#define simple_sigsim_name rvp_sigsim_name

const char simple_sigsim_name[] = "simple";

_Atomic uint64_t established = 0;

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
simple_sigsim_raise_all_in_mask(uint64_t masked)
{
	uint64_t bit;
	int bitno;

	for (bitno = 0; bitno < 64; bitno++) {
		if ((masked & established & __BIT(bitno)) != 0)
			raise(bitno_to_signo(bitno));
	}
}
