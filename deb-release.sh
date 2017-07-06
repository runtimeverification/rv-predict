#!/bin/sh

set -e
set -u

destdir=$(mktemp -d -t $(basename $0).destdir.XXXXXX)
tmpdir=$(mktemp -d -t $(basename $0).XXXXX)
rootfifo=${tmpdir}/rootfifo
rootsave=${tmpdir}/rootsave

cleanup()
{
	trap - EXIT ALRM HUP INT PIPE QUIT TERM
	rm -rf ${destdir} ${tmpdir}
}

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
fakeroot -s ${rootfifo} chown -R root:root ${destdir}/DEBIAN
fakeroot -i ${rootsave} dpkg-deb -b ${destdir} rv-predict-c.deb
exit 0
