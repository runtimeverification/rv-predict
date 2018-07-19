#include "rvpsignal.h"

#define default_sigsim_name rvp_sigsim_name

const char default_sigsim_name[] = "default";

void default_sigsim_raise_all_in_mask(rvp_sigsim_context_t, uint64_t);
void default_sigsim_estdis(int);
void default_sigsim_init(void);

__weak_alias(rvp_sigsim_init, default_sigsim_init)
__weak_alias(rvp_sigsim_raise_all_in_mask, default_sigsim_raise_all_in_mask)
__weak_alias(rvp_sigsim_establish, default_sigsim_estdis)
__weak_alias(rvp_sigsim_disestablish, default_sigsim_estdis)

void
default_sigsim_init(void)
{
	return;
}

void
default_sigsim_estdis(int signo __unused)
{
	return;
}

void
default_sigsim_raise_all_in_mask(
    rvp_sigsim_context_t ctx __unused, uint64_t masked __unused)
{
	return;
}
