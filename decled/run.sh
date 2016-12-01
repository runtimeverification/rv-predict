#!/bin/sh

# system includes come from 
# ~/clang+llvm-3.8.1-x86_64-linux-gnu-ubuntu-16.04/bin/clang -v -x c++ -fsyntax-only /dev/null

$(dirname $0)/../build/decled $(dirname $0)/decled.cc -- -D__STDC_LIMIT_MACROS -D__STDC_CONSTANT_MACROS -I ~/clang+llvm-3.8.1-x86_64-linux-gnu-ubuntu-16.04/include/ -std=c++11 \
-isystem /usr/lib/gcc/x86_64-linux-gnu/5.4.0/../../../../include/c++/5.4.0 \
-isystem /usr/lib/gcc/x86_64-linux-gnu/5.4.0/../../../../include/x86_64-linux-gnu/c++/5.4.0 \
-isystem /usr/lib/gcc/x86_64-linux-gnu/5.4.0/../../../../include/c++/5.4.0/backward \
-isystem /usr/local/include \
-isystem /home/dyoung/clang+llvm-3.8.1-x86_64-linux-gnu-ubuntu-16.04/bin/../lib/clang/3.8.1/include \
-isystem /usr/include/x86_64-linux-gnu \
-isystem /usr/include \
-I ~/clang+llvm-3.8.1-x86_64-linux-gnu-ubuntu-16.04/include

