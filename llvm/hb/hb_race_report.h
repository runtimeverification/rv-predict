#ifndef _RVP_HB_RACE_REPORT_H_
#define _RVP_HB_RACE_REPORT_H_

#include "reader.h"
#include <nlohmann/json.hpp>
using json = nlohmann::json;

json hb_race_report(const rvp_pstate_t *ps, const rvp_ubuf_t *ub, rvp_op_t op);

#endif /* _RVP_HB_RACE_REPORT_H_ */
