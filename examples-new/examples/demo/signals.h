#include <features.h>
#include <signal.h>	/* sigset(3) */

void signals_changemask(int, int, sigset_t *);
void signals_unmask(int, sigset_t *);
void signals_mask(int, sigset_t *);
void signals_restore(const sigset_t *);
