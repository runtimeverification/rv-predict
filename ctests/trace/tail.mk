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
	echo "We who are about to ? :test.trace" 1>&2
	@$(.OBJDIR)/$(PROG) > /dev/null
	echo "test.trace xit" 1>&2

test_output: test.trace
	echo "We who are about to ? :test_output" 1>&2
	@rvpdump -t symbol-friendly $(RVP_TRACE_FILE) | rvpsymbolize $(.OBJDIR)/$(PROG) | $(CTEST_dir)/normalize-humanized-trace
	echo "test_output xit" 1>&2

CLEANFILES+=test.trace

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
