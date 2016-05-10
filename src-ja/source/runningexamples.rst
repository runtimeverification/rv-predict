.. Running Examples
実行例
================

.. We provide examples demonstrating RV-Predict capabilities in detecting 
    concurrency bugs. Below we focus on detecting data races. 
    Data races are a common kind of concurrency bug in multi-threaded applications. 
    Intuitively, a data race occurs when two threads concurrently access a shared memory 
    and at least one of the accesses is a write. 
    Data races are very hard to detect with traditional testing techniques. It requires
    occurrence of simultaneous access from multiple treads to a particular region which
    results with a corrupted data that violates a particular user provided assertion or 
    test case. Therefore, traditional software engineering testing methodology is 
    inadequate because all tests passing most of the time with rare fails with mysterious
    rare message might create a false sense of reliability.

RV-Predictが並列処理のバグを検出する能力を説明します。以下、データ競合の検出にフォーカスします。データ競合はマルチスレッドを使用したアプリケーションにおいて、一般的な並列処理のバグです。２つのスレッドが共有メモリへ同時にアクセスし、少なくともその一つが書き込みの場合、データ競合が発生します。データ競合は、従来のテスト技術では検出が非常に難しいバグです。複数スレッドからある領域への同時アクセスの発生が必要となります。結果的に、アサーションやテストケースでは対応できないような破損データを引き起こすアクセスです。そのため、従来のソフトウェアエンジニアリングテスト手法は不十分です。テストを実施してもこの種のエラーはめったに発生しないので、すべてのテストが完了すれば、信頼性があると錯覚してしまう可能性があるからです。

.. Despite all the effort on solving this problem, it remains a challenge in practice to
    detect data races effectively and efficiently. RV-Predict aims to change this undesired situation. 
    Below we are summarizing some of the most common data races in C and C++ and show how 
    to detect them with RV-Predict. The examples described below can be found in RV-Predict[C] 
    distribution ``examples/demo`` directory.
    For any file in that directory, simply run ``rv-predict-c[++]-compile <file>.c[pp]`` to
    compile it, followed by ``rv-predict-execute ./a.out`` to execute it.

この問題を解決すべくあらゆる努力がなされていますが、効果的かつ効率的なデータ競合の検出はいまだに大きなチャレンジです。RV-Predictは、この望ましくない状況を変えることを目的としています。以下では、C/C++における最も一般的なデータ競合についていくつかまとめています。また、それらをRV-Predictで検出する方法を紹介します。以下で説明する例は、RV-Predict[C]ディストリビューションの ``examples/demo`` ディレクトリで確認できます。このディレクトリ内のファイルについては、単純に ``rv-predict-c[++]-compile <file>.c[pp]`` と実行するだけでコンパイルできます。実行するには、 ``rv-predict-execute ./a.out`` と続けてください。


.. 1. Concurrent Access to a Shared Variable
1. 共有変数への同時アクセス
-----------------------------------------
.. This is the simplest form of a data race, and also the most frequent in practice.
    The problem description is straightforward: multiple threads are accessing a shared
    variable without any synchronization.

これはデータ競合の最も単純なかたちで、実際に最も頻発するものでもあります。この問題の説明は簡単です：複数のスレッドが同期することなしに共有変数へアクセスしている。

.. POSIX Threads
POSIXスレッド
~~~~~~~~~~~~~

.. Consider the following snippet of the code from ``dot-product.c`` that uses POSIX Threads library
    for multi-threading.

以下は、 ``dot-product.c``  から抜粋したコードで、マルチスレッド用のPOSIXスレッドライブラリを使用しています。

.. code-block:: c

  void *dotprod(void *arg) {
    int i, start, end, len ;
    long offset;
    float mysum, *x, *y;
    offset = (long)arg;
     
    len = dotstr.veclen;
    start = offset*len;
    end   = start + len;
    x = dotstr.a;
    y = dotstr.b;

    mysum = 0;
    for (i = start; i < end ; i++) {
      mysum += (x[i] * y[i]);
    }

    dotstr.sum += mysum;
    printf("Thread %ld did %d to %d:  mysum=%f global sum=%f\n",offset,start,end,mysum,dotstr.sum);

    return NULL;
  }


.. The function dotprod is activated when the thread is created.
    All input to this routine is obtained from a structure 
    of type DOTDATA and all output from this function is written into
    this structure. The benefit of this approach is apparent for the 
    multi-threaded program: when a thread is created we pass a single
    argument to the activated function - typically this argument
    is a thread number. All  the other information required by the 
    function is accessed from the globally accessible structure. 

dotprod関数はスレッドが生成されたときに実行されます。このルーチンへの全入力はDOTDATA型の構造体から取得され、この関数の全出力はこの構造体へ書き込まれます。このアプローチの恩恵は、マルチスレッドプログラムでは明らかです：スレッドが生成されるとき、実行される関数にひとつの引数を渡しますが、たいていこの引数はスレッドナンバーです。関数によって必要とされる他のすべての情報は、グローバルにアクセス可能な構造体からアクセスされます。


.. code-block:: c

  int main (int argc, char *argv[]) {
    //  << code ommitted for brevity >>
    /* Create threads to perform the dotproduct  */
    for(i = 0; i < NUMTHRDS; i++) {
      pthread_create(&callThd[i], NULL, dotprod, (void *)i);
    }
    // << code ommitted for brevity >>
  }   
  

.. The main program creates threads which do all the work and then 
    print out result upon completion. Before creating the threads,
    the input data is created. 
    Each thread works on a different set of data.
    The offset is specified by ``i``. The size of
    the data for each thread is indicated by ``VECLEN`` (not shown above, please see the complete source).
    Since all threads update a shared structure, 
    there is a race condition. The main thread needs to wait for
    all threads to complete, it waits for each one of the threads.

mainプログラムは処理を実行するスレッドを生成し、処理が完了すると結果を出力します。スレッドを生成する前に、入力データが生成されます。各スレッドは異なるデータを処理します。 ``i`` でoffsetが指定されています。各スレッドで扱うデータのサイズは ``VECLEN`` （上の例にはないので、完全なソースを確認してください）で表されます。すべてのスレッドは共有の構造体を更新するので、レースコンディション（競合条件）が存在します。mainスレッドはすべてのスレッドが完了するのを待つ必要があり、それらのスレッドひとつひとつを待ちます。


.. RV-Predict[C] works in two steps. (Make sure you are in the directory examples/demo.)
    First, ``$ rv-predict-c-compile dot-product.c`` creates an instrumented version of a multi-threaded
    program that computes a dot products. 
    Second, ``$ rv-predict-execute ./a.out`` performs an offline analysis. 
    The results of the analysis:

RV-Predict[C]は２ステップで実行します。（examples/demoディレクトリ配下にいることを確認してください。）まず最初に、 ``$ rv-predict-c-compile dot-product.c``  が、ドットプロダクトを計算するマルチスレッドプログラムのインストルメントバージョンを生成します。次に、 ``$ rv-predict-execute ./a.out``  がオフライン解析を実行します。解析結果は以下の通りです：

.. code-block:: none

  Thread 0 did 0 to 10:  mysum=10.000000 global sum=10.000000
  Thread 1 did 10 to 20:  mysum=10.000000 global sum=20.000000
  Thread 2 did 20 to 30:  mysum=10.000000 global sum=30.000000
  Sum =  30.000000 
  Data race on global 'dotstr' of size 24 at 0x0000014b47a0 (a.out + 0x0000014b47b0): {{{
      Concurrent write in thread T3 (locks held: {})
   ---->  at dotprod dot-product.c:62
      T3 is created by T1
          at main dot-product.c:107

      Concurrent write in thread T2 (locks held: {})
   ---->  at dotprod dot-product.c:62
      T2 is created by T1
          at main dot-product.c:107
  }}}

  Data race on global 'dotstr' of size 24 at 0x0000014b47a0 (a.out + 0x0000014b47b0): {{{
      Concurrent read in thread T2 (locks held: {})
   ---->  at dotprod dot-product.c:62
      T2 is created by T1
          at main dot-product.c:107

      Concurrent write in thread T3 (locks held: {})
   ---->  at dotprod dot-product.c:62
      T3 is created by T1
          at main dot-product.c:107
  }}}

  Data race on global 'dotstr' of size 24 at 0x0000014b47a0 (a.out + 0x0000014b47b0): {{{
      Concurrent write in thread T2 (locks held: {})
   ---->  at dotprod dot-product.c:62
      T2 is created by T1
          at main dot-product.c:107

      Concurrent read in thread T3 (locks held: {})
   ---->  at dotprod dot-product.c:63
      T3 is created by T1
          at main dot-product.c:107
  }}}

.. First, note that the standard testing would not caught data races, 
    because the output and the final result are as expected. 
    However, RV-Predict's output correctly predicts three possible data races.
    The first one is on line 62: ``dotstr.sum += mysum;``, 
    where data race occurs because two threads can concurrently write to the shared variable. 
    The second data race is concerned with the same line, however this time our analysis
    informs that data race exists due to a concurrent read and a concurrent write. 
    Finally, the third report describes the case where there can be a concurrent write at line 62, 
    and a concurrent read at line 63: 
    ``printf("Thread %ld did %d to %d:  mysum=%f global sum=%f\n",offset,start,end,mysum,dotstr.sum);``.

まず、標準的なテストではデータ競合はほぼ発見されないということに注意してください。なぜなら、ここでの出力および最終的な結果もまた、期待通りのものだからです。しかし、RV-Predictの出力は３つのデータ競合の可能性を正しく予想しています。最初の競合は62行目の： ``dotstr.sum += mysum;``  ですが、２つのスレッドが同時に共有変数へ書き込む可能性があるため、データ競合が発生します。2つ目のデータ競合も同じ行ですが、同時読み出しおよび同時書き込みによるデータ競合が存在していることを知らせる解析結果となっています。最後に、3つ目の報告は62行目で同時書き込みがあり、63行目： ``printf("Thread %ld did %d to %d:  mysum=%f global sum=%f\n",offset,start,end,mysum,dotstr.sum);``  で同時読み出しがあるケースを説明しています。

.. This example also showcases the maximality and predictive power of our approach. In particular, 
    consider analysis results on the same program by widely used ThreadSanitizer tool from Google. 

この例はまた、我々のアプローチ、つまり最大限検出するということと予測力をよく表しています。特に、広く使用されているGoogle発のThreadSanitizerで同じプログラムを解析した結果を見てみましょう。

.. code-block:: none

  Thread 0 did 0 to 10:  mysum=10.000000 global sum=10.000000
  ==================
  WARNING: ThreadSanitizer: data race (pid=6010)
    Write of size 4 at 0x0000014ae3b0 by thread T2:
      #0 dotprod /home/eddie/work/rv-predict-c/examples/demo/dot-product.c:62:14 (a.out+0x0000004a53cd)

    Previous write of size 4 at 0x0000014ae3b0 by thread T1:
      #0 dotprod /home/eddie/work/rv-predict-c/examples/demo/dot-product.c:62:14 (a.out+0x0000004a53cd)

    Location is global 'dotstr' of size 24 at 0x0000014ae3a0 (a.out+0x0000014ae3b0)

    Thread T2 (tid=6013, running) created by main thread at:
      #0 pthread_create /home/eddie/work/llvm-3.7.0.src/projects/compiler-rt/lib/tsan/rtl/tsan_interceptors.cc:849 (a.out+0x000000446d93)
      #1 main /home/eddie/work/rv-predict-c/examples/demo/dot-product.c:107:5 (a.out+0x0000004a5668)

    Thread T1 (tid=6012, finished) created by main thread at:
      #0 pthread_create /home/eddie/work/llvm-3.7.0.src/projects/compiler-rt/lib/tsan/rtl/tsan_interceptors.cc:849 (a.out+0x000000446d93)
      #1 main /home/eddie/work/rv-predict-c/examples/demo/dot-product.c:107:5 (a.out+0x0000004a5668)

  SUMMARY: ThreadSanitizer: data race /home/eddie/work/rv-predict-c/examples/demo/dot-product.c:62:14 in dotprod
  ==================
  Thread 1 did 10 to 20:  mysum=10.000000 global sum=20.000000
  Thread 2 did 20 to 30:  mysum=10.000000 global sum=30.000000
  Sum =  30.000000 
  ThreadSanitizer: reported 1 warnings

.. Note, that ThreadSanitizer can only detect one data race, specifically, the case when 
    there are two concurrent writes to the shared variable. 

ThreadSanitizerはひとつのデータ競合のみを検出できるということに気づくでしょう。具体的には、共有変数へ２つの同時書き込みがあるケースです。

.. Furthermore, consider Helgrind, another widely used tool for detecting concurrency bug
    that is part of the Valgrind tool-set. The result of Helgrind analysis is shown below.

さらに、Helgrind、これはValgrindツールセットの一部で並列処理のバグを検出するための広く使用されているもう一つのツールについても見てみましょう。Helgrindの解析結果を以下に示します。

.. code-block:: none

  Thread 0 did 0 to 10:  mysum=10.000000 global sum=10.000000
  ==6192== ---Thread-Announcement------------------------------------------
  ==6192== 
  ==6192== Thread #3 was created
  ==6192==    at 0x515543E: clone (clone.S:74)
  ==6192==    by 0x4E44199: do_clone.constprop.3 (createthread.c:75)
  ==6192==    by 0x4E458BA: create_thread (createthread.c:245)
  ==6192==    by 0x4E458BA: pthread_create@@GLIBC_2.2.5 (pthread_create.c:611)
  ==6192==    by 0x4C30E0D: ??? (in /usr/lib/valgrind/vgpreload_helgrind-amd64-linux.so)
  ==6192==    by 0x40090F: main (dot-product.c:107)
  ==6192== 
  ==6192== ---Thread-Announcement------------------------------------------
  ==6192== 
  ==6192== Thread #2 was created
  ==6192==    at 0x515543E: clone (clone.S:74)
  ==6192==    by 0x4E44199: do_clone.constprop.3 (createthread.c:75)
  ==6192==    by 0x4E458BA: create_thread (createthread.c:245)
  ==6192==    by 0x4E458BA: pthread_create@@GLIBC_2.2.5 (pthread_create.c:611)
  ==6192==    by 0x4C30E0D: ??? (in /usr/lib/valgrind/vgpreload_helgrind-amd64-linux.so)
  ==6192==    by 0x40090F: main (dot-product.c:107)
  ==6192== 
  ==6192== ----------------------------------------------------------------
  ==6192== 
  ==6192== Possible data race during read of size 4 at 0x601080 by thread #3
  ==6192== Locks held: none
  ==6192==    at 0x4007E4: dotprod (dot-product.c:62)
  ==6192==    by 0x4C30FA6: ??? (in /usr/lib/valgrind/vgpreload_helgrind-amd64-linux.so)
  ==6192==    by 0x4E45181: start_thread (pthread_create.c:312)
  ==6192==    by 0x515547C: clone (clone.S:111)
  ==6192== 
  ==6192== This conflicts with a previous write of size 4 by thread #2
  ==6192== Locks held: none
  ==6192==    at 0x4007F5: dotprod (dot-product.c:62)
  ==6192==    by 0x4C30FA6: ??? (in /usr/lib/valgrind/vgpreload_helgrind-amd64-linux.so)
  ==6192==    by 0x4E45181: start_thread (pthread_create.c:312)
  ==6192==    by 0x515547C: clone (clone.S:111)
  ==6192==  Address 0x601080 is 16 bytes inside data symbol "dotstr"
  ==6192== 
  ==6192== ----------------------------------------------------------------
  ==6192== 
  ==6192== Possible data race during write of size 4 at 0x601080 by thread #3
  ==6192== Locks held: none
  ==6192==    at 0x4007F5: dotprod (dot-product.c:62)
  ==6192==    by 0x4C30FA6: ??? (in /usr/lib/valgrind/vgpreload_helgrind-amd64-linux.so)
  ==6192==    by 0x4E45181: start_thread (pthread_create.c:312)
  ==6192==    by 0x515547C: clone (clone.S:111)
  ==6192== 
  ==6192== This conflicts with a previous write of size 4 by thread #2
  ==6192== Locks held: none
  ==6192==    at 0x4007F5: dotprod (dot-product.c:62)
  ==6192==    by 0x4C30FA6: ??? (in /usr/lib/valgrind/vgpreload_helgrind-amd64-linux.so)
  ==6192==    by 0x4E45181: start_thread (pthread_create.c:312)
  ==6192==    by 0x515547C: clone (clone.S:111)
  ==6192==  Address 0x601080 is 16 bytes inside data symbol "dotstr"
  ==6192== 
  Thread 1 did 10 to 20:  mysum=10.000000 global sum=20.000000
  Thread 2 did 20 to 30:  mysum=10.000000 global sum=30.000000
  Sum =  30.000000 

.. Helgrind is able to detect two data races related to concurrent writes or a concurrent
    read and a concurrent write at line 62, but not is not able to predict with a concurrent write 
    at line 62 and a concurrent read at line 63. 

Helgrindは、62行目の同時書き込みおよび、同時読み出しと書き込みに関する２つのデータ競合を検出することができます。しかし、62行目で同時書き込みがあり、63行目で同時読み出しがあることについては予測てきていません。

C/C++ 11
~~~~~~~~~
.. One of the most significant features in the new C and C++11 Standard is the support 
    for multi-threaded programs. This the feature makes it possible to write multi-threaded
    C/C++ program without relying on platform specific extensions and writing portable multi-threaded
    code with standardized behavior. RV-Predict[C] support C/C++11 concurrency, and thus 
    it is able to detect concurrency bugs in the code written using C/C++11 constructs. 

新しいC/C++ 11スタンダードにおける最も重要な特徴の一つは、マルチスレッドプログラムのサポートです。これにより、プラットフォーム固有の拡張に頼ったり、標準化された振る舞いを持つ移植可能なマルチスレッドコードを書くことなしに、C/C++マルチスレッドプログラムを書くことができるようになりました。RV-Predict[C]はC/C++11の同時実行をサポートします。また、C/C++11を使用して書かれたコード中の並行処理のバグを検出することができます。

.. Consider the following example implementing a simple state machine. 

簡単なステートマシンを実装した以下の例を考えてみましょう。

.. code-block:: c

  mutex l;
  bool ready = false;
  enum State { STOP, INIT, START };
  State state = STOP;

  void init() {
    l.lock();
      ready = true;
    l.unlock();
    state = INIT;
    l.lock();
      ready = true;
    l.unlock();
  }

  void start() {
    // yield increases likelihood of avoiding expensive locking and unlocking
    // before being ready to enter the START state
    this_thread::yield(); 
    l.lock();
      if (ready && state == INIT) {
        state = START;
     }
    l.unlock();
  }

  void stop() {
    l.lock();
      ready = false;
      state = STOP;
    l.unlock();
  }

  int main() {
      thread t1(init);
      thread t2(start);
      thread t3(stop);
      t1.join(); t2.join(); t3.join();
      return 0;
  }

.. (For full source see examples/demo/simple-state-machine.cpp.)
    This program implements state machine with three states, and each thread models 
    some state machine transitions. Moreover, the developers seem to have devised a reasonable 
    locking policy that appears to protect shared resources. 
    This class of programs are hard to test, since there are many valid observable behaviors.
    So, some of the previously mentioned tools ThreadSanitizer or Helgrind can be used to 
    increase confidence in the correctness of the program. In fact, neither ThreadSanitizer 
    nor Helgrind report any problems with programs. 

(完全なソースは examples/demo/simple-state-machine.cpp を確認してください。)このプログラムは３つの状態をもつステートマシンを実装しており、各スレッドは状態遷移をモデル化しています。さらに、開発者は共有のリソースを保護するためと思われる妥当な策であるロックを思いついたようです。このクラスのプログラムはテストするのが困難です。というのも、たくさんの有効な監視可能な振る舞いがあるからです。そのため、ThreadSanitizeやHelgrind といった先ほど触れたツールを、プログラムが正しいことの信頼性を高めるために使用することができます。実際は、ThreadSanitizerもHelgrindもこのプログラムについて問題を報告しません。

.. However, there are three subtle data races in the program, and RV-Predict[C] finds them all. 

しかし、このプログラムには３つのデータ競合が存在し、RV-Predict[C]はそのすべてを見つけます。

.. Compile this programs as shown below. 

このプログラムは以下のようにコンパイルします。

.. code-block:: none

    rv-predict-c++-compile simple-state-machine.cpp
    rv-predict-execute ./a.out

.. The results of analysis will be:
解析の結果：

.. code-block:: none

  Data race on global 'state' of size 4 at 0x00000153ccf4 (a.out + 0x00000153ccf4): {{{
      Concurrent write in thread T2 (locks held: {})
   ---->  at init() simple-state-machine.cpp:19
      T2 is created by T1
          at main simple-state-machine.cpp:44

      Concurrent read in thread T3 (locks held: {WriteLock@94})
   ---->  at start() simple-state-machine.cpp:28
          - locked WriteLock@94 at start() simple-state-machine.cpp:27 
      T3 is created by T1
          at main simple-state-machine.cpp:44
  }}}

.. First data race is due to a write at line 19: ``state = INIT;``, while concurrently
    reading the current value of the state variable. This behavior might lead to a 
    behavior where the START state is not reached because of the aforementioned data race. 

最初のデータ競合は19行目： ``state = INIT;``  の書き込みによるものですが、一方で同時にstate変数の現在の値を読み出しています。前述のデータ競合により、START状態にはならない可能性があります。

.. code-block:: none

  Data race on global 'state' of size 4 at 0x00000153ccf4 (a.out + 0x00000153ccf4): {{{
      Concurrent write in thread T2 (locks held: {})
   ---->  at init() simple-state-machine.cpp:19
      T2 is created by T1
          at main simple-state-machine.cpp:44

      Concurrent write in thread T4 (locks held: {WriteLock@94})
   ---->  at stop() simple-state-machine.cpp:37
          - locked WriteLock@94 at stop() simple-state-machine.cpp:35 
      T4 is created by T1
          at main simple-state-machine.cpp:45
  }}}

.. Second data race is likely particularly dangerous, because there are concurrent
    writes of INIT and STOP to the state variable, which effectively means that the
    program could begin entering the START state with possibly critical reasons to 
    prevent the progress. 

２つ目のデータ競合は、state変数へINITとSTOPの同時書き込みがあるため、特に危険となりうるものです。これはつまり、ひょっとすると進捗を阻害する重大な理由を抱えながら、プログラムがSTART状態に入り始める可能性を示唆しています。

.. code-block:: none

  Data race on global 'state' of size 4 at 0x00000153ccf4 (a.out + 0x00000153ccf5): {{{
      Concurrent write in thread T2 (locks held: {})
   ---->  at init() simple-state-machine.cpp:19
      T2 is created by T1
          at main simple-state-machine.cpp:44

      Concurrent write in thread T3 (locks held: {WriteLock@94})
   ---->  at start() simple-state-machine.cpp:29
          - locked WriteLock@94 at start() simple-state-machine.cpp:27 
      T3 is created by T1
          at main simple-state-machine.cpp:44
  }}}

.. Finally, the third data race can effectively invert the state from START of INIT.

最後に、３つの目のデータ競合は、状態をSTARTからINITへ戻す可能性があります。

.. In summary, this simple program demonstrates that the state-of-the-art tools can be inadequate
    in detection of subtle data races with possibly dire consequences, while RV-Predict[C] can
    clearly identify all the data races. 

要するに、この単純なプログラムは、大惨事となりうる分かりにくいデータ競合の検出においては最先端のツールでも不十分であることを示しています。一方で、RV-Predict[C]は明らかにすべてのデータ競合を認識することができます。

.. 2. Unsafe Data Strucuture Manipulation
2. 安全でないデータ構造操作
--------------------------------------
    
.. Many standard library data structures are not designed to be used in a multi-threaded environment, 
    e.g. widely used vector class. 

多くの標準ライブラリデータ構造はマルチスレッド環境で使用されるように設計されていません。例えば、広く使用されているvectorクラス等です。

.. First, consider a simple example (examples.demo/unsafe-vector.c):

まずは、簡単な例（examples.demo/unsafe-vector.c）を見てみましょう：

.. code-block:: c

  #include <vector>
  #include <thread>

  using namespace std;

  vector<int> v;

  void thread1() {
      v.push_back(1);
  }

  void thread2() {
      v.push_back(2);
  }

  int main() {
      thread t1(thread1);
      thread t2(thread2);

      t1.join();
      t2.join();

      return 0;
  }

.. In the example both threads are trying to add to ``std::vector`` without synchronization.
    RV-Predict[C] catches the data race as shown below. 

例の２つのスレッドは同期しないで ``std::vector`` に追加しようとしています。RV-Predict[C]は、以下に示すようにデータ競合を捕捉します。 

.. code-block:: none

  Data race on global 'v' of size 24 at 0x00000153ecc8 (a.out + 0x00000153ecd8): {{{
      Concurrent read in thread T2 (locks held: {})
   ---->  at thread1() unsafe-vector.cpp:12
      T2 is created by T1
          at main unsafe-vector.cpp:20

      Concurrent write in thread T3 (locks held: {})
   ---->  at thread2() unsafe-vector.cpp:16
      T3 is created by T1
          at main unsafe-vector.cpp:20
  }}}

  ...

.. This example is easily fixed by using some synchronization mechanisms (e.g., locks) when
    performing the access to the shared variable ``v``. 

この例は、共有変数 ``v``  へのアクセスを実行する際に（ロック等の）同期メカニズムを使用することで、簡単に修正できます。

.. Consider now a more interesting example (see below), where we used ``vector`` data structure
    to implement a stack. At first sight, it looks like all the operations are properly synchronized, 
    however just because we are using a mutex or other synchronization mechanism to protect 
    shared data, it does not mean we are protected from race conditions!

今度はもっと興味深い例について考えてみましょう（以下を見てください）。ここでは、スタックを実装するために ``vector``  データ構造を使用しています。最初は、全ての操作が適切に同期されているように見えますが、共有データを保護するためにミューテックスやその他同期メカニズムを使用しているというだけなので、それはレースコンディションから保護されているということにはなりません。

.. code-block:: c

  using namespace std;
  mutex myMutex;
  class stack
  {
  public:
    stack() {};
    ~stack() {};
    void pop();
    int top() { return data.back(); }
    void push(int);
    void print();
    int getSize() { return data.size(); }
  private:
      vector<int> data;
  };

  void stack::pop()
  {
    lock_guard<mutex> guard(myMutex);
    data.erase(data.end()-1);
  }

  void stack::push(int n) {
    lock_guard<mutex> guard(myMutex);
    data.push_back(n);
  }

  void stack::print()
  {
    cout << "initial stack : " ;
    for(int item : data)
        cout << item << " ";
    cout << endl;
  }

  void process(int val, string s) {
    lock_guard<mutex> guard(myMutex);
    cout << s << " : " << val << endl;
  }

  void thread_function(stack& st, string s) {
    int val = st.top();
    st.pop();
    process(val, s);
  }

  int main()
  {
      stack st;
      for (int i = 0; i < 10; i++)  st.push(i);

      st.print();

      while(true) {
        if(st.getSize() > 0) {
          thread t1(&thread_function, ref(st), string("thread1"));
          thread t2(&thread_function, ref(st), string("thread2"));
          t1.join();
          t2.join();
        } else break;
      }

      return 0;
  }

.. (For full source see examples/demo/stack.cpp.)
    In the example below each shared access is guarded using

(完全なソースはexamples/demo/stack.cppをご覧ください。）この例では、各共有アクセスは以下を使用してガードされています。

.. code-block:: c
    
  lock_guard<mutex> guard(myMutex);
  
.. Now, it would be tempting to conclude that the code is thread-safe. 
    However, we actually cannot rely on the result of getSize(). 
    Although it might be correct at the time of call, once it returns
    other threads are free to access the stack and might push() new 
    elements to the stack or pop() existing elements of the stack. 

今、コードはスレッドセーフであると結論づけようとしています。しかし、実際はgetSize()の結果を信用することはできません。それは呼び出し時には正しいかもしれませんが、いったんリターンすると、他のスレッドはスタックへアクセスしたり、スタックへ新しい要素をpush()したり、スタックから既存の要素をpop()したりを自由にできます。

.. This particular data race is consequence of the interface design, and
    the use of mutex internally to protect the stack does not prevent it. 
    As shown below, RV-Predict[C] can be used to detect these kind of flaws. 

この特殊なデータ競合はインタフェース設計の結果であり、スタックを保護するための内部的なミューテックスの使用がそれを防ぐことはありません。以下で示すように、RV-Predict[C]をこの種の欠陥を検出するために使用することができます。

.. code-block:: none

  Data race on array element #11: {{{
      Concurrent read in thread T3 (locks held: {})
   ---->  at stack::top() Stack.cpp:18
      T3 is created by T1
          at main Stack.cpp:66

      Concurrent write in thread T2 (locks held: {WriteLock@27})
   ---->  at stack::pop() Stack.cpp:29
          - locked WriteLock@27 at stack::pop() Stack.cpp:29 
      T2 is created by T1
          at main Stack.cpp:65
  }}}



3. Double-checked Locking
-------------------------

.. Suppose you have a shared resource (e.g.shared a database connection or a large allocation a
    big chunk of of memory) that is expensive to construct, so it is only done when necessary. 
    A common idiom used in such cases is known as `double-checked locking` pattern. 
    The basic idea is that the pointer is first read without acquiring the lock, and the lock
    is acquired only if the pointer is NULL. The pointer is then checked again once the lock has
    been acquired in case another threads has done the initialization between the first check
    and this thread acquiring a lock. 

コンストラクトにコストのかかる共有リソース（例えば、共有のデータベースコネクションや大きな塊でのメモリアロケーション）がある場合、必要なときにだけコンストラクトされるでしょう。そのようなケースで使用される一般的なイディオムは、double-checked lockingパターンとして知られています。その基本的な考え方は、まず最初にロックを取得しないでポインタが読み出され、ポインタがNULLの場合にのみロックが取得される、というものです。最初のチェックとロック取得の間で他のスレッドが初期化を実施した場合にロックが取得されると、ポインタは再度チェックされます。

.. For full source see examples/demo/double-checked-locking.cpp.

完全なソースは examples/demo/double-checked-locking.cpp をご覧ください。

.. code-block:: c

  struct some_resource
  {
      void do_something()
      {}
      
  };

  std::shared_ptr<some_resource> resource_ptr;
  std::mutex resource_mutex;
  std::thread thread;
  std::thread join;
  void foo()
  {
    if(!resource_ptr) {
      std::unique_lock<std::mutex> lk(resource_mutex);
      if(!resource_ptr)
      {
          resource_ptr.reset(new some_resource);
      }
      resource_ptr->do_something();
    }
  }

  int main()
  {
      std::thread::thread t1(foo);
      std::thread::thread t2(foo);

      t1.join();
      t2.join();
  }

.. However, this pattern has become infamous because it has potential for a nasty race condition. 
    As shown below, RV-Predict[C] detect the race condition. Specifically, the data race occurs
    because the read outside the lock is not synchronized with the write done by the thread inside 
    the lock. The race condition includes the pointer and the object pointed to: even if a thread
    sees the pointer written by another thread, it might not see the newly created instance of 
    ``some_resource``, resulting in the call to ``do_something()`` operating on incorrect values. 

しかし、このパターンは悪名高くなりました。なぜなら、やっかいなレースコンディションの可能性があるからです。以下の通り、RV-Predict[C]はレースコンディションを検出します。具体的には、ロック外の読み出しが、ロック内のスレッドによる書き込みと同期されないため、データ競合が発生します。レースコンディションはポインタおよびそれが指すオブジェクトを含みます：あるスレッドが他のスレッドによって書き込まれるポインタを見ている場合でも、新しく生成された ``some_resource``  のインスタンスを見ていないかもしれません。結果、 ``do_something()``  が間違った値に作用するように呼び出されることになります。

.. code-block:: none

  Data race on global 'resource_ptr' of size 16 at 0x00000153dcc8 (a.out + 0x00000153dcc8): {{{
      Concurrent read in thread T3 (locks held: {})
   ---->  at foo() double-checked-locking.cpp:19
      T3 is created by T1
          at main double-checked-locking.cpp:32

      Concurrent write in thread T2 (locks held: {WriteLock@dc})
   ---->  at foo() double-checked-locking.cpp:23
          - locked WriteLock@dc at foo() double-checked-locking.cpp:21 
      T2 is created by T1
          at main double-checked-locking.cpp:32
  }}}
  ...


.. 4. Broken Spinnning Loop
4. スピンループからの脱出
------------------------

.. Sometimes we want to synchronize multiple threads based on whether some condition has been met. 
    And it is a common pattern to use a while loop that repeatedly checks that condition:

ある条件に一致するかどうかに基づいて、複数のスレッドを同期したい場合があります。そして、whileループを使用して繰り返しその条件をチェックするのが一般的なパターンです。

.. code-block:: c

  using namespace std;

  bool condition = false;
  int sharedVar;

  void thread1() {
      sharedVar = 1;
      condition = true;
  }

  void thread2() {
      while(!condition) {
          this_thread::yield();
      }
      if(sharedVar != 1) {
          throw new runtime_error("How is this possible!?");
      }
  }

  int main() {
      thread t1(thread1);
      thread t2(thread2);
      t1.join();
      t2.join();
      return 0;
  }

.. As shown below, RV-Predict[C] detect the data race on ``condition`` variable. 

以下の通り、RV-Predict[C]は変数 ``condition`` でのデータ競合を検知します。

.. code-block:: none

  Data race on global 'condition' of size 1 at 0x00000153cd88 (a.out + 0x00000153cd88): {{{
      Concurrent write in thread T2 (locks held: {})
   ---->  at thread1() spinning-loop.cpp:14
      T2 is created by T1
          at main spinning-loop.cpp:28

      Concurrent read in thread T3 (locks held: {})
   ---->  at thread2() spinning-loop.cpp:18
      T3 is created by T1
          at main spinning-loop.cpp:28
  }}}


