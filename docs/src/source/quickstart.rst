Quickstart
==========

RV-Predict[C] works in two steps.
First, ``$ rv-predict-c-compile file.c`` creates an instrumented version of a 
multithreaded C program (rv-predict-c-compile is just a wrapper for our customized 
version of clang compiler). 
Second, ``$ rv-predict-execute ./a.out`` performs and offline data race analysis. 

.. code-block:: none

    rv-predict-c-compile file.c
    rv-predict-execute ./a.out

For c++ programs, just use ``rv-predict-c++-compile file.cpp`` as shown below:

.. code-block:: none

    rv-predict-c++-compile file.c
    rv-predict-execute ./a.out


You can also use RV-Predict[C] with a piece of software built using Gnu Autoconf, use the
following command (our tool currently relies on clang compiler for the generation of the instrumented code):

.. code-block:: none

    CC=clang CFLAGS=-fsanitize=rvpredict ./configure

You can also configure a makefile which has specified a CC variable for
specifying the compiler with

.. code-block:: none

    make <target> CC=clang CFLAGS=-fsanitize=rv-predict

Note: if your code uses ``g++`` just replace ``clang`` with ``clang++``.
