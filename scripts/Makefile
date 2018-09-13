#
# This makefile uses mk-configure macro files
# (https://github.com/cheusov/mk-configure) with BSD make.  On a
# POSIX-compliant platforms like Linux, Mac OS X, or *BSD, I recommend
# installing bmake and mk-configure from pkgsrc.org. bmake is in the
# pkgsrc bootstrap kit. The package for mk-configure is in devel/.
#

MAN=rvpa.1 rvpc.1 rvpsymbolize.1
FILESMODE=0755
FILES=qclang-4.0
FILES+=rvpa.sh rvpc.sh rvpsymbolize.sh
FILES+=console-metadata.json csv-metadata.json json-metadata.json
FILESNAME_rvpa.sh=rvpa
FILESNAME_rvpc.sh=rvpc
FILESNAME_rvpsymbolize.sh=rvpsymbolize
FILESMODE_console-metadata.json=0644
FILESMODE_csv-metadata.json=0644
FILESMODE_json-metadata.json=0644
FILESDIR_console-metadata.json=$(DATADIR)/rv-predict-c
FILESDIR_csv-metadata.json=$(DATADIR)/rv-predict-c
FILESDIR_json-metadata.json=$(DATADIR)/rv-predict-c
LINKS=${BINDIR}/rvpc ${BINDIR}/rvpc++
LINKS+=${BINDIR}/rvpc ${BINDIR}/rvpld
LINKS+=${BINDIR}/rvpc ${BINDIR}/qrvpld
LINKS+=${BINDIR}/rvpc ${BINDIR}/qrvpc
LINKS+=${BINDIR}/qclang-4.0 ${BINDIR}/qclang
LINKS+=${BINDIR}/qclang-4.0 ${BINDIR}/qld
MLINKS+=rvpc.1 rvpc++.1
MLINKS+=rvpc.1 rvpld.1
MLINKS+=rvpc.1 qrvpc.1

#
# I assume here and in rvpc.sh.in that there is only one directory
# between PREFIX and BINDIR, and that may not always be the case.
#
rvpc.sh! rvpc.sh.in
	@sed "s,--LIBEXECDIR--,$(LIBEXECDIR:C,^$(PREFIX)/,,g),g" < $(.ALLSRC) > $(.OBJDIR)/$(.TARGET)

.include <mkc.files.mk>
