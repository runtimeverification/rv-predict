#ifndef _RV_IO_PRIVATE_H_
#define _RV_IO_PRIVATE_H_

#include <sys/types.h>	/* for size_t and ssize_t */

#include "iovec.h"	/* for rvp_iovec_t */

ssize_t xferallv(int, ssize_t (*)(int, const rvp_iovec_t *, int), const int,
    const rvp_iovec_t *, rvp_iovec_t *, int);

#endif /* _RV_IO_PRIVATE_H_ */
