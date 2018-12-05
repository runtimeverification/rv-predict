#include "hb_race_report.h"
#include <string>
#include <sstream>

json hb_race_report(const rvp_pstate_t *ps, const rvp_ubuf_t *decor, rvp_op_t op){
	rvp_addr_t addr;
	switch(op){
		case RVP_OP_LOAD1:
		case RVP_OP_LOAD2:
		case RVP_OP_LOAD4:
		case RVP_OP_ATOMIC_LOAD1:
		case RVP_OP_ATOMIC_LOAD2:
		case RVP_OP_ATOMIC_LOAD4:
		case RVP_OP_STORE1:
		case RVP_OP_STORE2:
		case RVP_OP_STORE4:
		case RVP_OP_ATOMIC_STORE1:
		case RVP_OP_ATOMIC_STORE2:
		case RVP_OP_ATOMIC_STORE4:
			addr = decor->ub_load1_2_4_store1_2_4.addr;
			break;

		case RVP_OP_LOAD8:
		case RVP_OP_LOAD16: // TODO(umang): Ask david what field corresponds to 16 byte wide load/stores
		case RVP_OP_ATOMIC_LOAD8:
		case RVP_OP_ATOMIC_LOAD16: // TODO(umang): Ask david what field corresponds to 16 byte wide load/stores
		case RVP_OP_STORE8:
		case RVP_OP_STORE16:
		case RVP_OP_ATOMIC_STORE8:
		case RVP_OP_ATOMIC_STORE16:
			addr = decor->ub_load8_store8.addr;
			break;

		default:
			break;
	}
	std::stringstream addr_stream;
	addr_stream <<  std::hex << addr;
	std::string j_descr = "Data race on [0x" + addr_stream.str()  + "]";

	json j;
	j["description"] = j_descr;
	j["category"] = {"Undefined", "C"};
	j["error_id"] = "UB-CEER4";

	json j_citation_1;
	j_citation_1["document"] = "C11";
	j_citation_1["section"] = "5.1.2.4";
	j_citation_1["paragraph"] = "25";
	
	json j_citation_2;
	j_citation_2["document"] = "C11";
	j_citation_2["section"] = "J.2";
	j_citation_2["paragraph"] = "1 item 5";

	json j_citation_3;
	j_citation_3["document"] = "CERT-C";
	j_citation_3["section"] = "MSC15-C";

	json j_citation_4;
	j_citation_4["document"] = "MISRA-C";
	j_citation_4["section"] = "8.1";
	j_citation_4["paragraph"] = "3";

	j["citations"] = {j_citation_1, j_citation_2, j_citation_3, j_citation_4};

	j["friendly_cat"] = "Undefined behavior";
	j["long_desc"] = "According to ISO C11, when two threads access the same"
					 "memory location,\none of the accesses is a write, at "
					 "least one of the accesses is not\natomic, and neither"
					 "access happens before the other, then a data race\noccurs. " 
					 "A data race results in undefined behavior.\n";
	return j;
}
