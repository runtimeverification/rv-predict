/* uname-tc.c : test case for uname() interface */

#include <stdio.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/utsname.h>

#include <tet_api.h>

#undef TET_INSPECT      /* must undefine because TET_ is reserved prefix */
#define TET_INSPECT 33  /* this would normally be in a test suite header */

static void tp1();

/* Initialize TCM data structures */
void (*tet_startup)() = NULL;	/* no start-up function */
void (*tet_cleanup)() = NULL;	/* no clean-up function */
struct tet_testlist tet_testlist[] = {
    { tp1, 1 },
    { NULL, 0 }
};


/* Test Case Wide Declarations */
static char msg[256];			/* buffer for info lines */

static void
tp1()         /* successful uname: return 0 */
{
    int ret, err;
    struct utsname name;

    tet_infoline("UNAME OUTPUT FOR MANUAL CHECK");

    /* The test cannot determine automatically whether the information
       returned by uname() is correct.  It therefore outputs the
       information with an INSPECT result code for checking manually. */

    errno = 0;
    if ((ret=uname(&name)) != 0)
    {
        err = errno;
        (void) sprintf(msg, "uname() returned %d, expected 0", ret);
        tet_infoline(msg);
        if (err != 0)
        {
            (void) sprintf(msg, "errno was set to %d", err);
            tet_infoline(msg);
        }
        tet_result(TET_FAIL);
    }
    else
    {
	(void) sprintf(msg, "System Name:  \"%s\"", name.sysname);
	tet_infoline(msg);
	(void) sprintf(msg, "Node Name:    \"%s\"", name.nodename);
	tet_infoline(msg);
	(void) sprintf(msg, "Release:      \"%s\"", name.release);
	tet_infoline(msg);
	(void) sprintf(msg, "Version:      \"%s\"", name.version);
	tet_infoline(msg);
	(void) sprintf(msg, "Machine Type: \"%s\"", name.machine);
	tet_infoline(msg);

        tet_result(TET_INSPECT);
    }
}
