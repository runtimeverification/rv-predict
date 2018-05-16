#include <signal.h>
#include <stdbool.h>
#include <stdlib.h>
#include <unistd.h>

#include "nbcompat.h"

#define update(__lvalue) do {	\
	(__lvalue) ^= 1;	\
} while (/*CONSTCOND*/false)

/* We expect for data races to occur on `racy` because it is
 * not protected by DI()/EI().
 *
 * We expect for NO data races to occur on `racefree`, because
 * it is protected by DI()/EI().
 */
static int racy, racefree;

/* This is an interrupt service routine (ISR) established with
 * LOW priority.  It can fire if interrupts are enabled.
 * However, it cannot fire, even if interrupts are enabled, and
 * we are already inside a HIGH priority interrupt.
 *
 * All ISRs begin with interrupts DISABLED.
 */
void
lopri_handler(void)
{
	const char enter_msg[] = "( low priority interrupt\n";
	const char exit_msg[] = "low priority interrupt )\n";

	write(STDERR_FILENO, enter_msg, sizeof(enter_msg) - 1);

	update(racefree);
	EI();
	update(racy);
	write(STDERR_FILENO, exit_msg, sizeof(exit_msg) - 1);
}

/* This is an interrupt service routine (ISR) established with
 * HIGH priority.  It can fire if interrupts are enabled.
 * It can fire EVEN if we are already inside a HIGH priority interrupt.
 *
 * Remember that all ISRs begin with interrupts DISABLED.
 */
void
hipri_handler(void)
{
	const char enter_msg[] = "( high priority interrupt\n";
	const char exit_msg[] = "high priority interrupt )\n";

	write(STDERR_FILENO, enter_msg, sizeof(enter_msg) - 1);

	update(racefree);
	EI();
	update(racy);
	write(STDERR_FILENO, exit_msg, sizeof(exit_msg) - 1);
}

int
main(void)
{
	/* EI enables ALL interrupts */
	EI();
	update(racy);
	/* DI disables ALL interrupts */
	DI();
	update(racefree);
	return EXIT_SUCCESS;
}
