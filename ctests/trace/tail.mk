#
# Tail portion of Makefiles in the trace tests directory
#

CC?=rvpc
CPPFLAGS+=-I$(CTEST_dir)/../include
WARNS=4
STRIPFLAG=


COPTS+=-O0 -g
LDADD+=-pthread

.PHONY: test_output

test.trace: $(PROG)
	@$(.OBJDIR)/$(PROG) > /dev/null

LOCAL_NORMALIZE?=cat

test_output: test.trace
	@rvpdump -t symbol-friendly $(RVP_TRACE_FILE) | rvpsymbolize $(.OBJDIR)/$(PROG) | $(CTEST_dir)/normalize-humanized-trace | $(LOCAL_NORMALIZE)

CLEANFILES+=test.trace

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
