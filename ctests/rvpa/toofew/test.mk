.PHONY : test_output
test_output:
	@set -e; \
	rvpa 2>&1 > /dev/null && false || true
#	${MAKE} ${MAKEFLAGS} distclean > /dev/null

.include <mkc.minitest.mk>
