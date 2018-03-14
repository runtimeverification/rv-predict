#include <intr_exports.h>
#include <signal.h>
#include <stdbool.h>
#include <stdlib.h>
#include <unistd.h>

#include "nbcompat.h"

#define	DI()	__rvpredict_intr_disable()
#define	EI()	__rvpredict_intr_enable()

#define update(__lvalue) do {	\
	(__lvalue) ^= 1;	\
} while (/*CONSTCOND*/false)

static int racy, racefree;

static void __rv_interrupt_handler(irq0, 0)
lopri_handler(void)
{
	const char msg[] = "low priority interrupt\n";

	write(STDERR_FILENO, msg, sizeof(msg) - 1);

	update(racefree);
	EI();
	update(racy);
}

static void __rv_interrupt_handler(irq1, 1)
hipri_handler(void)
{
	const char msg[] = "high priority interrupt\n";

	write(STDERR_FILENO, msg, sizeof(msg) - 1);

	update(racefree);
	EI();
	update(racy);
	DI();
	update(racefree);
}

int
main(void)
{
	EI();
	update(racy);
	DI();
	update(racefree);
	EI();
	update(racy);
	DI();
	update(racefree);
	EI();
	update(racy);
	return EXIT_SUCCESS;
}
