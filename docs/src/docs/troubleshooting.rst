
Problems running RV-Predict?
----------------------------

We list below some possible issues occurring when using RV-Predict and ways to
address them.  For any unlisted issue you might experience, please use the
`RV Support Center`_.


[Error]  RV-Predict must be on the PATH for prediction to run.
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Problem
  The above error shows up when running RV-Predict

Reason
  RV-Predict requires some DLLs (part of the Visual C++ redistributable package)
  to be on the PATH for the prediction process to run.

Advice
  Follow the advice on screen and add the ``bin`` RV-Predict directory
  to the PATH.

Problem
  If the problem persists, although the ``bin`` directory is on the PATH

Reason
  RV-Predict was installed using a different architecture JVM than the one
  used for running it.

Advice
  Reinstall RV-Predict using the same JVM you intend to run it with.
  If you need using RV-Predict with both 32 bit and  64 bit JVMs,
  try installing it into different locations, using the two JVMs.
  Note though that you might need diffrent PATHs to run each.

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
