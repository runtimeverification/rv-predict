#ifndef _OUAT_FRAME_H_
#define _OUAT_FRAME_H_

#include <stdbool.h>

const void *program_counter(void);
void innerframe(const void **, const void **);
bool bracketed(const void *, const void *, const void *);

#endif /* _OUAT_FRAME_H_ */
