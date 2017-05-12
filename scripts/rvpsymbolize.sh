#!/bin/sh

set -e

usage()
{
	echo "usage: $(basename $0) program" 1>&2
	exit 1
}

[ $# -eq 1 ] || usage

normalize()
{
	sed 's/at \(0x[0-9a-f]\+\) dummy.c:999/{\1}/g'
}

func_addr_regex='{\(0x[0-9a-f]\+\)}'
func_sym_sed_template='s,^\(.\+\);;\(.\+\);;\(.*\)$,s|\1|in \2 at \3|g,'

data_addr_regex='\(\[0x[0-9a-f]\+[^]]*\]\)'
data_sym_sed_template='s,^\(.\+\);;\(.\+\);;\(.\+\)$,s|\3|\2 at \1|g,'

tmpdir=$(mktemp -d)

last_n_components()
{
	ncomponents=$1
	filename=$2
	result=$(basename $filename)
	while [ $ncomponents -gt 1 ]; do
		filename=$(dirname $filename)
		[ $filename = "." ] && break
		ncomponents=$((ncomponents - 1))
		result=$(basename $filename)/$result
	done
	if [ $((${#result} + 4)) -lt ${#filename} ]; then
		echo ".../${result}"
	else
		echo $filename
	fi
}

normalize | tee $tmpdir/original | \
grep "$func_addr_regex" | sed "s/.*$func_addr_regex.*/{\1}/g" | sort -u | \
    rvsyms -r $1 | \
    sed 's/\([^:]\+\)\(:[^:]\+:[^;]\+\);\(.\+\);;\(.\+\)$/\4 \1 \2 \3/
s/\([^:]\+\)\(:[^;]\+\);\(.\+\);;\(.\+\)$/\4 \1 \2 \3/
s/\([^;]\+\);\(.\+\);;\(.\+\)$/\3 \1 :: \2/' | \
tee $tmpdir/funcsyms_proto_proto_script | \
while read regex path linecol symbol; do
	shortened_path=$(last_n_components 2 $path)
	echo "${regex};;${symbol};;${shortened_path}${linecol##::}"
done | tee $tmpdir/funcsyms_proto_script | sed "$func_sym_sed_template" > $tmpdir/funcsyms_sed_script

grep "$data_addr_regex" < $tmpdir/original | \
sed "s/.*$data_addr_regex.*/\1/g" | sort -u | \
    rvsyms -r $1 | \
    tee $tmpdir/datasyms_proto_script | \
    sed "$data_sym_sed_template" > $tmpdir/datasyms_sed_script

sed -f $tmpdir/datasyms_sed_script < $tmpdir/original | \
    sed -f $tmpdir/funcsyms_sed_script

if true; then
	rm -rf $tmpdir
else
	echo look in $tmpdir 1>&2
fi

exit 0
