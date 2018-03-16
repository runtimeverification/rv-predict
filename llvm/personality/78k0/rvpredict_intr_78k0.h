#include <rvpredict_intr.h>

#define renesas_78k0_priority_low 0
#define renesas_78k0_priority_high 1

#define	DI()	__rvpredict_intr_disable()
#define	EI()	__rvpredict_intr_enable()
