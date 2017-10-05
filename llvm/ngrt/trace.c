#include <assert.h>
#include <sys/types.h>	/* for open(2), getpid(2), fstat(2) */
#include <sys/stat.h>	/* for open(2), fstat(2) */
#include <fcntl.h>	/* for open(2) */
#include <libgen.h>	/* for basename(3) */
#include <sys/uio.h>	/* for writev(2) */
#include <stdio.h>	/* for snprintf(3) */
#include <stdlib.h>	/* for getenv(3) */
#include <time.h>	/* for time(3) */
#include <unistd.h>	/* for getlogin(3), fstat(2) */

#include "access.h"
#include "io.h"
#include "nbcompat.h"
#include "notimpl.h"
#include "ring.h"
#include "rvpint.h"
#include "supervise.h"
#include "thread.h"
#include "trace.h"
#include "tracefmt.h"

typedef struct _threadswitch {
	rvp_addr_t deltop;
	uint32_t id;
} __packed __aligned(sizeof(uint32_t)) threadswitch_t;

static const rvp_trace_header_t header = {
	  .th_magic = "RVP_"
	, . th_version = {0, 0, 0, 4}
	, .th_byteorder = '0' | ('1' << 8) | ('2' << 16) | ('3' << 24)
	, .th_pointer_width = sizeof(rvp_addr_t)
	, .th_data_width = sizeof(uint32_t)
};

static char *
rvp_expand_template(const char *template)
{
	const char *addition, *next, *percent = "%";
	char *exename, *username;
	time_t tm;
	char *out;
	char pidbuf[sizeof("18446744073709551616")],
	     secs_since_epoch[sizeof("18446744073709551616")];
	char buf[PATH_MAX] = "";
	const size_t buflen = sizeof(buf);
	int rc;
	size_t nleft;

	if (template == NULL)
		return strdup("./rvpredict.trace");
	if (strchr(template, '%') == NULL)
		return strdup(template);

	if ((username = getlogin()) == NULL)
		err(EXIT_FAILURE, "%s: could not get username", __func__);

	if ((tm = time(NULL)) == (time_t)-1) {
		err(EXIT_FAILURE, "%s: could not read the current time",
		    __func__);
	}

	rc = snprintf(secs_since_epoch, sizeof(secs_since_epoch), "%ju",
	    (intmax_t)tm);
	if (rc < 0 || rc >= sizeof(secs_since_epoch)) {
		err(EXIT_FAILURE,
		    "%s: seconds since epoch exceeds %zu characters",
		    __func__, sizeof(secs_since_epoch) - 1);
	}

	rc = snprintf(pidbuf, sizeof(pidbuf), "%ju", (intmax_t)getpid());
	if (rc < 0 || rc >= sizeof(pidbuf)) {
		err(EXIT_FAILURE, "%s: PID exceeds %zu characters",
		    __func__, sizeof(pidbuf) - 1);
	}

	exename = get_binary_path();

	for (out = buf, next = template;
	     *next != '\0' && (nleft = buflen - (out - buf) - 1) > 0;
	     next++) {
		if (*next != '%') {
			*out++ = *next;
			continue;
		}
		++next;
		switch (*next) {
		case 'n':
			addition = basename(exename);
			break;
		case 'p':
			addition = pidbuf;
			break;
		case 'u':
			addition = username;
			break;
		case 't':
			addition = secs_since_epoch;
			break;
		case '%':
			addition = percent;
			break;
		case '\0':
			err(EXIT_FAILURE,
			    "%s: %% at end of template", __func__);
			break;
		default:
			err(EXIT_FAILURE,
			    "%s: string interpolation %%%c not understood",
			    __func__, *next);
		}
		rc = snprintf(out, nleft, "%s", addition);
		if (rc < 0 || rc >= nleft) {
			err(EXIT_FAILURE,
			    "%s: filled & terminated template did not fit in "
			    "%zu characters", __func__, buflen);
		}
		out += rc;
	}
	*out = '\0';
	return strdup(buf);
}

static int
rvp_trace_open(void)
{
	if (rvp_analysis_fd != -1)
		return rvp_analysis_fd;

	const char *file_tmpl = getenv("RVP_TRACE_FILE");
	const char *fifo_tmpl = getenv("RVP_TRACE_FIFO");
	const char *tmpl = (fifo_tmpl != NULL) ? fifo_tmpl : file_tmpl;
	char *tracefn = rvp_expand_template(tmpl);
	int fd;

	if (tracefn == NULL) {
		err(EXIT_FAILURE,
		    "%s: could not expand trace-file template \"%s\"", __func__,
		        tmpl);
	}

	if (fifo_tmpl != NULL) {
		fd = open(tracefn, O_WRONLY);
	} else {
		fd = open(tracefn, O_WRONLY|O_CREAT|O_TRUNC,
		    S_IRUSR | S_IWUSR /* 0600 may be more readable! */);
	}

	if (fd == -1)
		err(EXIT_FAILURE, "%s: open(\"%s\")", __func__, tracefn);

	if (fifo_tmpl != NULL) {
		struct stat st;

		if (fstat(fd, &st) == -1)
			err(EXIT_FAILURE, "%s: fstat", __func__);

		switch (st.st_mode & S_IFMT) {
		case S_IFCHR:
		case S_IFIFO:
		case S_IFSOCK:
			break;
		default:
			errx(EXIT_FAILURE,
			    "%s: expected a character device, FIFO, "
			    "or socket at \"%s\"", __func__, tracefn);
		}
	}

	free(tracefn);

	return fd;
}

int
rvp_trace_begin(void)
{
	const int fd = rvp_trace_open();

	if (writeall(fd, &header, sizeof(header)) == -1)
		err(EXIT_FAILURE, "%s: writeall", __func__);

	return fd;
}

void
rvp_buf_put_pc_and_op(rvp_buf_t *b, const char **lastpcp, const char *pc,
    rvp_op_t op)
{
	int jmpvec = pc - *lastpcp;
	deltop_t *deltop;

	deltop = rvp_vec_and_op_to_deltop(jmpvec, op);

	*lastpcp = pc;

	if (deltop == NULL) {
		rvp_buf_put_voidptr(b, pc);
		deltop = rvp_vec_and_op_to_deltop(0, op);
		assert(deltop != NULL);
	}
	rvp_buf_put_voidptr(b, deltop);
}

void
rvp_ring_put_begin(rvp_ring_t *r, uint32_t tid, uint64_t generation)
{
	rvp_buf_t b = RVP_BUF_INITIALIZER;
	rvp_buf_put_voidptr(&b, rvp_vec_and_op_to_deltop(0, RVP_OP_BEGIN));
	rvp_buf_put(&b, tid);
	rvp_buf_put_u64(&b, generation);
	rvp_ring_put_buf(r, b);
}

void
rvp_buf_put_cog(rvp_buf_t *b, uint64_t generation)
{
	rvp_buf_put_voidptr(b, rvp_vec_and_op_to_deltop(0, RVP_OP_COG));
	rvp_buf_put_u64(b, generation);
}
