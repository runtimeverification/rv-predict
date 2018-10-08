#ifndef _RV_IO_H_
#define _RV_IO_H_

#include <stdint.h>
#include <sys/uio.h>	/* for readv(2) */
#include <sys/types.h>	/* for size_t and ssize_t */

typedef enum _iostat_sel {
	  IOSTAT_VECTORS = 0
	, IOSTAT_IOS
	, IOSTAT_MAX
} iostat_sel_t;

typedef struct _iostat {
	volatile uint32_t ios_lo[IOSTAT_MAX],
			  ios_hi[IOSTAT_MAX];
} iostat_t;

static inline void
iostat_inc(iostat_t *ios, iostat_sel_t sel)
{
	if (ios == NULL)
		return;

	if (ios->ios_lo[sel] < UINT32_MAX) {
		ios->ios_lo[sel]++;
		return;
	}
	ios->ios_hi[sel]++;
	/* TBD barrier */
	ios->ios_lo[sel] = 0;
	/* TBD barrier */
	ios->ios_hi[sel]++;
}

static inline uint64_t
iostat_get(const iostat_t *ios, iostat_sel_t sel)
{
	uint32_t ohi, nhi, lo;

	do {
		ohi = ios->ios_hi[sel];
		/* TBD barrier */
		lo = ios->ios_lo[sel];
		/* TBD barrier */
		nhi = ios->ios_hi[sel];
	} while ((ohi % 2) != 0 || ohi != nhi);

	return ((uint64_t)nhi << 31) | lo;
}

ssize_t writeall(int, const void *, size_t, iostat_t *);
ssize_t readallv(int, const struct iovec *, struct iovec *, int, iostat_t *);
ssize_t writeallv(int, const struct iovec *, struct iovec *, int, iostat_t *);

#endif /* _RV_IO_H_ */
