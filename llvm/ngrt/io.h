#ifndef _RV_IO_H_
#define _RV_IO_H_

#include <sys/uio.h>	/* for readv(2) */
#include <sys/types.h>	/* for size_t and ssize_t */

ssize_t writeall(int, const void *, size_t);
ssize_t readallv(int, const struct iovec *, struct iovec *, int);
ssize_t writeallv(int, const struct iovec *, struct iovec *, int);

#endif /* _RV_IO_H_ */
