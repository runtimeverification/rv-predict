#####Installation instructions:

1. clone https://github.com/runtimeverification/rv-predict-llvm
2. clone https://github.com/runtimeverification/rv-predict-clang in rv-predict-llvm/tools 
3. clone https://github.com/runtimeverification/rv-predict-compiler-rt in rv-predict-llvm/projects

[then follow the instructions here](http://llvm.org/docs/GettingStarted.html)

to build clang using CMake

*****

#####Usage instructions:

Add the flag **-fsanitize=rvpredict** when compiling with clang.


If you want proper line numbers and file information when reporting races, add the **-g** flag  for debugging symbols.

Once the binary is done, test using rv-predict-llvm \<binary file\>

Example usage:

```bash
clang++ -std=c++11 -fsanitize=rvpredict -g main.cpp -o file

rv-predict-llvm ./file
```
