.PHONY : test_output
test_output:
	@set -e; \
	${.OBJDIR}/basic | rvpsymbolize ${.OBJDIR}/basic | uniq -u
#	${MAKE} ${MAKEFLAGS} distclean > /dev/null

.include <mkc.minitest.mk>
