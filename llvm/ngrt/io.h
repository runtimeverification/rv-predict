#ifndef _RV_IO_H_
#define _RV_IO_H_

#include <sys/types.h>	/* for size_t and ssize_t */

#include "iovec.h"	/* for rvp_iovec_t */

ssize_t writeall(int, const void *, size_t);
ssize_t readallv(int, const rvp_iovec_t *, rvp_iovec_t *, int);
ssize_t writeallv(int, const rvp_iovec_t *, rvp_iovec_t *, int);

#endif /* _RV_IO_H_ */
