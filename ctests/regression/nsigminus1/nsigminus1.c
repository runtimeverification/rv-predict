#include <err.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>

/* Make sure that we can fetch the state of every signal using
 * `sigaction` without crashing.  We used to (in version 1.9?)
 * crash on signal 32.
 *
 * Maya Rashish reported the bug and supplied the first draft
 * of this test case.
 */
int
main(void)
{
	/* XXX: AFAICT, no standard defines NSIG.  We should iterate over
	 * the signals in a different way.
	 */
        for (int sig = 1; sig < NSIG; sig++) {
                struct sigaction context;
                if (sigaction(sig, NULL, &context) == -1)
			printf("sig %d -> %s\n", sig, strerror(errno));
		else
			printf("sig %d\n", sig);
        }
        return 0;
}
