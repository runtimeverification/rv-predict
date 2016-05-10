.. Quickstart
クイックスタート
==========

.. RV-Predict[C] works in two steps.
    First, ``$ rv-predict-c-compile file.c`` creates an instrumented version of a 
    multithreaded C program (rv-predict-c-compile is just a wrapper for our customized 
    version of clang compiler). 
    Second, ``$ rv-predict-execute ./a.out`` performs and offline data race analysis. 

RV-Predict[C]は２ステップで機能します。まずは、``$ rv-predict-c-compile file.c`` が、マルチスレッドCプログラムのインスツルメンテッドバージョンを生成します。（rv-predict-c-compileは我々がカスタマイズしたバージョンのclangコンパイラのラッパーです。）次に、``$ rv-predict-execute ./a.out`` でオフラインでデータ競合解析を実行します。

.. code-block:: none

    rv-predict-c-compile file.c
    rv-predict-execute ./a.out

.. For c++ programs, just use ``rv-predict-c++-compile file.cpp`` as shown below:
C++プログラムの場合は、以下のように``rv-predict-c++-compile file.cpp``を使用してください：

.. code-block:: none

    rv-predict-c++-compile file.c
    rv-predict-execute ./a.out


.. You can also use RV-Predict[C] with a piece of software built using Gnu Autoconf, use the
    following command (our tool currently relies on clang compiler for the generation of the instrumented code):
また、GNU Autoconfを使用してビルドされるソフトウェアと一緒にRV-Predict[C]を使うこともできます。以下のコマンドを使用してください（我々のツールは現在、インスツルメンテッドコードの生成についてclangコンパイラに依存しています）:

.. code-block:: none

    CC=clang CFLAGS=-fsanitize=rvpredict ./configure

.. You can also configure a makefile which has specified a CC variable for
    specifying the compiler with
以下のように、コンパイラ指定のためのCC変数を定義したmakefileを構成することも可能です。

.. code-block:: none

    make <target> CC=clang CFLAGS=-fsanitize=rv-predict

.. Note: if your code uses ``g++`` just replace ``clang`` with ``clang++``.
注：あなたのコードが``g++``を使用している場合は、``clang``を``clang++``に置き換えるだけです。
