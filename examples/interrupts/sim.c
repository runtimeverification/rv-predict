#include <intr_exports.h>
#include <signal.h>
#include <stdbool.h>
#include <stdlib.h>
#include <unistd.h>

#include "nbcompat.h"

#define update(__lvalue) do {	\
	(__lvalue) += 1;	\
} while (/*CONSTCOND*/false)

static int racy, racefree;

static void __rv_interrupt_handler(irq0, 0) __attribute__((__used__))
handler0(void)
{
	const char msg[] = "interrupt 0\n";
	int s;

	update(racy);
	s = __rvpredict_splhigh();
	update(racefree);
	write(STDERR_FILENO, msg, sizeof(msg) - 1);
	__rvpredict_splx(s);
}

static void __rv_interrupt_handler(irq1, 1) __attribute__((__used__))
handler1(void)
{
	const char msg[] = "interrupt 1\n";
	int s;

	update(racy);
	update(racefree);
	s = __rvpredict_splhigh();
	write(STDERR_FILENO, msg, sizeof(msg) - 1);
	__rvpredict_splx(s);
}

int
main(void)
{
	int s, t, u;
	__rvpredict_intr_enable();
	update(racy);
	s = __rvpredict_splhigh();
	t = __rvpredict_splhigh();
	u = __rvpredict_splhigh();
	update(racefree);
	__rvpredict_splx(u);
	__rvpredict_splx(t);
	__rvpredict_splx(s);
	update(racy);
	s = __rvpredict_splhigh();
	update(racefree);
	__rvpredict_splx(s);
	update(racy);
	s = __rvpredict_splhigh();
	update(racefree);
	__rvpredict_splx(s);
	update(racy);
	s = __rvpredict_splhigh();
	update(racefree);
	__rvpredict_splx(s);
	update(racy);
	return EXIT_SUCCESS;
}

