#!/bin/sh


echo -n "Single CPP file: "
rv-predict-compile *\.cpp && ./a.out && rm a.out && rm *bin
