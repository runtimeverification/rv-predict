FROM ubuntu
MAINTAINER Runtime Verification 

RUN apt-get update 
RUN apt-get upgrade -y  

# Install prerequisite packages
RUN apt-get install -y m4 
RUN apt-get install -y make 
RUN apt-get install -y maven mk-configure
RUN apt-get install -y opam openjdk-8-jdk-headless
RUN apt-get install -y pandoc 
RUN apt-get install -y sed   
RUN apt-get install -y clang-4.0 llvm-4.0

# Setup LLVM 
RUN ln -s /usr/bin/clang-4.0 /usr/bin/clang
RUN ln -s /usr/bin/clang++-4.0 /usr/bin/clang++

