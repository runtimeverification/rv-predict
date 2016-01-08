#!/bin/sh


echo -n "Single C file: "
rv-predict-compile *\.c && ./a.out && rm a.out && rm *bin
