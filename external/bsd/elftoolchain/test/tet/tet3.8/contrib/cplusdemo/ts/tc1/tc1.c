// C++ headers first
#include <iostream.h>

// These headers for use of the TET API
#include <stdlib.h>
#include <tet_api.h>

static void startup()
{
// startup routine called before the test purposes

	cout << "This is from the startup routine for tp1\n";
	cout << "Its interesting to note that C++ output\n";
	cout << "using cout does not get journaled.\n";
	cout << "Test 2 will explain why!.\n";
}

void tp1();

extern "C" {
	void (*tet_startup)() = startup, (*tet_cleanup)() = NULL;
	struct tet_testlist tet_testlist[] = { {tp1,1}, {NULL,0} };
}

void tp1()
{
// this is a c++ comment

	cout << "This output comes from test purpose tp1\n";
	tet_infoline("This is the first test case (tc1)");
	tet_result(TET_PASS);
}

