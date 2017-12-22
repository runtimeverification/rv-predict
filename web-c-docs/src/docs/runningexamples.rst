Running Examples
================

We provide examples demonstrating RV-Predict capabilities in detecting
concurrency bugs. Below we focus on detecting data races.  Data races
are a common kind of concurrency bug in multithreaded applications.
A data race occurs when two threads concurrently access a shared memory
and at least one of the accesses is a write.  Data races are very hard
to detect with traditional testing techniques. It requires occurrence
of simultaneous access from multiple threads to a particular region
which results in corrupted data that violates a particular user-provided
assertion or test case.  Traditional software engineering testing methods
are inadequate, because all tests passing most of the time with only rare,
mysterious failures might create a false sense of reliability.

Despite all the effort spent on solving this problem, it remains
a challenge in practice to detect data races effectively and
efficiently. RV-Predict aims to change this undesirable situation.
Below we summarize some of the most common data races in C and C++,
showing how to detect them with RV-Predict. The examples described below
can be found in the directory ``/usr/share/examples/rv-predict-c``.

1. Concurrent Access to a Shared Variable
-----------------------------------------
This is the simplest form of a data race, and also the most frequent
in practice.  The problem description is straightforward: multiple
threads are accessing a shared variable without any synchronization.

POSIX Threads
~~~~~~~~~~~~~

Consider the following snippet of the code from ``dot-product.c`` that
uses the POSIX Threads library for multithreading.

.. code-block:: c
	:number-lines: 37
  
	static void *
	dotprod(void *arg)
	{
		/* Define and use local variables for convenience */

		int i, start, end, len;
		long offset;
		float mysum, *x, *y;
		offset = (long)arg;

		len = dotstr.veclen;
		start = offset * len;
		end   = start + len;
		x = dotstr.a;
		y = dotstr.b;

		/*
		 * Perform the dot product and assign result
		 * to the appropriate variable in the structure.
		 */

		mysum = 0;
		for (i = start; i < end; i++)
		mysum += x[i] * y[i];

		dotstr.sum += mysum;
		printf("Thread %ld did %2d to %2d:  mysum=%f global sum=%f\n",
		offset, start, end, mysum, dotstr.sum);

		return NULL;
	}


The function ``dotprod`` is activated when the thread is created.  All
input to this routine is obtained from a structure of type ``DOTDATA``
and all output from this function is written into this structure.
All the other information required by the function is accessed from the
globally accessible structure.


.. code-block:: c

	int
	main(void)
	{
		// << code omitted for brevity >>

		for(i = 0; i < NUMTHRDS; i++) {
			pthread_create(&callThd[i], NULL, dotprod, (void *)i);
		}

		// << code omitted for brevity >>
	}

The main program creates some input data before it creates threads that
perform the dot products. Each thread works
on a different slice of data, ``VECLEN`` items long, starting at the
offset given by ``i``.  The main thread waits for each thread to complete.
Then, it prints the resulting sum.  Since all threads update a shared
structure, there is a race condition.

We will apply RV-Predict/C to ``dot-product`` in two steps.  Make sure you
are in the directory ``/usr/share/examples/rv-predict-c/c11``.  First, ``$
rvpc dot-product.c`` creates an instrumented version of a multithreaded
program that computes a dot product.  Second, ``$ ./a.out`` runs the
program and analyzes its run-time behavior.  The results look like this:

.. code-block:: none

        Thread 0 did  0 to 10:  mysum=10.000000 global sum=10.000000
        Thread 1 did 10 to 20:  mysum=10.000000 global sum=20.000000
        Thread 2 did 20 to 30:  mysum=10.000000 global sum=30.000000
        Sum =  30.000000 
        -- Window 1 --
        -- Window 2 --
        Data race on dotstr.sum at dot-product.c:
            Write in thread 3
              > in dotprod at .../c11/dot-product.c:62
            Thread 3 created by thread 1
                in main at .../c11/dot-product.c:109

            Read in thread 2
              > in dotprod at .../c11/dot-product.c:64
            Thread 2 created by thread 1
                in main at .../c11/dot-product.c:109


        Data race on dotstr.sum at dot-product.c:
            Read in thread 3
              > in dotprod at .../c11/dot-product.c:62
            Thread 3 created by thread 1
                in main at .../c11/dot-product.c:109

            Write in thread 2
              > in dotprod at .../c11/dot-product.c:62
            Thread 2 created by thread 1
                in main at .../c11/dot-product.c:109


First, note that merely running the program does not reveal a data race,
because the output and the final result are as expected.  However,
RV-Predict correctly *predicts* two data races.  The first report
describes the case where there can be a concurrent write at line 62,
and a concurrent read in the ``printf`` statement ending at line 64:

.. code-block:: c
	:number-lines: 37

		printf("Thread %ld did %2d to %2d:  mysum=%f global sum=%f\n",
		    offset, start, end, mysum, dotstr.sum);

The second report concerns line 62, ``dotstr.sum += mysum;``, where a
data race occurs because two threads concurrently read and write the
shared variable ``dotstr.sum``.

This example also showcases the maximality and predictive power of our
approach. In particular, consider the results produced for the same
program by the widely used LLVM ThreadSanitizer tool.

.. code-block:: none

        Thread 0 did  0 to 10:  mysum=10.000000 global sum=10.000000
        ==================
        WARNING: ThreadSanitizer: data race (pid=6206)
          Write of size 4 at 0x0000014ace50 by thread T2:
            #0 dotprod /home/dyoung/share/examples/rv-predict-c/c11/dot-product.c:62:13 (dot-product+0x0000004a237d)

          Previous write of size 4 at 0x0000014ace50 by thread T1:
            #0 dotprod /home/dyoung/share/examples/rv-predict-c/c11/dot-product.c:62:13 (dot-product+0x0000004a237d)

          Location is global 'dotstr' of size 24 at 0x0000014ace40 (dot-product+0x0000014ace50)

          Thread T2 (tid=6209, running) created by main thread at:
            #0 pthread_create <null> (dot-product+0x000000422236)
            #1 main /home/dyoung/share/examples/rv-predict-c/c11/dot-product.c:109:3 (dot-product+0x0000004a2128)

          Thread T1 (tid=6208, finished) created by main thread at:
            #0 pthread_create <null> (dot-product+0x000000422236)
            #1 main /home/dyoung/share/examples/rv-predict-c/c11/dot-product.c:109:3 (dot-product+0x0000004a2128)

        SUMMARY: ThreadSanitizer: data race /home/dyoung/share/examples/rv-predict-c/c11/dot-product.c:62:13 in dotprod
        ==================
        Thread 1 did 10 to 20:  mysum=10.000000 global sum=20.000000
        Thread 2 did 20 to 30:  mysum=10.000000 global sum=30.000000
        Sum =  30.000000 
        ThreadSanitizer: reported 1 warnings

Note that ThreadSanitizer reports only one data race, specifically, a case
where there are two concurrent writes to ``dotstr.sum``.  RV-Predict/C
predicts that the program can read and write ``dotstr.sum`` concurrently.
ThreadSanitizer misses the race between lines 62 and 64 entirely.

Furthermore, consider Helgrind, another widely used tool for detecting
concurrency bugs that is part of the Valgrind toolset. The result of
Helgrind analysis is shown below.

.. code-block:: none

  Thread 0 did  0 to 10:  mysum=10.000000 global sum=10.000000
  ==17736== ---Thread-Announcement------------------------------------------
  ==17736== 
  ==17736== Thread #3 was created
  ==17736==    at 0x516439E: clone (clone.S:74)
  ==17736==    by 0x4E46149: create_thread (createthread.c:102)
  ==17736==    by 0x4E47E83: pthread_create@@GLIBC_2.2.5 (pthread_create.c:679)
  ==17736==    by 0x4C34BB7: ??? (in /usr/lib/valgrind/vgpreload_helgrind-amd64-linux.so)
  ==17736==    by 0x40081B: main (dot-product.c:109)
  ==17736== 
  ==17736== ---Thread-Announcement------------------------------------------
  ==17736== 
  ==17736== Thread #2 was created
  ==17736==    at 0x516439E: clone (clone.S:74)
  ==17736==    by 0x4E46149: create_thread (createthread.c:102)
  ==17736==    by 0x4E47E83: pthread_create@@GLIBC_2.2.5 (pthread_create.c:679)
  ==17736==    by 0x4C34BB7: ??? (in /usr/lib/valgrind/vgpreload_helgrind-amd64-linux.so)
  ==17736==    by 0x40081B: main (dot-product.c:109)
  ==17736== 
  ==17736== ----------------------------------------------------------------
  ==17736== 
  ==17736== Possible data race during read of size 4 at 0x601080 by thread #3
  ==17736== Locks held: none
  ==17736==    at 0x40095B: dotprod (dot-product.c:62)
  ==17736==    by 0x4C34DB6: ??? (in /usr/lib/valgrind/vgpreload_helgrind-amd64-linux.so)
  ==17736==    by 0x4E476B9: start_thread (pthread_create.c:333)
  ==17736== 
  ==17736== This conflicts with a previous write of size 4 by thread #2
  ==17736== Locks held: none
  ==17736==    at 0x400964: dotprod (dot-product.c:62)
  ==17736==    by 0x4C34DB6: ??? (in /usr/lib/valgrind/vgpreload_helgrind-amd64-linux.so)
  ==17736==    by 0x4E476B9: start_thread (pthread_create.c:333)
  ==17736==  Address 0x601080 is 16 bytes inside data symbol "dotstr"
  ==17736== 
  ==17736== ----------------------------------------------------------------
  ==17736== 
  ==17736== Possible data race during write of size 4 at 0x601080 by thread #3
  ==17736== Locks held: none
  ==17736==    at 0x400964: dotprod (dot-product.c:62)
  ==17736==    by 0x4C34DB6: ??? (in /usr/lib/valgrind/vgpreload_helgrind-amd64-linux.so)
  ==17736==    by 0x4E476B9: start_thread (pthread_create.c:333)
  ==17736== 
  ==17736== This conflicts with a previous write of size 4 by thread #2
  ==17736== Locks held: none
  ==17736==    at 0x400964: dotprod (dot-product.c:62)
  ==17736==    by 0x4C34DB6: ??? (in /usr/lib/valgrind/vgpreload_helgrind-amd64-linux.so)
  ==17736==    by 0x4E476B9: start_thread (pthread_create.c:333)
  ==17736==  Address 0x601080 is 16 bytes inside data symbol "dotstr"
  ==17736== 
  Thread 1 did 10 to 20:  mysum=10.000000 global sum=20.000000
  Thread 2 did 20 to 30:  mysum=10.000000 global sum=30.000000
  Sum =  30.000000 

Helgrind is able to detect two data races related to concurrent writes or
a concurrent read and a concurrent write at line 62, but not is not able
to predict a concurrent write at line 62 and a concurrent read at line 63.

2. Simple State Machine
-----------------------

Consider the following example implementing a simple state machine. 

.. code-block:: c

  #include <pthread.h>
  #include <sched.h>
  #include <stdbool.h>
  
  pthread_mutex_t l = PTHREAD_MUTEX_INITIALIZER;
  bool ready = false;
  typedef enum { STOP, INIT, START } state_t;
  state_t state = STOP;
  
  void *
  init(void *arg)
  {
  	pthread_mutex_lock(&l);
  	ready = true;
  	pthread_mutex_unlock(&l);
  	state = INIT;
  	pthread_mutex_lock(&l);
  	ready = true;
  	pthread_mutex_unlock(&l);
  	return NULL;
  }
  
  void *
  start(void *arg)
  {
  	sched_yield();
  	pthread_mutex_lock(&l);
  	if (ready && state == INIT) {
  		state = START;
  	}
  	pthread_mutex_unlock(&l);
  	return NULL;
  }
  
  void *
  stop(void *arg)
  {
  	pthread_mutex_lock(&l);
  	ready = false;
  	state = STOP;
  	pthread_mutex_unlock(&l);
  	return NULL;
  }
  
  int
  main()
  {
  	pthread_t t1, t2, t3;
  	pthread_create(&t1, NULL, init, NULL);
  	pthread_create(&t2, NULL, start, NULL);
  	pthread_create(&t3, NULL, stop, NULL);
  	pthread_join(t1, NULL);
  	pthread_join(t2, NULL);
  	pthread_join(t3, NULL);
  	return 0;
  }

(For full source see
``/usr/share/examples/rv-predict-c/c11/simple-state-machine.c``.)
This program implements a state machine with three states.  Each thread
models some state machine transitions. The developers seem to have
devised a reasonable locking policy that appears to protect shared
resources.  This class of program is hard to test, since there are many
valid observable behaviors.  One of the previously mentioned tools,
ThreadSanitizer or Helgrind, can be used to increase confidence in
the correctness of the program.  There are three subtle data races in
the program.  RV-Predict/C finds them all.  Neither ThreadSanitizer nor
Helgrind report any problems in it.

Compile and run the program as shown below:

.. code-block:: none

    rvpc simple-state-machine.c
    ./a.out

The results of analysis will be:

.. code-block:: none

  Data race on state at simple-state-machine.c:
      Write in thread 2
        > in init at .../simple-state-machine.c:19
      Thread 2 created by thread 1
        > in main at .../simple-state-machine.c:52
  
      Write in thread 3 holding lock l at simple-state-machine.c
        > in start at .../simple-state-machine.c:32
        - locked l at simple-state-machine.c start at .../simple-state-machine.c:30
      Thread 3 created by thread 1
        > in main at .../simple-state-machine.c:53
  
  
      Undefined behavior (UB-CEER5):
          see C11 section 5.1.2.4:25 http://rvdoc.org/C11/5.1.2.4
          see C11 section J.2:1 item 5 http://rvdoc.org/C11/J.2
          see CERT-C section MSC15-C http://rvdoc.org/CERT-C/MSC15-C
          see MISRA-C section 8.1:3 http://rvdoc.org/MISRA-C/8.1

The first data race can effectively invert the state from START to INIT.

.. code-block:: none

  Data race on state at simple-state-machine.c:
      Write in thread 2
        > in init at .../simple-state-machine.c:19
      Thread 2 created by thread 1
        > in main at .../simple-state-machine.c:52
  
      Write in thread 4 holding lock l at simple-state-machine.c
        > in stop at .../simple-state-machine.c:43
        - locked l at simple-state-machine.c stop at .../simple-state-machine.c:41
      Thread 4 created by thread 1
        > in main at .../simple-state-machine.c:54
  
  
      Undefined behavior (UB-CEER5):
          see C11 section 5.1.2.4:25 http://rvdoc.org/C11/5.1.2.4
          see C11 section J.2:1 item 5 http://rvdoc.org/C11/J.2
          see CERT-C section MSC15-C http://rvdoc.org/CERT-C/MSC15-C
          see MISRA-C section 8.1:3 http://rvdoc.org/MISRA-C/8.1

The second data race may be particularly dangerous, because there
are concurrent writes of INIT and STOP to the state variable, which
effectively means that the program could start to enter the START state
while there were critical reasons to STOP.

.. code-block:: none

  Data race on state at simple-state-machine.c:
      Write in thread 2
        > in init at .../simple-state-machine.c:19
      Thread 2 created by thread 1
        > in main at .../simple-state-machine.c:52
  
      Read in thread 3 holding lock l at simple-state-machine.c
        > in start at .../simple-state-machine.c:31:21
        - locked l at simple-state-machine.c start at .../simple-state-machine.c:30
      Thread 3 created by thread 1
        > in main at .../simple-state-machine.c:53
  
  
      Undefined behavior (UB-CEER4):
          see C11 section 5.1.2.4:25 http://rvdoc.org/C11/5.1.2.4
          see C11 section J.2:1 item 5 http://rvdoc.org/C11/J.2
          see CERT-C section MSC15-C http://rvdoc.org/CERT-C/MSC15-C
          see MISRA-C section 8.1:3 http://rvdoc.org/MISRA-C/8.1
  
The last data race is due to a write at line 19: ``state = INIT;``,
while concurrently reading the current value of the state variable. This
data race might lead to a behavior where the START state is not reached.

In summary, this simple program demonstrates that the state-of-the-art
tools can be inadequate to detect subtle data races with possibly dire
consequences, while RV-Predict/C can clearly identify all of the data
races.

3. Double-checked Locking
-------------------------

Suppose you have a shared resource (e.g.shared a database connection or a large allocation a
big chunk of of memory) that is expensive to construct, so it is only done when necessary. 
A common idiom used in such cases is known as `double-checked locking` pattern. 
The basic idea is that the pointer is first read without acquiring the lock, and the lock
is acquired only if the pointer is NULL. The pointer is then checked again once the lock has
been acquired in case another threads has done the initialization between the first check
and this thread acquiring a lock. 

For full source see examples/rv-predict-c/c11/double-checked-locking.c.

.. code-block:: c

  #include <pthread.h>
  #include <stdatomic.h>
  #include <stdio.h>
  #include <stdlib.h>
  #include "nbcompat.h"
  
  typedef struct _resource {
          void (*do_something)(void);
  } resource_t;
  
  static void
  something(void)
  {
          printf("something\n");
  }
  
  resource_t * volatile resource_ptr = NULL;
  pthread_mutex_t resource_mutex = PTHREAD_MUTEX_INITIALIZER;
  
  void *foo(void *arg __unused)
  {
          if (resource_ptr == NULL) {
                  pthread_mutex_lock(&resource_mutex);
                  if (resource_ptr == NULL) {
                          resource_t *r = malloc(sizeof(*r));
                          r->do_something = something;
                          resource_ptr = r;
                  }
                  (*resource_ptr->do_something)();
                  pthread_mutex_unlock(&resource_mutex);
          }
          return NULL;
  }
  
  int
  main(void)
  {
          pthread_t t1, t2;
          pthread_create(&t1, NULL, foo, NULL);
          pthread_create(&t2, NULL, foo, NULL);
  
          pthread_join(t1, NULL);
          pthread_join(t2, NULL);
  }

However, this pattern has become infamous because it has potential for
a nasty race condition.  As shown below, RV-Predict/C detect the race
condition. Specifically, the data race occurs because the read outside
the lock is not synchronized with the write done by the thread inside the
lock. The race condition includes the pointer and the object pointed to:
even if a thread sees the pointer written by another thread, it might
not see the newly created instance of ``some_resource``, resulting in
the call to ``do_something()`` operating on incorrect values.

.. code-block:: none

  Data race on resource_ptr at double-checked-locking.c:
      Read in thread 3
        > in foo at .../double-checked-locking.c:22:19
      Thread 3 created by thread 1
        > in main at .../double-checked-locking.c:40
  
      Write in thread 2 holding lock resource_mutex at double-checked-locking.c
        > in foo at .../double-checked-locking.c:27
        - locked resource_mutex at double-checked-locking.c foo at .../double-checked-locking.c:23
      Thread 2 created by thread 1
        > in main at .../double-checked-locking.c:39

4. Broken Spinnning Loop
------------------------

Sometimes we want to synchronize multiple threads based on whether some condition has been met. 
And it is a common pattern to use a while loop that repeatedly checks that condition:

.. code-block:: c

  #include <err.h>
  #include <pthread.h>
  #include <sched.h>
  #include <stdbool.h>
  #include <stdlib.h>
  
  bool condition = false;
  int sharedVar;
  
  void *
  thread1(void *arg)
  {
          sharedVar = 1;
          condition = true;
          return NULL;
  }
  
  void *
  thread2(void *arg)
  {
          while (!condition) {
                  sched_yield();
          }
          if (sharedVar != 1) {
                  errx(EXIT_FAILURE, "How is this possible!?");
          }
          return NULL;
  }
  
  int
  main(void)
  {
          pthread_t t1, t2;
  
          pthread_create(&t1, NULL, thread1, NULL);
          pthread_create(&t2, NULL, thread2, NULL);
  
          pthread_join(t1, NULL);
          pthread_join(t2, NULL);
          return 0;
  }

As shown below, RV-Predict/C detect the data race on ``condition`` variable. 

.. code-block:: none

  Data race on condition at spinning-loop.c:
      Write in thread 2
        > in thread1 at .../spinning-loop.c:18
      Thread 2 created by thread 1
        > in main at .../spinning-loop.c:39
  
      Read in thread 3
        > in thread2 at .../spinning-loop.c:25:9
      Thread 3 created by thread 1
        > in main at .../spinning-loop.c:40

