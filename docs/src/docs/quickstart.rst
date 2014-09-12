Quickstart
==========

Prerequisites
-------------

RV-Predict requires Java Runtime Environment 1.7 or higher.

Installation
------------

Download the installer from the `RV-Predict website`_, execute it
and follow the installation instructions.  Remember to add the ``bin``
directory under the RV-Predict installation directory to your ``PATH``
environment variable.

Running RV-Predict
------------------

RV-Predict is designed as a drop-off replacement for the ``java``
command line.  It is invoked as follows:

.. code-block:: none

    rv-predict [options] class [args...]        #(to predict races in a class), or
    rv-predict [options] -jar jarfile [args...] #(to predict races in an executable jar)

where ``[options]`` include both RV-Predict and Java specific options.


Common options
~~~~~~~~~~~~~~

The list of common options can be obtained by using the ``-h`` or ``--help``
option when invoking RV-Predict:


.. code-block:: none

    rv-predict -h

    Usage: rv-predict [rv_predict_options] [--] [java_options] <java_command_line>
      Common options (use -h -v for a complete list):

          --log                 Record execution in given directory (no prediction)

          --predict             Run prediction on logs from given directory

      -v, --verbose             Generate more verbose output

      -h, --help                Print help info

-  the ``--log <dir>`` option can be used to tell RV-Predict that the execution
   should be logged in the ``<dir>`` directory and that the prediction phase
   should be skipped.
-  the ``--predict <dir>`` option can be used to tell RV-Predict to skip the
   logging phase, using the logged trace from the ``<dir>`` directory to run
   the prediction algorithms on.  When using this option, specifying the java
   options and java command line are no longer necessary.
-  ``--`` can be used as a terminator for the RV-Predict options.

Advanced options
~~~~~~~~~~~~~~~~

The complete list of RV-Predict options can be obtained by
combining the ``-h`` and ``-v`` options:


.. code-block:: none

    rv-predict -h -v

As this list of advanced options is continuously evolving, we only list the
more common ones here.  Please feel free to contact us in case the explanations
displayed by ``rv-predict -h -v`` are not sufficient:

-  the ``--solver <cmd>`` option instructs RV-Predict to use a different SMT
   solver command for handling SMT queries.
   The solver command needs to be such that it takes a file containing a formula
   in the SMT-LIB v1.2 language and produces a model if the formula is satisfiable.
   If no solver is specified, RV-Predict will use a bundled version of `z3`_
   with the ``-smt`` option enabled (to specify that SMT-LIB v1 is used).
-  the ``--maxlen <size>`` (default: ``1000``) option instructs RV-Predict to
   find races between events with the largest distance of `size` in the logged
   trace.  The larger the ``size`` is, the more races are expected to be detected,
   and the more time RV-Predict will take.
-  the ``--output [yes|no|<name>]`` (default: ``yes``) option controls
   how the output of the program being analyzed should be handled:

   -  ``yes`` specifies the output should be displayed;
   -  ``no`` says the output should be removed;
   -  a ``<name>`` tells to redirect the standard output to
      ``<name>.out`` and the standard error to ``<name>.err``.

Enhancing the prediction power
~~~~~~~~~~~~~~~~~~~~~~~~~~




To be effective, RV-Predict tries to keep a good balance between efficiency 
and prediction power.  Nevertheless, while the default settings were 
engineered to work for most common cases, there might be cases where 
user input could improve the prediction process.  We provide several 
options for advanced users to tune RV-Predict:

#. Window size.  For efficiency reasons, RV-Predict splits the execution 
   trace into segments (called windows) of a specified size.  The default 
   window size is `1000`;  however, the user can alter this size using 
   the `--maxlen` option, with the intuition that a larger size provides 
   better coverage, at the expense of increasing the analysis time.
#. Excluding packages.  To allow better control over the efficiency, 
   RV-Predict provides the option `--exclude` to remove certain packages from 
   logging.  This option takes a list of package prefixes separated by `,` and 
   excludes from logging any class in a package starting with one of the 
   prefixes.   The default excluded packages are: `java`, 
   `javax`, `sun`, `sunw`, `com.sun`, `com.ibm`, `com.apple`, `apple.awt`, 
   `org.xml`, `org.h2`, and `rvpredict`.
   Please note that excluding packages might affect precision, as events from 
   non-logged packages might prevent certain race conditions from occurring.
#. Including packages.  To give more flexibility to selecting which packages 
   to include and exclude, RV Predict also provides the `--include` option 
   which is similar to the `--exclude` option (comma separated list of 
   package prefixes), but opposite in effect.  
#. Aggressive logging.  Through its `--with-profile` option, RV Predict 
   provides some heuristics to detect and filter out from the log non-shared 
   data accesses.  Although not suitable for smaller applications (as it 
   involves an additional preprocessing step for profiling), it can often bring 
   significant speedups for larger applications, as it drastically reduces the 
   trace size.

.. _z3: http://z3.codeplex.com
.. _RV-Predict website: http://runtimeverification.com/predict
