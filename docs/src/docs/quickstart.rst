Quickstart
==========

Prerequisites
-------------

RV-Predict requires Java Runtime Environment 1.8 or higher.

Installation
------------

Download the installer from the `RV-Predict website`_ and execute it
with ``java -jar <installer>`` (unless your browser executes it
automatically upon download), following all instructions.

Running RV-Predict
------------------

RV-Predict's main operation mode is an agent, easing the  integration
with IDEs and build management tools like Maven.  Moreover, it can also be run
as a standalone application, either as a drop in replacement for the ``java``
command, or for offline trace analysis.

In the following, we assume ``<rvPath>`` is the installation directory
for RV-Predict.

As an agent
~~~~~~~~~~~

Running RV-Predict as an agent along with your Java application simply
requires adding the ``-javaagent:<rvPath>/rv-predict.jar`` option to
your Java command line. In addition, we strongly recommend you to
also add the ``-XX:hashCode=1`` option; this significantly reduces
the possibility of false positive due to identity hash code collision.
Passing options to the agent can be done as standard for agents:
using  ``-javaagent:<rvPath>/rv-predict.jar="<rv_predict_options>"``,
where ``<rv_predict_options>`` are RV-Predict options.

Integration with Maven
``````````````````````
For Maven-based projects which have tests, one can simply run ``mvn test``,
after modifying the individual project's ``pom.xml`` to have an element
similar to the following:

::

  <build>
      <plugins>
          ...
          <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>${surefire-version}</version>
          <configuration>
              <argLine>-javaagent:<rvPath>/rv-predict.jar -XX:hashCode=1</argLine>
          </configuration>
          </plugin>
      ...
      </plugins>
  </build>

Replace ``${surefire-version}`` with the exact surefire plugin version
used by the project (e.g., ``2.16``).

Adding the ``-javaagent`` option is the only change
needed to an existing project and tests can still be run with ``mvn test``.
Again, the ``-XX:hashCode=1`` option is optional but highly recommended.

Integration with IDEs
`````````````````````

Generic instructions
  options ``-javaagent`` and ``-XX:hashCode=1`` (optional)
  need to be added to the VM options of your Run/Debug Configurations.
Eclipse
  From the menu select **Run** -> **Run Configurations** ->
  (then you select the configuration that you are running) ->
  select **Arguments** tab -> enter
  ``-javaagent:<rvPath>/rv-predict.jar``
  into the **VM arguments** field.
IntelliJ IDEA
  From the menu select **Run** -> **Edit Configurations** ->
  (then you select the configuration that you are running) -> enter
  ``-javaagent:<rvPath>/rv-predict.jar``
  into the **VM options:** field.

On the command line
~~~~~~~~~~~~~~~~~~~

RV-Predict is invoked as follows:

.. code-block:: none

        java -jar <rv-path>/rv-predict.jar [rv_predict_options] [--] [java_options] class [args...]
            (to predict races in a class)
    or  java -jar <rv-path>/rv-predict.jar [rv_predict_options] [--] [java_options] -jar jarfile [args...]
            (to predict races in an executable jar file)

where ``[rv_predict_options]`` are RV-Predict options and ``[java_options]`` are
normal Java options. Whenever it might cause confusion, the optional ``--`` can
be used as a terminator for the RV-Predict options.

To make it easier to run RV-Predict on the command line, scripts are provided
in the ``<rvPath>/scripts`` directory.

Tuning RV-Predict
-----------------

The list of common options can be obtained by using the ``-h`` or ``--help``
option when invoking RV-Predict:


.. code-block:: none

    java -jar <rv-path>/rv-predict.jar -h

    Usage: rv-predict [rv_predict_options] [--] [java_options] <java_command_line>
      Common options (use -h -v for a complete list):

          --offline          Run prediction offline

          --log              Log execution trace without running prediction

          --predict          Run prediction on logs from the given directory

          --dir-name         The name of the base directory where RV-Predict
                             creates log directories
                             Default:

          --include          Comma separated list of packages to include

          --exclude          Comma separated list of packages to exclude

          --window           Window size (must be >= 64)
                             Default: 1000

          --stacks           Record call stack events and compute stack traces
                             in race report

          --suppress         Suppress race reports on the fields that match
                             the given (comma-separated) list of regular
                             expressions
                             Default:

      -v, --verbose          Generate more verbose output

          --version          Print product version and exit

      -h, --help             Print help info

Explanation:

-  the ``--offline`` option tells RV-Predict to store the logged execution
   trace on disk and only run the prediction algorithm after the application
   terminates.
-  the ``--log`` option tells RV-Predict to log the execution trace but skip
   the prediction phase.
-  the ``--predict <dir>`` option tells RV-Predict to skip the logging phase,
   using the logged trace from the ``<dir>`` directory to run the prediction
   algorithms. When using this option, specifying the java options and java
   command line are no longer necessary.
-  the ``--dir-name <dir>`` option specifies the name of the work directory
   where RV-Predict creates its log directories. For example, if we specify
   ``--dir-name foo`` then the log directory created by RV-Predict would look
   like ``/tmp/foo/rv-predictXXX`` on a linux system.
-  the ``--include`` option tells RV-Predict to include the given packages
   in instrumentation; this option takes precedence over the following
   ``--exclude`` option.
-  the ``--exclude`` option tells RV-Predict to exclude the given packages
   from instrumentation.
-  the ``--window <size>`` (default: ``1000``) option tells RV-Predict to
   find races between events with the largest distance of ``<size>`` in the
   logged trace.  The larger the ``<size>`` is, the more races are expected
   to be detected, and the more time RV-Predict will take.
-  the ``--stacks`` option tells RV-Predict to record call stack events that
   can be used to compute stack traces in the race report.
-  the ``--suppress`` option tells RV-Predict to suppress race reports on
   the fields that match the given regular expression patterns; only used
   when the user is absolutely certain that the data race to be suppressed
   is benign.
-  ``--`` can be used as a terminator for the RV-Predict options.

Advanced options
~~~~~~~~~~~~~~~~

The complete list of RV-Predict options can be obtained by
combining the ``-h`` and ``-v`` options:


.. code-block:: none

    java -jar <rv-path>/rv-predict.jar -h -v

As this list of advanced options is continuously evolving, we only list the
more common ones here.  Please feel free to contact us in case the explanations
displayed by invoking the tool are not sufficient:

-  the ``--profile`` option instructs RV-Predict to run in the profiling mode
   which does not perform any deep analysis. It is commonly used to estimate the
   number and distribution of events generated from the instrumented classes.

Suggested JVM memory tweaks
~~~~~~~~~~~~~~~~~~~~~~~~~~~

As RV-Predict instruments the code at runtime and records sequences of
events in the JVM memory, running RV-Predict on larger applications might
require adjusting the memory limits of the JVM.
For example, here are the initial options passes by our helper script when
invoking RV-Predict:
-Xss4m sets the thread stack size of the JVM to be 4MB,
-Xms64m sets the initial heap size to be 64MB
-Xmx1g sets the maximum heap size to be 1G


Enhancing prediction power
~~~~~~~~~~~~~~~~~~~~~~~~~~

By default, RV-Predict tries to keep a good balance between efficiency
and prediction power.  Nevertheless, while the default settings were
engineered to work for most common cases, there might be cases where
user input could improve the prediction process.  We provide several
options for advanced users to tune RV-Predict:

#. Window size.  For efficiency reasons, RV-Predict splits the execution
   trace into segments (called windows) of a specified size.  The default
   window size is ``1000``;  however, the user can alter this size using
   the ``--window`` option, with the intuition that a larger size provides
   better coverage, at the expense of increasing the analysis time.
#. Excluding packages.  To allow better control over the efficiency,
   RV-Predict provides the option ``--exclude`` to remove certain packages from
   logging.  This option takes a list of package patterns prefixes separated
   by ``,`` and excludes from logging any class matched by one of the patterns.
   The patterns can use ``*`` to match any sequence of characters. Moreover,
   ``*`` is automatically assumed at the end of each pattern (to make sure
   inner classes are excluded together with their parent).
   Please note that excluding packages might affect precision, as events from
   non-logged packages might prevent certain race conditions from occurring.
   Note: in ``bash``-like enviroments, the ``$`` character must be escaped
   as it is used by the shell to introduce environment variables.
#. Including packages.  To give more flexibility to selecting which packages
   to include and exclude, RV-Predict also provides the ``--include`` option
   which is similar to the ``--exclude`` option (comma separated list of
   package patterns), but opposite in effect.


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
  option to RV-Predict.



.. _z3: http://z3.codeplex.com
.. _RV-Predict website: http://runtimeverification.com/predict
.. _RV Support Center: https://runtimeverification.com/support/
