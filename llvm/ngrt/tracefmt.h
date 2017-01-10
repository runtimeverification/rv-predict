/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_TRACEFMT_H_
#define _RVP_TRACEFMT_H_

#include "nbcompat.h"

/* RV-Predict trace file header.  Located at byte 0 of a trace file.  The
* trace starts at the first rvp_trace_t-sized boundary after the header,
* and it ends at EOF.
*/
struct _rvp_trace_header {
	char th_magic[4];               // 'R' 'V' 'P' '_'
					//
	uint32_t th_version;            // 0
					//
	uint32_t th_byteorder;          // byte-order indication,
					// see discussion
					//
	uint8_t th_pointer_width;       // width of a pointer, in bytes
					//
	uint8_t th_data_width;          // default data width, in bytes
					//
	uint8_t th_pad1[2];
} __aligned(sizeof(uint32_t)) __packed;

typedef struct _rvp_trace_header rvp_trace_header_t;

#endif /* _RVP_TRACEFMT_H_ */
