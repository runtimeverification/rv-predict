#
# Tail portion of Makefiles in the trace tests directory
#

PREDICT_CC?=rvpc
CC?=$(PREDICT_CC)
CPPFLAGS+=-I$(.CURDIR)/../../../include
# _POSIX_C_SOURCE=200809L is needed for SA_RESETHAND on Linux
CPPFLAGS+="-D_POSIX_C_SOURCE=200809L"

WARNS=4
STRIPFLAG=


COPTS+=-O0 -g
.if $(OS:Uunknown) != QNX
LDADD+=-pthread
.endif

.PHONY: test_output
 
test.trace: $(PROG)
	@$(.OBJDIR)/$(PROG) > /dev/null || { rm -f $(RVP_TRACE_FILE) && echo $(PROG) exited with an error 1>&2 && false ; }

PRE_NORMALIZE?=cat
POST_NORMALIZE?=cat

test_output: test.trace
	@rvpdump -t symbol-friendly $(RVP_TRACE_FILE) | rvpsymbolize $(.OBJDIR)/$(PROG) | $(PRE_NORMALIZE) | $(CTESTS_DIR)/normalize-humanized-trace | $(POST_NORMALIZE)

CLEANFILES+=test.trace

.include <mkc.prog.mk>
.include <mkc.minitest.mk>
