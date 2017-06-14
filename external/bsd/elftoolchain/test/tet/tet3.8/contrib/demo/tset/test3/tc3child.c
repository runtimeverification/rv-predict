/* child prog for test3 */
/* simply outputs a tet_infoline and result of TET_PASS */


#include <stdlib.h>
#include <tet_api.h>


int tet_main(int argc, char ** argv)
{
	tet_infoline ( "tp3 child process called ok\n");
	tet_result(TET_PASS);
	return 0;
}

