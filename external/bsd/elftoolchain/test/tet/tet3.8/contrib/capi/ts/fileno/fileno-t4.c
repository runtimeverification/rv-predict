/* fileno-t4.c : child program of test purpose 4 for fileno() */

#include <stdlib.h>
#include <stdio.h>
#include <errno.h>

#include <tet_api.h>

static char msg[256];			/* buffer for info lines */

/* ARGSUSED */

int
tet_main(argc, argv)
int argc;
char **argv;
{
    long ret, pos;
    int	fd, err, fail = 0;
    FILE *streams[3];
    static char *strnames[] = { "stdin", "stdout", "stderr" };

    /* initialise the streams[] array */
    streams[0] = stdin;
    streams[1] = stdout;
    streams[2] = stderr;

    /* check file positions of streams are same as set up in parent */

    for (fd = 0; fd < 3; fd++)
    {
	pos = 123 + 45*fd; /* must match lseek() in parent */
	errno = 0;
	if ((ret = ftell(streams[fd])) != pos)
	{
	    err = errno;
	    (void) sprintf(msg, "ftell(%s) returned %ld, expected %ld",
		strnames[fd], ret, pos);
	    tet_infoline(msg);
	    if (err != 0)
	    {
		(void) sprintf(msg, "errno was set to %d", err);
		tet_infoline(msg);
	    }
	    fail = 1;
	}
    }
    
    if (fail == 0)
        tet_result(TET_PASS);
    else
        tet_result(TET_FAIL);

    return 0;
}
