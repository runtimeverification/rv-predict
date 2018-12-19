#include <sys/types.h>	/* for open(2) */
#include <sys/stat.h>	/* for open(2) */
#include <err.h>
#include <errno.h>
#include <fcntl.h>	/* for open(2) */
#include <inttypes.h>	/* for intmax_t */
#include <limits.h>	/* for SIZE_MAX */
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>	/* for EXIT_* */
#include <string.h>	/* strcmp(3) */
#include <unistd.h>	/* for STDIN_FILENO */

#include "nbcompat.h"
#include "reader.h"
#include "op_to_info.h"
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <vector>
#include <queue>
#include <map>
#include "merge_util.h"

typedef std::pair<uint64_t, uint32_t> pair_gen_tid; 

void merge(char* output_dir, int max_tid){

	std::map<uint32_t, int> tid_to_fd;
	char filepath[1024];
	for(uint32_t tid = 1; tid <= max_tid; tid ++){
		sprintf(filepath, "%s/%d.bin", output_dir, tid);
		int fd = open(filepath, O_RDONLY);
		tid_to_fd[tid] = fd;
	}

	std::map<uint32_t, rvp_pstate_t*> tid_to_ps;
	for(uint32_t tid = 1; tid <= max_tid; tid ++){
		tid_to_ps[tid] = new rvp_pstate_t ();
	}

	std::map<uint32_t, rvp_ubuf_t*> tid_to_ub;
	for(uint32_t tid = 1; tid <= max_tid; tid ++){
		tid_to_ub[tid] = new rvp_ubuf_t ();
	}

	std::priority_queue<pair_gen_tid, std::vector<pair_gen_tid>, std::greater<pair_gen_tid> > pq_gen;

	// Read the first event (begin) of each thread and thereby get the first generation numbers of the threads. 
	for(uint32_t tid = 1; tid <= max_tid; tid ++){
		uint64_t gen_tid = rvp_read_header(tid_to_fd[tid], tid, tid_to_ps[tid], tid_to_ub[tid]);
		pq_gen.push(std::make_pair(gen_tid, tid));	
	}

	while(!pq_gen.empty()){
		pair_gen_tid ptg = pq_gen.top();
		pq_gen.pop();
		uint32_t tid = ptg.second;

		uint64_t new_gen = rvp_trace_dump_until_cog(tid_to_fd[tid], tid, tid_to_ps[tid], tid_to_ub[tid]);
		if(new_gen > 0){
			pq_gen.push(std::make_pair(new_gen, tid));
		}
	}
}