Quickstart
==========

RV-Predict/C works in two steps.  First, ``$ rvpc file.c`` creates an
instrumented version of a multithreaded C program (rvpc is just a wrapper
for our customized version of clang compiler).  Second, ``$ ./a.out``
runs the program and performs offline data-race analysis.

.. code-block:: none

    rvpc file.c
    ./a.out

For C++ programs, just use ``rvpc++ file.cpp`` as shown below:

.. code-block:: none

    rvpc++ file.c
    ./a.out

You can also use RV-Predict/C with a piece of software built using GNU
Autoconf, use the following command (our tool currently relies on clang
compiler for the generation of the instrumented code):

.. code-block:: none

    CC=rvpc ./configure

You can also configure a makefile which has specified a CC variable for
specifying the compiler with

.. code-block:: none

    make <target> CC=rvpc

Note: if your code uses ``g++`` just replace ``rvpc`` with ``rvpc++``.
