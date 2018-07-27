#ifndef _RVP_MISSING_POSIX_H_
#define _RVP_MISSING_POSIX_H_

#include <stdarg.h>	/* for va_list */

int asprintf(char **, const char *, ...);
int vasprintf(char **, const char *, va_list);

#endif /* _RVP_MISSING_POSIX_H_ */
