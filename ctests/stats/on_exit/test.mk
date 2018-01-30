.PHONY : test_output
test_output:
	@set -e; \
	RVP_TRACE_FILE=/dev/null RVP_TRACE_ONLY=yes RVP_INFO_ATEXIT=yes $(.OBJDIR)/on_exit 2>&1 | sort
#	${MAKE} ${MAKEFLAGS} distclean > /dev/null

.include <mkc.minitest.mk>
