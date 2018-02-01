Running RV-Predict/C
====================

RV-Predict/C works in two steps.  First, ``$ rvpc file.c`` creates an
instrumented version of a multithreaded C program (``rvpc`` is just
a wrapper for our customized version of clang compiler).  Second, ``$
./a.out`` runs the program and performs offline data-race analysis.

.. code-block:: none

    rvpc file.c
    ./a.out

For C++ programs, just use ``rvpc++ file.cpp`` as shown below:

.. code-block:: none

    rvpc++ file.cpp
    ./a.out

To use RV-Predict/C with a piece of software that is configured using GNU
Autoconf, use the following command:

.. code-block:: none

    CC=rvpc ./configure

To use RV-Predict/C on a project that uses a makefile but does *not*
use GNU autoconf, provide a CC variable on the command line:

.. code-block:: none

    make <target> CC=rvpc

Note: if your code uses ``g++`` or ``clang++``, then replace ``rvpc``
with ``rvpc++``.
