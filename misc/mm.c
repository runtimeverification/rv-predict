#include <err.h>
#include <fcntl.h>	/* for open(2) */
#include <stdio.h>
#include <stdlib.h>	/* for EXIT_FAILURE, EXIT_SUCCESS */
#include <unistd.h>

#include <sys/mman.h>	/* for mmap(2) */
#include <sys/stat.h>	/* for open(2) */
#include <sys/types.h>	/* for open(2) */

#include "nbcompat.h"

int
main(void)
{
	static char empty[4096 * 2] __aligned(4096);
	char fn[sizeof("/tmp/mm.XXXXXX")] = "/tmp/mm.XXXXXX";
	int fd;

	if ((fd = mkstemp(fn)) == -1)
		err(EXIT_FAILURE, "mkstemp");
	if (unlink(fn) == -1)
		err(EXIT_FAILURE, "unlink");
	if (write(fd, empty, sizeof(empty)) == -1)
		err(EXIT_FAILURE, "write");
	void *p = mmap(empty, sizeof(empty),
	    PROT_READ|PROT_WRITE, MAP_SHARED|MAP_FIXED, fd, 0);
	if (p == MAP_FAILED)
		err(EXIT_FAILURE, "%s.%d: mmap", __func__, __LINE__);
	if (p != empty)
		errx(EXIT_FAILURE, "p != empty");
	empty[0] = 'q';
	void *q = mmap(&empty[4096], 4096, PROT_READ|PROT_WRITE,
	    MAP_SHARED|MAP_FIXED, fd, 0);
	if (q == MAP_FAILED)
		err(EXIT_FAILURE, "%s.%d: mmap", __func__, __LINE__);
	empty[4096] = 'a';
	if (empty[0] == 'a')
		printf("yup\n");
	else
		printf("nope\n");
	return EXIT_SUCCESS;
}
