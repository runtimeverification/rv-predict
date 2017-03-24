#!/bin/sh

set -e

usage()
{
	echo "usage: $(basename $0) { --pc | --ptr } program" 1>&2
	exit 1
}

[ $# -eq 2 ] || usage

case $1 in
--pc)
	regex='at \(0x[0-9a-f]\+\) dummy.c:999'
	sed_template='s,^0x\([0-9a-f]\+\) \([^@]\+\)@\(.*\)$,s|at 0x0\\+\1 dummy.c:999|at \2 \3|g,'
	;;
--ptr)
	regex='\[\(0x[0-9a-f]\+\)\]'
	sed_template='s,^0x\([0-9a-f]\+\) \([^@]\+\)@\(.*\)$,s|\[0x0\\+\1\]|\2 at \3|g,'
	;;
*)
	usage
	;;
esac

tmpdir=$(mktemp -d)
tee $tmpdir/original | \
grep "$regex" | sed "s/.*$regex.*/\1/g" | sort -u | \
    llvm-symbolizer -obj $2 -inlining -print-address | \
    awk 'BEGIN {RS = "" ; FS = "\n"} /^0x/,/^$/ { print $1 " " gensub(" ", "\\\\ ", "g", $2) " " $3; }' | \
while read addr symbol path; do
	echo $addr ${symbol}@$(basename $path)
	
done | \
    sed "$sed_template" > $tmpdir/sed_script

sed -f $tmpdir/sed_script < $tmpdir/original

rm -rf $tmpdir

exit 0
