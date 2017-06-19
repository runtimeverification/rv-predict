#!/bin/sh
#
# Note: this is a more-or-less "portable" Bourne shell script.
# That is, it is not reliant on any bash-isms.
#

pass_dir=$(dirname $0)/../lib
runtime_dir=$(dirname $0)/../lib

cplusplus=no
sources=no
link=yes
compile=yes

prog=$(basename $0)

if [ ${prog%%++} != ${prog} ]; then
	cplusplus=yes
fi

for arg in "$@"; do
	case "$arg" in
	--)	break
		;;
	-MD)	continue
		;;
	-M*|-E)	compile=no
		link=no
		continue
		;;
	-m32)	bits=32
		continue
		;;
	-m64)	bits=64
		continue
		;;
	-[cS]|-shared)	link=no
		continue
		;;
	*.cc|*.cp|*.cxx|*.cpp|*.CPP|*.c++|*.C)
		cplusplus=yes
		sources=yes
		;;
	*.c)
		sources=yes
		;;
	esac
done


if [ ${cplusplus:-no} = yes ]; then
	compiler="clang++ -std=c++11"
else
	compiler=clang
fi

if [ ${sources:-yes} = yes -a ${compile:-yes} = yes ]; then
	pass="-Xclang -load -Xclang $pass_dir/rvpinstrument.so -g"
fi

# -ldl for dlsym()
# -lrt for timer_create() et cetera, in hacks.c
# -pthread for POSIX threads
if [ ${link:-yes} = yes ]; then
	runtime="-L${runtime_dir} -lrvprt${bits:-} -ldl -lrt -pthread -g"
fi

$compiler ${pass:-} "$@" ${runtime:-}
