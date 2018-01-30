/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */
#ifndef _RV_INTERRUPT_LIB_H_
#define _RV_INTERRUPT_LIB_H_

#include <stdbool.h>

void establish(void (*)(int), bool);

#endif /* _RV_INTERRUPT_LIB_H_ */
