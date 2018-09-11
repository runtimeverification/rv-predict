.PHONY : test_output
test_output:
	@set -e; \
	RVP_TRACE_FILE=/dev/null RVP_TRACE_ONLY=yes $(.OBJDIR)/on_signal 2>&1 | sort | $(.CURDIR)/../normalize-numbers
#	${MAKE} ${MAKEFLAGS} distclean > /dev/null

.include <mkc.minitest.mk>
