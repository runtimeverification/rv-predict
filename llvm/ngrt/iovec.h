#ifndef _RVP_IOVEC_H_
#define _RVP_IOVEC_H_

#ifdef STANDALONE
typedef struct _rvp_iovec {
       void  *iov_base;    /* Starting address */
       size_t iov_len;     /* Number of bytes to transfer */
} rvp_iovec_t;
#else
#include <sys/uio.h>	/* for struct iovec */
typedef struct iovec rvp_iovec_t;
#endif

#endif /* _RVP_IOVEC_H_ */
