#ifndef _RVP_READER_H_
#define _RVP_READER_H_

typedef enum _rvp_output_type {
	  RVP_OUTPUT_PLAIN_TEXT = 0
	, RVP_OUTPUT_SYMBOL_FRIENDLY
	, RVP_OUTPUT_LEGACY_BINARY
} rvp_output_type_t;

void rvp_trace_dump(rvp_output_type_t, int);

#endif /* _RVP_READER_H_ */
