.PHONY : test_output
test_output:
	@set -e; \
	cd $(.CURDIR); \
	rvpa --no-symbol /dev/null 2> /dev/null
#	${MAKE} ${MAKEFLAGS} distclean > /dev/null

.include <mkc.minitest.mk>
