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

mkfifo ${rootfifo}
tail -f ${rootfifo} > ${rootsave} &

# avoid creating files & directories with permissions 0775 
umask 022
mkcmake FAKEROOT_FIFO=${rootfifo} RELEASE=yes DESTDIR=${destdir} PREFIX=/usr install
fakeroot -s ${rootfifo} cp -rp DEBIAN ${destdir}/.
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
fakeroot -i ${rootsave} dpkg-deb -b ${destdir} rv-predict-c-${version}.deb
exit 0
