#!/bin/sh

ntags_walker=$(./walker ./walker | wc -l)
ntags_objdump=$(objdump --dwarf=info ./walker | grep 'DW_TAG_' | wc -l)

if [ $ntags_walker -ne $ntags_objdump ]; then
	echo "walker found ${ntags_walker} tags, objdump found ${ntags_objdump}"
	exit 1
fi

exit 0
