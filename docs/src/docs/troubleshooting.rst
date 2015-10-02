
Problems running RV-Predict?
----------------------------

We list below some possible issues occurring when using RV-Predict and ways to
address them.  For any unlisted issue you might experience, please use the
`RV Support Center`_.

Program does not seem to terminate
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Problem
  The execution of the program takes too long when run using RV-Predict.

Reason
  It could be due to the overhead required by RV-Predict analysis, or due to a
  deadlock condition triggered by the logged program.

Advice
  You can stop the program at any time and run the prediction phase on the
  already logged trace using the ``--predict`` option with the directory in which
  the trace was logged (printed by RV-Predict when the logging was started).

Stack overflow error
~~~~~~~~~~~~~~~~~~~~

Problem
  I'm getting an unexpected *Stack Overflow* exception and a huge stack
  trace when running my program with RV-Predict.

Reason
  The execution trace to be analyzed is collected by RV-Predict using a Java agent,
  which means that the call stack of the logging module adds on top of the call stack
  of the original application.

Advice
  Try increasing the stack size of the logged program by passing the ``-Xss``
  (as part of the JVM options) to RV-Predict.


.. _RV Support Center: https://runtimeverification.com/support/
