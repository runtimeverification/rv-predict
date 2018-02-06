.PHONY : test_output
test_output:
	@set -e; \
	${.OBJDIR}/$(PROG) | od -t x1
#	${MAKE} ${MAKEFLAGS} distclean > /dev/null

.include <mkc.minitest.mk>
