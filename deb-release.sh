#!/bin/sh

set -e
set -u
set -x

usage()
{
	echo "usage: $(basename $0) version" 1>&2
	exit 1
}

cleanup()
{
	trap - EXIT ALRM HUP INT PIPE QUIT TERM
	cd ${start_dir}
	echo "============ Failed ============"
	echo $@ $version
	echo ${start_dir} ${tmpdir}
	# rm -rf ${tmpdir}
}

[ $# -eq 1 ] || usage

version=$1

start_dir=$(pwd)
tmpdir=$(mktemp -d -t $(basename $0).XXXXX)
destdir=${tmpdir}/debian/rv-predict-c

trap cleanup EXIT ALRM HUP INT PIPE QUIT TERM

changelog_regex="^rv-predict-c ($(echo $version | sed 's,\.,\\.,g')-1) xenial; urgency=medium"

cp -a debian $tmpdir/

if [ ${version%%-SNAPSHOT} != ${version} ]
then
	cat - debian/changelog > ${tmpdir}/debian/changelog <<CHANGELOG_TEMPLATE
rv-predict-c (${version}-1) xenial; urgency=medium

  * Miscellaneous bug fixes and new features.

 -- Runtime Verification, Inc. <support@runtimeverification.com>  $(date +'%a, %d %b %Y %H:%M:%S %z')

CHANGELOG_TEMPLATE
elif ! head -1 debian/changelog > ${tmpdir}/changelog_first_line || \
     ! grep -q "${changelog_regex}" < ${tmpdir}/changelog_first_line
then
	echo "Before you run $(basename $0), you should add version ${version} to debian/changelog." 1>&2
	exit 1
else
	cp debian/changelog ${tmpdir}/debian/changelog
fi

gzip -9fn < ${tmpdir}/debian/changelog > ${tmpdir}/changelog.Debian.gz


# Create `version` file that records rv-predict/c version number
echo $version > ${tmpdir}/version

# avoid creating files & directories with permissions 0775 
#umask 022

NBINSTALL="nb-install -U -D ${destdir} -M ${destdir}/metalog -h md5"
DEBINSTALL="nb-install -U -D ${tmpdir}/debian -M ${tmpdir}/debian/metalog"

#
# Get nb-install onto our PATH
#
export PATH=$(rvpmake -V '$(.OBJDIR)')/.tools/bin:${PATH}

#
# Call rvpmake with NBINSTALL=yes so that meta-information is
# captured.
#
RVPMAKE="rvpmake DEBUG=yes RELEASE=yes DESTDIR=${destdir} PREFIX=/usr PACKAGE_LIBDIR=/usr/lib/x86_64-linux-gnu LIBEXECDIR=/usr/lib/x86_64-linux-gnu"
$RVPMAKE all

# nb-install requires that the directory that receives the metalog exists.
mkdir -p ${destdir}
$NBINSTALL -d -o root -g root -m 0755 ${destdir}/
$NBINSTALL -d -o root -g root -m 0755 ${destdir}/usr
$NBINSTALL -d -o root -g root -m 0755 ${destdir}/usr/lib
$NBINSTALL -d -o root -g root -m 0755 ${destdir}/usr/share
$NBINSTALL -d -o root -g root -m 0755 ${destdir}/usr/share/doc
$NBINSTALL -d -o root -g root -m 0755 ${destdir}/usr/share/doc/rv-predict-c
$NBINSTALL -o root -g root -m 0644 ${tmpdir}/changelog.Debian.gz \
    ${destdir}/usr/share/doc/rv-predict-c/changelog.Debian.gz
$NBINSTALL -o root -g root -m 0444 ${tmpdir}/version \
    ${destdir}/usr/share/doc/rv-predict-c/version
$NBINSTALL -d -o root -g root -m 0755 ${destdir}/usr/share/examples
$NBINSTALL -d -o root -g root -m 0755 ${destdir}/usr/share/examples/rv-predict-c
$NBINSTALL -d -o root -g root -m 0755 ${destdir}/usr/share/man
$NBINSTALL -d -o root -g root -m 0755 ${destdir}/usr/share/rv-predict-c
$NBINSTALL -d -o root -g root -m 0755 ${destdir}/usr/share/rv-predict-c/html-report
$RVPMAKE NBINSTALL="${NBINSTALL}" install
cd $tmpdir
dh_makeshlibs
dpkg-shlibdeps debian/rv-predict-c/usr/lib/x86_64-linux-gnu/rvpinstrument.so \
    debian/rv-predict-c/usr/bin/rvpdump \
    debian/rv-predict-c/usr/bin/rvp-error \
    debian/rv-predict-c/usr/bin/rvpshortenpaths \
    debian/rv-predict-c/usr/bin/rvpsymbolize-json \
    debian/rv-predict-c/usr/bin/rvptrimframe \
    debian/rv-predict-c/usr/bin/rvsyms

mkdir -p debian/rv-predict-c/DEBIAN
dpkg-gencontrol -Pdebian/rv-predict-c
cd $start_dir

deb_filename=rv-predict-c_${version}-1_amd64.deb
man_match="^\./usr/share/man/.*\.[0-9]\>"

cd ${destdir}/DEBIAN
nb-pax -wzf ${tmpdir}/control.tar.gz .
cd ${destdir}/

grep "${man_match}" < ${destdir}/metalog > ${destdir}/metalog.man
grep -v "${man_match}" < ${destdir}/metalog > ${destdir}/metalog.dedup

mv -f ${destdir}/metalog.dedup ${destdir}/metalog

while read fn rest; do
	gzip -9fn < $fn > ${tmpdir}/tmp.gz
	$NBINSTALL -o root -g root -m 0644 \
	  ${tmpdir}/tmp.gz ${destdir}/${fn##./}.gz
	rm -f ${tmpdir}/tmp.gz
done < ${destdir}/metalog.man

{
	echo "/set uname=root gname=root"
	#
	# lintian(1) expects directory names to end in /, apparently.
	# Make it so.  Also sort by filename.
	#
#	sed 's,\([^[:space:]]*[^/]\)\([[:space:]]\+.*type=dir.*\)$,\1/\2,' < \
#	    ${destdir}/metalog | sort -u -k 1,1
	sort -u -k 1,1 < ${destdir}/metalog
} | tee ${destdir}/metalog.new | nb-mtree -D -k md5 | \
grep '\<md5=' | sed 's/[[:space:]]*\<type=[^[:space:]]\+[[:space:]]*//' | \
sed 's/md5=\([^[:space:]]\+\)/\1 /' > ${tmpdir}/md5sum
$DEBINSTALL -o root -g root -m 0644 ${tmpdir}/md5sum ${destdir}/DEBIAN/md5sums

mv -f ${destdir}/metalog.new ${destdir}/metalog

nb-pax --xz -M -wf ${tmpdir}/data.tar.xz < ${destdir}/metalog
echo "2.0" > ${tmpdir}/debian-binary
cd ${tmpdir}
ar rcs ${start_dir}/${deb_filename} debian-binary control.tar.gz data.tar.xz

exit 0
