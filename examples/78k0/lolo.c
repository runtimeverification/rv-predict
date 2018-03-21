#include <signal.h>
#include <stdbool.h>
#include <stdlib.h>
#include <unistd.h>

#include "nbcompat.h"

#define update(__lvalue) do {	\
	(__lvalue) ^= 1;	\
} while (/*CONSTCOND*/false)

static int racy, racefree;

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

int
main(void)
{
	EI();
	DI();
	return EXIT_SUCCESS;
}
