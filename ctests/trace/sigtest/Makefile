#
#
.include "../head.mk"
MKCOVERAGE=no
CTESTS_DIR=$(.CURDIR)/../..
# _POSIX_C_SOURCE=200809L forces us to use the SYSV signal(3) implementation
CPPFLAGS+="-D_POSIX_C_SOURCE=200809L"
PROG=sigtest
CLEANFILES+=_mk*
PRE_NORMALIZE=$(.CURDIR)/../../dememoize

.include "../tail.mk"
