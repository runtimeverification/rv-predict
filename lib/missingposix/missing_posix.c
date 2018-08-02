#include <errno.h>
#include <limits.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

#include "rvpredict_missing_posix.h"

int
vasprintf(char **bufp, const char *fmt, va_list ap)
{
	int buflen = 1;
	char *buf, *nbuf;

	for (buf = NULL;
	     buflen < INT_MAX && (nbuf = realloc(buf, buflen)) != NULL;
	     buflen = (INT_MAX - buflen > buflen) ? 2 * buflen : INT_MAX) {

		buf = nbuf;
		const int rc = vsnprintf(buf, buflen, fmt, ap);

		if (rc < 0)
			return rc;

		if (rc >= buflen)
			continue;

		*bufp = buf;
		return rc;
	}
	if (buf != NULL)
		free(buf);
	errno = ENOMEM;
	return -1;
}

int
asprintf(char **strp, const char *fmt, ...)
{
	va_list ap;

	va_start(ap, fmt);
	const int rc = vasprintf(strp, fmt, ap);
	va_end(ap);

	return rc;
}
