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

static void
EI(void)
{
	/* Set the global enable flag. */
	tiaic_write(TIAIC_GER, TIAIC_GER_ENABLE);
}

static void
DI(void)
{
	/* Clear the global enable flag. */
	tiaic_write(TIAIC_GER, 0);
}

/* This is an interrupt service routine (ISR) established with
 * LOW priority.  It can fire if interrupts are enabled.
 * However, it cannot fire, even if interrupts are enabled, and
 * we are already inside a HIGH priority interrupt.
 *
 * All ISRs begin with interrupts DISABLED.
 */
void
uart_handler(void)
{
	const char enter_msg[] = "( uart interrupt\n";
	const char exit_msg[] =  "  uart interrupt )\n";

	write(STDERR_FILENO, enter_msg, sizeof(enter_msg) - 1);

	DI();
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
spi_handler(void)
{
	const char enter_msg[] = "( spi interrupt\n";
	const char exit_msg[] =  "  spi interrupt )\n";

	write(STDERR_FILENO, enter_msg, sizeof(enter_msg) - 1);

	DI();
	update(racefree);
	EI();
	update(racy);
	write(STDERR_FILENO, exit_msg, sizeof(exit_msg) - 1);
}

int
main(void)
{
	/* On startup, TIAIC interrupts are globally disabled. */

	/* Establish operating mode. */ 
	tiaic_write(TIAIC_CR,
	    TIAIC_CR_PRHOLDMODE | TIAIC_CR_NESTMODE_INDIVIDUAL);
	/* Enable host interrupts on fast interrupt (FIQ) and
	 * interrupt request (IRQ) lines.
	 */
	tiaic_write(TIAIC_HIER, TIAIC_HIER_FIQ | TIAIC_HIER_IRQ);
	/* Unmask system interrupts UART0 and SIP0. */
	tiaic_write(TIAIC_EISR, __SHIFTIN(TIAIC_UART0_INT, TIAIC_EISR_INDEX));
	tiaic_write(TIAIC_EISR, __SHIFTIN(TIAIC_SPI0_INT, TIAIC_EISR_INDEX));
	/* Establish SPI interrupt with lower channel (higher priority)
	 * than UART interrupt.
	 */
	tiaic_write(TIAIC_CMR(TIAIC_UART0_INT / 4),
		    __SHIFTIN(3, TIAIC_CMR_CHNL_MASK(TIAIC_UART0_INT % 4)));
	tiaic_write(TIAIC_CMR(TIAIC_SPI0_INT / 4),
		    __SHIFTIN(2, TIAIC_CMR_CHNL_MASK(TIAIC_SPI0_INT % 4)));

	/* enable ALL interrupts */
	EI();
	update(racy);

	/* disable ALL interrupts */
	DI();
	update(racefree);
	return EXIT_SUCCESS;
}
