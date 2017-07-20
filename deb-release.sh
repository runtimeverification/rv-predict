#!/bin/sh

set -e
set -u

usage()
{
	echo "usage: $(basename $0) version" 1>&2
	exit 1
}

cleanup()
{
	trap - EXIT ALRM HUP INT PIPE QUIT TERM
	rm -rf ${destdir} ${tmpdir}
}

[ $# -eq 1 ] || usage

version=$1

destdir=$(mktemp -d -t $(basename $0).destdir.XXXXXX)
tmpdir=$(mktemp -d -t $(basename $0).XXXXX)
rootfifo=${tmpdir}/rootfifo
rootsave=${tmpdir}/rootsave

trap cleanup EXIT ALRM HUP INT PIPE QUIT TERM
rm -f rv-predict-c.deb
if ! type mkcmake > /dev/null 2>&1; then
	export PATH=${HOME}/pkg/bin:${PATH}
fi

# I do not want to run this script as root, but the files in a Debian
# binary package need to have the proper ownership, root:root.  In
# Ubuntu, the way for an unprivileged user to install a file "as if"
# using root permissions is with fakeroot. fakeroot will save file
# meta-information to a file, however, independent invocations of
# fakeroot truncate the meta-information file.  In order to accumulate
# meta-information over several invocations of 'fakeroot install -o root
# -g root ...', I use a FIFO as the output file.  I empty the FIFO to a
# regular file in the background using tail -f.
#
# TBD: this would be a lot easier with NetBSD's install(1), mtree(1),
# and pax(1) utilities, so someday I may install a NetBSD cross
# toolchain on the RV, Inc., development boxes.
# 
mkfifo ${rootfifo}
tail -f ${rootfifo} > ${rootsave} &

# avoid creating files & directories with permissions 0775 
umask 022

#
# Call mkcmake with FAKEROOT_FIFO set so that meta-information is
# captured.  When FAKEROOT_FIFO is set, Makefile.common sets INSTALL to
# fakeroot $(INSTALL) -s $(FAKEROOT_FIFO).
#
mkcmake FAKEROOT_FIFO=${rootfifo} RELEASE=yes DESTDIR=${destdir} PREFIX=/usr install
fakeroot -s ${rootfifo} cp -rp DEBIAN ${destdir}/.
fakeroot -s ${rootfifo} chmod 0755 ${destdir}/DEBIAN/*
cat > ${tmpdir}/control <<END_OF_CONTROL
Package: rv-predict-c
Version: ${version}-1
Architecture: amd64
Depends: clang (>= 1:3.8~), clang (<< 1:3.9~), java8-runtime, libc6
Section: devel
Priority: optional
Maintainer: David Young <david.young@runtimeverification.com>
Description: RV-Predict/C predicts data races in C and C++ programs.
 Use RV-Predict/C compiles and runs a C/C++ program with
 instrumentation that produces an execution trace.  RV-Predict/C
 analyzes the execution trace and reports the data races that could
 occur.
END_OF_CONTROL
fakeroot -s ${rootfifo} install -o root -g root -m 0664 \
    ${tmpdir}/control ${destdir}/DEBIAN/control
fakeroot -s ${rootfifo} chown -R root:root ${destdir}/DEBIAN

#
# Use `fakeroot -i ${rootsave}` to invoke `dpkg-deb` so that
# in the binary package `dpkg-deb` builds, the meta-information
# that we accumulated previously takes effect.
#
fakeroot -i ${rootsave} dpkg-deb -b ${destdir} rv-predict-c-${version}.deb
exit 0
