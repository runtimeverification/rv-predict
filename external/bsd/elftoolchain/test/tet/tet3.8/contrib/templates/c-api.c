/* A template test case for the C API */

/*---System Headers---*/

#include <stdlib.h>

/*---TET API header---*/

#include <tet_api.h>


/*---TET test purpose declarations---*/

static void tp1(), tp2(), tp3();


/*---TET startup/cleanup function declarations---*/

static void startup(), cleanup();

/*
 * If no startup and cleanup routines, set these 
 * as follows:
 *
 * void (*tet_startup)() = NULL;
 * void (*tet_cleanup)() = NULL;
 *
 */

void (*tet_startup)() = startup;
void (*tet_cleanup)() = cleanup;

/* --- Initialize TCM data structures --- */

/* Array of pointers to test functions, and the
 * invocable component to which they belong
 * Most typically test purposes map to individual
 * invocable components
 */
struct tet_testlist tet_testlist[] = {
    { tp1, 1 },
    { tp2, 2 },
    { tp3, 3 },
    { NULL, 0 }
};

/* --- test case specific code --- */

/* Test Case Wide Declarations */

#define NUM_TEST_CASES 3   /* can be useful */

/* --- test case startup routine ---*/

/* 
 * startup routine called once before any test purposes executed,
 * usually for some general test setup
 *
 */

static void
startup()
{
    int i;
    static char *reason = "Reason string why tests cancelled";
	
    /* check any dependencies, if fail then cancel the tests */
    if (!dependency) {
	for ( i = 1; i <= NUM_TEST_CASES; i++) {
        	/* Prevent tests which need this dependency from executing */
        	tet_delete(i, reason);
	}
    }
    else
	/* some other startup code here */
	;
}

/* --- test case cleanup routine ---*/

static void
cleanup()
{
    /* typically to remove files etc created by start-up */
    /* called once after end of test purposes */
}

/*---TET test purpose functions---*/

/* test purpose one */

static void
tp1()         
{
    int pass = 0;

    tet_infoline("output a message");

    /*
     * test code goes here 
     * test code should set pass to 1 when appropriate
     *
     */
    if (pass == 0 )
        tet_result(TET_FAIL);
    else 
        tet_result(TET_PASS);

    return;
}

static void
tp2() 
{
	/* another test purpose */
}

static void
tp3()
{
	/* another test purpose*/
}

/*--- End of TET test purpose functions---*/
