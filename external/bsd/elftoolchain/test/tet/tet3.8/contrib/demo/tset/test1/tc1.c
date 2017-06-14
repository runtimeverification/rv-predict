/* tc1 - a very simple test example */

#include <stdlib.h>
#include <tet_api.h>

/* 
 *  The tet_startup / tet_cleanup functions will be explained
 *  in detail later in the course, as will the tet_testlist
 *  structure 
 */

void (*tet_startup)() = NULL, (*tet_cleanup)() = NULL;
void tp1();

struct tet_testlist tet_testlist[] = { {tp1,1}, {NULL,0} };

void tp1()
{
 	(void) tet_printf("test case: %s, TP number: %d ",
                tet_pname, tet_thistest);
	tet_result(TET_PASS);

/*	tet_result(TET_FAIL);*/
}

