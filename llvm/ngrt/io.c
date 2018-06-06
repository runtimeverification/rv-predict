#include <assert.h>
#include <err.h>
#include <inttypes.h>	/* for PRIu32 */
#include <stdbool.h>
#include <stdio.h>	/* for printf */
#include <stdlib.h>	/* for malloc(3), NULL */
#include <string.h>	/* for memcpy(3) */
#include <unistd.h>	/* for write(2) */
#include <sys/param.h>	/* for MAX() */
#include <sys/stat.h>	/* for open(2) */
#include <fcntl.h>	/* for open(2) */

#include "io.h"
#include "io_private.h"
#include "nbcompat.h"	/* for __arraycount, offsetof */

static inline void __printflike(1, 2)
dbg_printf(const char *fmt __unused, ...)
{
	return;
}

static ssize_t
iovsum(const rvp_iovec_t *iov, int iovcnt)
{
	int i;
	ssize_t sum = 0;

	for (i = 0; i < iovcnt; i++)
		sum += iov[i].iov_len;
	
	return sum;
}

ssize_t
writeall(int fd, const void *buf, size_t nbytes)
{
	ssize_t nwritten;
	const char *p = buf;

	for (; nbytes > 0; p += nwritten, nbytes -= nwritten) {
		nwritten = write(fd, p, nbytes);
		if (nwritten == -1)
			return -1;
	}
	return nbytes;
}

static void
advance_iov(rvp_iovec_t **iovp, int *iovcntp, ssize_t nbytes)
{
	int i;
	const int iovcnt = *iovcntp;
	rvp_iovec_t *iov;

	for (iov = *iovp, i = 0; nbytes > 0 && i < iovcnt; iov++, i++) {
		if (iov->iov_len > nbytes) {
			iov->iov_len -= nbytes;
			iov->iov_base = (char *)iov->iov_base + nbytes;
			break;
		}
		if (nbytes < iov->iov_len) {
			errx(EXIT_FAILURE, "nbytes %zd <= iov->iov_len %zu",
			    nbytes, iov->iov_len);
		}
		nbytes -= iov->iov_len;
	}
	*iovp = iov;
	*iovcntp = iovcnt - i;
}

ssize_t
xferallv(int fd, ssize_t (*xferv)(int, const rvp_iovec_t *, int),
    const int err_ret,
    const rvp_iovec_t *iov0, rvp_iovec_t *scratch, int iovcnt)
{
	ssize_t nexpected = iovsum(iov0, iovcnt),
		nxferd,
		ntotal;

	nxferd = xferv(fd, iov0, iovcnt);
	if (nxferd == -1 || nxferd == err_ret || nxferd == nexpected) {
		dbg_printf("%s.%d: nxferd -> %zd", __func__, __LINE__, nxferd);
		return nxferd;
	}

	rvp_iovec_t *niov = memcpy(scratch, iov0, sizeof(iov0[0]) * iovcnt),
	    *iov = niov;

	for (ntotal = nxferd; ntotal < nexpected; ntotal += nxferd) {
		advance_iov(&iov, &iovcnt, nxferd);
		nxferd = xferv(fd, iov, iovcnt);
		if (nxferd == -1) {
			dbg_printf("%s.%d: nxferd -> %zd", __func__, __LINE__,
			    nxferd);
			return -1;
		}
		if (nxferd == 0) {
			dbg_printf("%s.%d: nxferd -> %zd", __func__, __LINE__,
			    nxferd);
			break;
		}

		assert(nxferd <= nexpected - ntotal);
	}
	if (ntotal != nexpected) {
		dbg_printf("%s.%d: ntotal %zd != nexpected %zd",
		    __func__, __LINE__, ntotal, nexpected);
	}
	return ntotal;
}
