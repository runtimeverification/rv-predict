#!/bin/sh

{
	echo stderr 1>&2
	echo stdout
} 3>&2 2>&1 1>&3 3>&- | tee stdout

