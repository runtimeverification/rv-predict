#!/bin/sh

set -e
set -u

usage()
{
	echo "usage: $(basename $0) version [treeish]" 1>&2
	exit 1
}

[ $# -lt 1 -o 2 -lt $# ] && usage

version=$1

treeish=${2:-HEAD}

release_dir=rv-predict-c-${version}

objdir=$(mkcmake -V .OBJDIR)

if [ -e ${objdir}/${release_dir} ]; then
	echo "${objdir}/${release_dir} already exists; it is in the way." 1>&2
	exit 1
fi

git archive --format=tar --prefix=$release_dir/ ${treeish} | \
    tar x -C ${objdir}

cd ${objdir}/${release_dir}

if dch -v ${version}-1 'Miscellaneous bug fixes and new features.' && \
   dch -r 'Miscellaneous bug fixes and new features.' && \
   MVN="mvn -Duser.home=$HOME" dpkg-buildpackage -us -uc -b
then
	echo "dpkg-buildpackage succeeded." 1>&2
	cd -
	cp ${objdir}/${release_dir}/debian/changelog debian/changelog
	echo "Copied back new debian/changelog." 1>&2
	echo "Cleaning up ${release_dir}." 1>&2
	rm -rf ${release_dir}
	exit 0
fi

rc=$?
echo "dpkg-buildpackage failed. Left ${release_dir} for post-mortem." \
    1>&2
rm -rf /${release_dir}
exit $rc
