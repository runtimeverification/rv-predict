/* tc3 a simple test case, that forks a child */
#include <stdlib.h>
#include <tet_api.h>
#include <errno.h>

extern char** environ;

void tp1(), ch1();

void (*tet_startup)() = NULL, (*tet_cleanup)() = NULL;
struct tet_testlist tet_testlist[] = { {tp1,1}, {NULL,0} };


void tp1()
{
	tet_infoline("This is the third test (tc3)");
	tet_fork(ch1, TET_NULLFP, 30, 0);
	tet_result(TET_PASS);
}


void
ch1()
{
	static char *args[]= {"./tc3child", NULL};

	(void) tet_exec(args[0], args, environ);
	tet_infoline("tet_exec() failed");
	tet_result(TET_UNRESOLVED);
}

