#!/bin/sh

set -e
set -u

destdir=$(mktemp -d -t $(basename $0).XXXXXX)

cleanup()
{
	trap - EXIT ALRM HUP INT PIPE QUIT TERM
	rm -rf ${destdir}
}

trap cleanup EXIT ALRM HUP INT PIPE QUIT TERM
rm -f rv-predict-c.deb
if ! type mkcmake > /dev/null 2>&1; then
	export PATH=${HOME}/pkg/bin:${PATH}
fi

mkcmake RELEASE=yes DESTDIR=${destdir} PREFIX=/usr install
cp -rp DEBIAN ${destdir}/.
chown -R root:root ${destdir}/DEBIAN
dpkg-deb -b ${destdir} rv-predict-c.deb
exit 0
