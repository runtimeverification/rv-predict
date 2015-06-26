Running Examples
================

RV-Predict comes with a suite of small benchmark examples which can be
found in ``examples/basic.jar``, whose source code is in
``examples/basic-src``.  The file ``examples/basic-examples-list.txt``
lists all runnable classes in ``examples/basic.jar``.
Additionally, a `Spring Framework`_ example (``examples/SpringExample.jar``) is
provided to show how RV-Predict can handle multiple class-loaders, as well
as a more complex example based on the `Apache FtpServer`_
(``examples/ftpserver.jar``).

account.Account
---------------

Short Description
~~~~~~~~~~~~~~~~~

The Account class is a demo for a multi-threaded system which manages accounts
while keeping track of their internal balance.  Because of poor synchronization,
a rare data-race yielding a nondeterministic behavior is possible.

Normal run
~~~~~~~~~~

Below is the output (no race) shown during a normal run:

Running command:

.. code-block:: none

    java -cp examples/basic.jar account.Account

Expected output:

.. code-block:: none

    Bank system started
    loop: 2
    loop: 2
    sum: 256
    sum: -174
    sum: -33
    sum: 76
    ..
    End of the week.
    Bank records = 125, accounts balance = 125.
    Records match.

RV-Predict run
~~~~~~~~~~~~~~

To invoke RV-Predict on the Account class, simply replace
``java`` by ``rv-predict`` on the command line:

.. code-block:: none

    rv-predict -cp examples/basic.jar account.Account

If preferring RV-Predict's agent mode, the similar command would be:

 .. code-block:: none

    java -javaagent:<rvPath>/lib/rv-predict.jar -cp examples/basic.jar account.Account


As previously, a complete execution output of the Account class is generated,
most probably not exhibiting the data-race either.  Nevertheless, this output
is followed by the RV-Predict analysis of the observed execution which shows
that under different thread scheduling multiple races could have been
observed:

.. code-block:: none

    ----------------Instrumented execution to record the trace-----------------
    Log directory: /tmp/rv-predict7274661192308018898
    Finished retransforming preloaded classes.
    Bank system started
    loop: 2
    loop: 2
    sum: 256
    sum: -174
    sum: 76
    sum: -33
    ..
    End of the week.
    Bank records = 125, accounts balance = 125.
    Records match.
    Data race on field account.Account.Bank_Total: {{{
        Concurrent write in thread T10 (locks held: {})
     ---->  at account.Account.Service(Account.java:98)
        T10 is created by T1
            at account.Account.go(Account.java:46)

        Concurrent read in thread T1 (locks held: {})
     ---->  at account.Account.checkResult(Account.java:75)
        T1 is the main thread
    }}}

    Data race on field account.Account.Bank_Total: {{{
        Concurrent write in thread T10 (locks held: {})
     ---->  at account.Account.Service(Account.java:98)
        T10 is created by T1
            at account.Account.go(Account.java:46)
    
        Concurrent read in thread T1 (locks held: {})
     ---->  at account.Account.checkResult(Account.java:76)
        T1 is the main thread
    }}}

    Data race on field account.BankAccount.Balance: {{{
        Concurrent write in thread T10 (locks held: {})
     ---->  at account.Account.Service(Account.java:97)
        T10 is created by T1
            at account.Account.go(Account.java:46)
    
        Concurrent read in thread T1 (locks held: {})
     ---->  at account.Account.go(Account.java:67)
        T1 is the main thread
    }}}
    
    Data race on field account.Account.Bank_Total: {{{
        Concurrent write in thread T10 (locks held: {})
     ---->  at account.Account.Service(Account.java:98)
        T10 is created by T1
            at account.Account.go(Account.java:46)
    
        Concurrent read in thread T11 (locks held: {})
     ---->  at account.Account.Service(Account.java:98)
        T11 is created by T1
            at account.Account.go(Account.java:46)
    }}}


Interpreting the results
------------------------

Upon invoking RV-Predict on a class or a jar file, one should expect a normal
execution of the class/jar (albeit slower, as the execution is logged),
followed by a list of races (if any) that were discovered during the execution.
Although some races might be benign for a particular program, all reported
races could actually occur under a different thread interleaving.  Benign
races can become problematic when the memory model or the platform changes,
so it is good practice to eliminate them from your code anyway.

For the example above, the ``Account`` example is executed, and what we observe
in the standard output stream is a normal interaction which exhibits no
data race, also indicated by the fact that the records match at the end of
the session.

The analysis performed on the logged trace exhibits 4 data-races which could
have occurred if the thread scheduling would have been different.

A race description usually follows the syntax

.. code-block:: none

    Data race on field <field_name>: {{{
        Concurrent <operation> on thread <thread_number> (locks held: {<locks>})
     ---->  at <method_name>(<file_name>:<line_number>)

        Concurrent <operation> on thread <thread_number> (locks held: {<locks>})
     ---->  at <method_name>(<file_name>:<line_number>)
    }}}

which presents the fully qualified name of the field on which the race occurred
(``<field_name>``) and the two racing locations identified as frames on the
method call stack: fully qualified name of the method (``<method_name>``), file
containing the location (``<file_name>``) and line number where the unprotected
field access occurred (``<line_number>``). The description also presents the
type of race (``<operation>``), which can be write-write or read-write, and
provides details about the threads and locks involved (``<thread_number>`` and
``<locks>``). 

Finally, if the race is due to an array access, the text ``field <field_name>``
is replaced by ``an array access`` in the messages above.

If no races are found, then the message ``No races found.`` is displayed. The 
races are logged in the log directory printed at the beginning of the report
(``/tmp/rv-predict7274661192308018898``) in ``report.txt``, and any errors or
stacktraces are recorded in ``debug.log``.

SpringExample.jar
-----------------

Short Description
~~~~~~~~~~~~~~~~~

This example is built by altering the standard "Hello World!" example for the
`Spring Framework`_ to exhibit a multi-threaded race condition which can be
triggered by commuting the order of two synchronization blocks.
This example shows that both ``jar`` files and complex class-loaders are supported.

Normal Run
~~~~~~~~~~

.. code-block:: none

    java -jar examples/SpringExample.jar

    log4j:WARN No appenders could be found for logger (org.springframework.context.support.ClassPathXmlApplicationContext).
    log4j:WARN Please initialize the log4j system properly.
    log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
    Hello ! World
    0

RV-Predict Run
~~~~~~~~~~~~~~


.. code-block:: none

    ----------------Instrumented execution to record the trace-----------------
    Log directory: /tmp/rv-predict3777313530719533961
    Finished retransforming preloaded classes.
    log4j:WARN No appenders could be found for logger (org.springframework.context.support.ClassPathXmlApplicationContext).
    log4j:WARN Please initialize the log4j system properly.
    log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
    Hello ! World
    0
    Data race on field HelloWorld.x: {{{
        Concurrent read in thread T10 (locks held: {})
     ---->  at HelloWorld$MyThread.run(HelloWorld.java:40)
        T10 is created by T1
            at HelloWorld.printHello(HelloWorld.java:19)

        Concurrent write in thread T1 (locks held: {Monitor@57af006c})
     ---->  at HelloWorld.printHello(HelloWorld.java:23)
            - locked Monitor@57af006c at HelloWorld.printHello(HelloWorld.java:21) 
        T1 is the main thread
    }}}


.. _Spring Framework: http://projects.spring.io/spring-framework/
.. _Apache FtpServer: http://mina.apache.org/ftpserver-project/
