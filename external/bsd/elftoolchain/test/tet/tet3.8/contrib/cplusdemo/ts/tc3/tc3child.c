
// child prog for test3
// the tet_main routine must have `C' linking

#include <iostream.h>

#include <stdlib.h>
#include <tet_api.h>

#if __cplusplus
extern "C" {
#endif

int tet_main(int argc, char ** argv)
{
	cout << "tp3 child process called ok\n";
	tet_result(TET_PASS);
	return 0;
}

#if __cplusplus
}
#endif
