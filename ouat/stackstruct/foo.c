#include <libelf.h>
#include <libdwarf.h>
#include <stdio.h>
#include <stdlib.h>

#include "foo.h"

void
foo(void)
{
	struct {
		int b;
		int a;
	} s = {0, 0};
#ifdef use_q
	int q;
#endif

/*
	void *retaddr = __builtin_return_address(0);
*/
	void *frmaddr = __builtin_frame_address(0);

	printf("s.a, s.b = %d, %d\n", s.a, s.b);
	printf("&s - __builtin_frame_address() = %td\n",
	    (char *)&s - (char *)frmaddr);
	printf("&s = %p\n", &s);
	printf("frame address = %p\n", frmaddr);
#ifdef use_q
	printf("&q - __builtin_frame_address() = %td\n",
	    (char *)&q - (char *)frmaddr);
#endif
}


