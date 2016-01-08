#!/bin/bash


echo -n "Multiple C++ files, different extensions, C++11 support: "
rv-predict-compile -std=c++11 main.cpp file1.cc file2.C && ./a.out && rm a.out && rm *bin
