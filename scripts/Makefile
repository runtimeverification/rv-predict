#
# This makefile uses mk-configure macro files
# (https://github.com/cheusov/mk-configure) with BSD make.  On a
# POSIX-compliant platforms like Linux, Mac OS X, or *BSD, I recommend
# installing bmake and mk-configure from pkgsrc.org. bmake is in the
# pkgsrc bootstrap kit. The package for mk-configure is in devel/.
#

MAN=rvpa.1 rvpc.1 rvplicense.1 rvpsymbolize.1 rvpx.1
FILESMODE=0755
FILES=rvpa.sh rvpc.sh rvplicense.sh rvpsymbolize.sh rvpx.sh
FILESNAME_rvpa.sh=rvpa
FILESNAME_rvpc.sh=rvpc
FILESNAME_rvplicense.sh=rvplicense
FILESNAME_rvpsymbolize.sh=rvpsymbolize
FILESNAME_rvpx.sh=rvpx
LINKS=${BINDIR}/rvpc ${BINDIR}/rvpc++
MLINKS=rvpc.1 rvpc++.1

.include <mkc.files.mk>
