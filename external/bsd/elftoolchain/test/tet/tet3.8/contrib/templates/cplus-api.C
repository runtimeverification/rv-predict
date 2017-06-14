// A template test case for the C++ API 

// ---  C++ System  headers first ---
#include <iostream.h>

// --- System Header for TET C API --- 

// ---TET API header ---

#include <tet_api.h>


//---TET test purpose declarations---

void tp1();

//---TET test startup declarations---
void startup();

//--- TET TCM declarations need to be in an extern "C" code block

extern "C" {

// ---TET startup/cleanup function declarations ---

	void (*tet_startup)() = startup, (*tet_cleanup)() = NULL;

//
// array of pointers to test functions, and the
// invocable component to which they belong
//

	struct tet_testlist tet_testlist[] = { {tp1,1}, {NULL,0} };


} // end of extern "C" code block


//  --- test case startup routine ---

static void startup()
{
// startup routine called once before the test purposes
	cout << "This is from the startup routine for tp1\n";
	cout << "Its interesting to note that C++ output\n";
	cout << "using cout does not get journaled.\n";
	cout << "Test 2 will explain why!.\n";
}


//---TET test purpose functions---

// test purpose one 

void tp1()
{
// this is a c++ comment
	cout << "This output comes from test purpose tp1\n";
	tet_infoline("This is the first test case (tc1)");
	tet_result(TET_PASS);
}

//--- End of TET test purpose functions---


