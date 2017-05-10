.PHONY : test_output
test_output:
	@set -e; \
	${.OBJDIR}/basic
	${MAKE} ${MAKEFLAGS} distclean > /dev/null

.include <mkc.minitest.mk>
