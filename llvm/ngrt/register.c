#include "register.h"

/* void fn(T *addr, T val) */
uint8_t
__rvpredict_peek1(uint8_t *p)
{
	return *p;
}

uint16_t
__rvpredict_peek2(uint16_t *p)
{
	return *p;
}

uint32_t
__rvpredict_peek4(uint32_t *p)
{
	return *p;
}

uint64_t
__rvpredict_peek8(uint64_t *p)
{
	return *p;
}

rvp_uint128_t
__rvpredict_peek16(rvp_uint128_t *p)
{
	return *p;
}

/* void fn(T *addr, T val) */
void
__rvpredict_poke1(uint8_t *p, uint8_t v)
{
	*p = v;
}

void
__rvpredict_poke2(uint16_t *p, uint16_t v)
{
	*p = v;
}

void
__rvpredict_poke4(uint32_t *p, uint32_t v)
{
	*p = v;
}

void
__rvpredict_poke8(uint64_t *p, uint64_t v)
{
	*p = v;
}

void
__rvpredict_poke16(rvp_uint128_t *p, rvp_uint128_t v)
{
	*p = v;
}
