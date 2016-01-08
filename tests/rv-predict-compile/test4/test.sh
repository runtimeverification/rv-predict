#!/bin/sh


echo -n "Multiple C files: "
rv-predict-compile *\.c && ./a.out && rm a.out && rm *bin
