#ifndef _RVP_ENDIAN_H_
#define _RVP_ENDIAN_H_

/* for htobe32() et cetera: */
#if defined(__NetBSD__)
#include <sys/endian.h>	
#elif defined(__QNX__)
#include <net/netbyte.h>
#else
#include <endian.h>
#endif

#endif /* _RVP_ENDIAN_H_ */
