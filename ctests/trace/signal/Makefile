#
#
.include "../head.mk"
MKCOVERAGE=no
CTESTS_DIR=$(.CURDIR)/../..
# Don't set _POSIX_C_SOURCE, because this test is not supposed to use
# the SYSV signal(3) implementation.
#CPPFLAGS+="-D_POSIX_C_SOURCE=200809L"
PROG=sigtest
CLEANFILES+=_mk*
PRE_NORMALIZE=$(.CURDIR)/../../dememoize

.include "../tail.mk"
