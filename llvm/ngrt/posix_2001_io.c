#include <sys/uio.h>	/* for struct iovec */

#include "io_private.h"	/* for xferallv */

ssize_t
readallv(int fd, const rvp_iovec_t *iov0, rvp_iovec_t *scratch, int iovcnt)
{
	return xferallv(fd, readv, 0, iov0, scratch, iovcnt);
}

ssize_t
writeallv(int fd, const rvp_iovec_t *iov0, rvp_iovec_t *scratch, int iovcnt)
{
	return xferallv(fd, writev, -1, iov0, scratch, iovcnt);
}

