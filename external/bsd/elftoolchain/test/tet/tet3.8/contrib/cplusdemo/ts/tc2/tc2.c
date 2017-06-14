// C++ headers first
#include <iostream.h>

#include <stdlib.h>
#include <tet_api.h>

void tp1();

extern "C" {
	void (*tet_startup)() = NULL, (*tet_cleanup)() = NULL;
	struct tet_testlist tet_testlist[] = { {tp1,1}, {NULL,0} };
}

void tp1()
{
	tet_infoline("This is the second test case (tc2)");
        cout <<"We have not set TET_OUTPUT_CAPTURE=True ";
        cout <<"so all normal stdin/stdout/stderr\nfiles are available.  ";
        cout <<"\nIf we had set output capture,  the results logged by";
        cout <<" the API would not be in the journal.\n";
        cout <<"But these lines would.\n";
	tet_result(TET_PASS);
}

