/* test2 - a very simplistic example */
/* this has two test purposes, startup and cleanup routines*/

#include <stdlib.h>
#include <tet_api.h>

void startup();
void cleanup();

void (*tet_startup)() = startup, (*tet_cleanup)() = cleanup;
void tp1();
void tp2();

struct tet_testlist tet_testlist[] = 
	{ {tp1,1}, 
	  {tp2, 2},
	  {NULL,0} };

void tp1()
{
	tet_infoline("This is the second test (tc2)");
	printf("We have not set TET_OUTPUT_CAPTURE=True ");
	printf("so all normal stdin/stdout/stderr\nfiles are available.  ");
	printf("\nIf we had set output capture,  the results logged by");
	printf(" the API would not be in the journal.\n");
	printf("But these lines would.\n");
	tet_result(TET_PASS);
}

void tp2()
{
	tet_infoline("This is the second test purpose within testcase (tc2)");
	tet_result(TET_PASS);
}

/* startup code */
void startup(){
        tet_infoline("test2: startup");
}

/* cleanup code */
void cleanup(){
        tet_infoline("test2: startup");
}

