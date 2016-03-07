Quickstart
==========

RV-Predict[C] works in two steps.
First, ``$ rv-predict-compile file.c`` creates an instrumented version of a 
multithreaded program (rv-predict-compile is just a wrapper for our customized 
version of clang compiler). 
Second, ``$ rv-predict-execute ./a.out`` performs and offline data race analysis. 

.. code-block:: none

    rv-predict-compile file.c
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
