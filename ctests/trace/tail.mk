#
# Tail portion of Makefiles in the trace tests directory
#

CC?=rvpc
CPPFLAGS+=-I$(CTEST_dir)/../include
WARNS=4
STRIPFLAG=


COPTS+=-O3 -g -O0
LDADD+=-pthread

.PHONY: test_output

test.trace: $(PROG)
	@$(.OBJDIR)/$(PROG) > /dev/null

test_output: test.trace
	@rvpdump -t symbol-friendly $(RVP_TRACE_FILE) | rvpsymbolize $(.OBJDIR)/$(PROG) | $(CTEST_dir)/normalize-humanized-trace

CLEANFILES+=test.trace

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
