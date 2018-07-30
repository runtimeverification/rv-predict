#
# Tail portion of Makefiles in the trace tests directory
#

CC?=rvpc
CPPFLAGS+=-I$(.CURDIR)/../../../include
CPPFLAGS+="-D_POSIX_C_SOURCE=200112L"
WARNS=4
STRIPFLAG=


COPTS+=-O0 -g
LDADD+=-pthread

.PHONY: test_output
 
test.trace: $(PROG)
	@$(.OBJDIR)/$(PROG) > /dev/null

LOCAL_NORMALIZE?=cat

test_output: test.trace
	@rvpdump -t symbol-friendly $(RVP_TRACE_FILE) | rvpsymbolize $(.OBJDIR)/$(PROG) | $(CTESTS_DIR)/normalize-humanized-trace | $(LOCAL_NORMALIZE)

CLEANFILES+=test.trace

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
