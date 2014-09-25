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

As previously, a complete execution output of the Account class is generated,
most probably not exhibiting the data-race either.  Nevertheless, this output
is followed by the RV-Predict analysis of the observed execution which shows
that under different thread scheduling multiple races could have been
observed:

.. code-block:: none

    ----------------Instrumented execution to record the trace-----------------
    Bank system started
    loop: 2
    sum: -174
    loop: 2
    sum: 256
    sum: -33
    sum: 76
    ..
    End of the week.
    Bank records = 125, accounts balance = 125.
    Records match.

    -------------------------Logging phase completed.--------------------------
    Race on field account.BankAccount.Balance between:
            account.Account.go(Account.java:67)
            account.Account.Service(Account.java:97)

    Race on field account.Account.Bank_Total between two instances of:
            account.Account.Service(Account.java:98)

    Race on field account.Account.Bank_Total between:
            account.Account.checkResult(Account.java:75)
            account.Account.Service(Account.java:98)

    Race on field account.Account.Bank_Total between:
            account.Account.checkResult(Account.java:76)
            account.Account.Service(Account.java:98)


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

    Race on field <field_name> between:
            <method_name>(<file_name>:<line_number>)
            <method_name>(<file_name>:<line_number>)

which presents the fully qualified name of the field on which the race occurred
(``<field_name>``) and the two racing locations identified as frames on the
method call stack: fully qualified name of the method (``<method_name>``), file
containing the location (``<file_name>``) and line number where the unprotected
field access occurred (``<line_number>``).

If the race occurs between accesses at the same location, the syntax is:

.. code-block:: none

    Race on field <field_name> between two instances of:
            <method_name>(<file_name>:<line_number>)

Finally, if the race is due to an array access, the text ``field <field_name>``
is replaced by ``an array access`` in the messages above.

If no races are found, then the message ``No races found.`` is displayed.


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
    1

RV-Predict Run
~~~~~~~~~~~~~~


.. code-block:: none

    rv-predict -jar examples/SpringExample.jar

    ----------------Instrumented execution to record the trace-----------------
    log4j:WARN No appenders could be found for logger (org.springframework.context.support.ClassPathXmlApplicationContext).
    log4j:WARN Please initialize the log4j system properly.
    log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
    Hello ! World
    1

    -------------------------Logging phase completed.--------------------------
    Race on field HelloWorld.x between:
            HelloWorld$MyThread.run(HelloWorld.java:40)
            HelloWorld.printHello(HelloWorld.java:23)


.. _Spring Framework: http://projects.spring.io/spring-framework/
.. _Apache FtpServer: http://mina.apache.org/ftpserver-project/
