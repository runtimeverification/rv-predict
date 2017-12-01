.PHONY : test_output
test_output:
	@set -e; \
	rvpa --no-symbol /dev/null $(.CURDIR)/rvpredict.trace 2> /dev/null
#	${MAKE} ${MAKEFLAGS} distclean > /dev/null

.include <mkc.minitest.mk>
