Running Examples
================

RV-Predict comes with a suite of small benchmark examples which can be
found in ``examples/examples.jar``, whose source code is in
``examples/src``.  Please consult ``examples/README.md`` for more details
on playing with them. We strongly encourage you to also check out our blog
article `Detecting popular data races in Java using RV-Predict`_, execute
all those examples and try to understand the data races occurring in each
and how RV-Predict detects and reports them.
Below we only discuss the example ``account.Account``.

account.Account
---------------

Normal run
~~~~~~~~~~

Below is the output (no race) shown during a normal run:

Running command:

.. code-block:: none

    java -cp examples/examples.jar account.Account

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

    rv-predict -cp examples/examples.jar account.Account

To use RV-Predict's agent mode, the similar command would be:

.. code-block:: none

    java -javaagent:lib/rv-predict.jar -cp examples/examples.jar account.Account


As previously, a complete execution output of the Account class is generated,
most probably not exhibiting the data-race either.  Nevertheless, this output
is followed by the RV-Predict analysis of the observed execution which shows
that under different thread scheduling multiple races could have been
observed:

.. code-block:: none

    ----------------Instrumented execution to record the trace-----------------
    [RV-Predict] Log directory: /tmp/rv-predict7274661192308018898
    [RV-Predict] Finished retransforming preloaded classes.
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
        Concurrent write in thread T12 (locks held: {})
     ---->  at account.Account.Service(Account.java:98)
            at account.BankAccount.Action(BankAccount.java:41)
            at account.BankAccount.run(BankAccount.java:56)
        T12 is created by T1
            at account.Account.go(Account.java:46)

        Concurrent read in thread T13 (locks held: {})
     ---->  at account.Account.Service(Account.java:98)
            at account.BankAccount.Action(BankAccount.java:41)
            at account.BankAccount.run(BankAccount.java:56)
        T13 is created by T1
            at account.Account.go(Account.java:46)
    }}}

    Data race on field account.Account.Bank_Total: {{{
        Concurrent write in thread T12 (locks held: {})
     ---->  at account.Account.Service(Account.java:98)
            at account.BankAccount.Action(BankAccount.java:41)
            at account.BankAccount.run(BankAccount.java:56)
        T12 is created by T1
            at account.Account.go(Account.java:46)

        Concurrent read in thread T1 (locks held: {})
     ---->  at account.Account.checkResult(Account.java:75)
            at account.Account.go(Account.java:70)
            at account.Account.main(Account.java:30)
        T1 is the main thread
    }}}

    Data race on field account.BankAccount.Balance: {{{
        Concurrent write in thread T12 (locks held: {})
     ---->  at account.Account.Service(Account.java:97)
            at account.BankAccount.Action(BankAccount.java:41)
            at account.BankAccount.run(BankAccount.java:56)
        T12 is created by T1
            at account.Account.go(Account.java:46)

        Concurrent read in thread T1 (locks held: {})
     ---->  at account.Account.go(Account.java:67)
            at account.Account.main(Account.java:30)
        T1 is the main thread
    }}}

    Data race on field account.Account.Bank_Total: {{{
        Concurrent write in thread T12 (locks held: {})
     ---->  at account.Account.Service(Account.java:98)
            at account.BankAccount.Action(BankAccount.java:41)
            at account.BankAccount.run(BankAccount.java:56)
        T12 is created by T1
            at account.Account.go(Account.java:46)

        Concurrent read in thread T1 (locks held: {})
     ---->  at account.Account.checkResult(Account.java:76)
            at account.Account.go(Account.java:70)
            at account.Account.main(Account.java:30)
        T1 is the main thread
    }}}


Interpreting the results
------------------------

Upon invoking RV-Predict on a class or a jar file, one should expect a normal
execution of the class/jar (albeit slower, as the execution is traced),
followed by a list of races (if any) that were discovered during the execution.

For the example above, the ``Account`` example is executed, and what we observe
in the standard output stream is a normal interaction which exhibits no
data race, also indicated by the fact that the records match at the end of
the session.

The analysis performed on the logged trace exhibits 4 data-races which could
have occurred if the thread scheduling would have been different.

Take for example the final data-race exhibited in the above example.
RV-Predict shows a race on the ``account.Account.Bank_Total`` field and gives
the stacktraces of the two memory accesses involved in the race.
The first stacktrace describes a write on the field performed by thread ``T12``
(holding no locks) during the execution of the ``account.Account.Service``
method at line ``98`` in the ``Account.java`` file.
Similarly, the second stacktrace describes a read on the same field performed
by thread T1 holding no locks during the execution of method
``account.Account.checkResult`` at line ``76`` of the ``Account.java`` file.
Full stacktraces are included for both accesses, including information about
the creation of the threads.

The stack trace is presented in the same format as in Java to ease integration
with IDEs and stacktrace analysis tools.
If there were locks acquired, the location where the locks were acquired would
have appeared in the stack traces.

Finally, if the race would be due to an array access,
the text ``field <field_name>`` would be replaced by ``an array access``
in the race report.

If no races are found, then the message ``No races found.`` is displayed. The 
races are logged in the log directory printed at the beginning of the report
(``/tmp/rv-predict7274661192308018898``) in ``result.txt``, and any errors or
stacktraces are recorded in ``debug.log``.


More Examples
-------------

Check out more examples at `Detecting popular data races in Java using RV-Predict`_.


.. _Detecting popular data races in Java using RV-Predict : https://runtimeverification.com/blog/?p=58
