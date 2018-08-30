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
#include "nbcompat.h"	/* for __arraycount, offsetof */

static inline void __printflike(1, 2)
dbg_printf(const char *fmt __unused, ...)
{
	return;
}

static ssize_t
iovsum(const struct iovec *iov, int iovcnt)
{
	int i;
	ssize_t sum = 0;

	for (i = 0; i < iovcnt; i++)
		sum += iov[i].iov_len;
	
	return sum;
}

ssize_t
writeall(int fd, const void *buf, size_t nbytes, iostat_t *ios)
{
	ssize_t nwritten;
	const char *p = buf;

	iostat_inc(ios, IOSTAT_VECTORS);
	for (; nbytes > 0; p += nwritten, nbytes -= nwritten) {
		iostat_inc(ios, IOSTAT_IOS);
		nwritten = write(fd, p, nbytes);
		if (nwritten == -1)
			return -1;
	}
	return nbytes;
}

static void
advance_iov(struct iovec **iovp, int *iovcntp, ssize_t nbytes)
{
	int i;
	const int iovcnt = *iovcntp;
	struct iovec *iov;

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

static ssize_t
xferallv(int fd, ssize_t (*xferv)(int, const struct iovec *, int),
    const int err_ret,
    const struct iovec *iov0, struct iovec *scratch, int iovcnt, iostat_t *ios)
{
	ssize_t nexpected = iovsum(iov0, iovcnt),
		nxferd,
		ntotal;

	iostat_inc(ios, IOSTAT_VECTORS);

	iostat_inc(ios, IOSTAT_IOS);
	nxferd = xferv(fd, iov0, iovcnt);
	if (nxferd == -1 || nxferd == err_ret || nxferd == nexpected) {
		dbg_printf("%s.%d: nxferd -> %zd", __func__, __LINE__, nxferd);
		return nxferd;
	}

	struct iovec *niov = memcpy(scratch, iov0, sizeof(iov0[0]) * iovcnt),
	    *iov = niov;

	for (ntotal = nxferd; ntotal < nexpected; ntotal += nxferd) {
		advance_iov(&iov, &iovcnt, nxferd);
		iostat_inc(ios, IOSTAT_IOS);
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

ssize_t
readallv(int fd, const struct iovec *iov0, struct iovec *scratch, int iovcnt,
    iostat_t *ios)
{
	return xferallv(fd, readv, 0, iov0, scratch, iovcnt, ios);
}

ssize_t
writeallv(int fd, const struct iovec *iov0, struct iovec *scratch, int iovcnt,
    iostat_t *ios)
{
	return xferallv(fd, writev, -1, iov0, scratch, iovcnt, ios);
}
