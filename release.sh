#!/bin/sh

rm -f rv-predict.tar.gz
mkcmake RELEASE=yes PREFIX= bin_targz
