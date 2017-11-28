#ifndef _RV_INTERRUPT_LIB_H_
#define _RV_INTERRUPT_LIB_H_

#include <stdbool.h>

void establish(void (*)(int), bool);

#endif /* _RV_INTERRUPT_LIB_H_ */
