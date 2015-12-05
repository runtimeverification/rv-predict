Quickstart
==========

Prerequisites
-------------

RV-Predict requires Java Runtime Environment 1.8 or higher.

Installation
------------

Download the installer from the `RV-Predict website`_ and execute it
with ``java -jar <installer>``, following all instructions. Remember
to add the ``bin`` directory under the RV-Predict installation
directory to your ``PATH`` environment variable.

Running RV-Predict
------------------

RV-Predict can be run both from the command line, as a drop in
replacement for the ``java`` command, and as an agent, to ease
integration with IDEs and build management tools like Maven.

In the following, we assume ``${rvPath}`` is the installation directory
for RV-Predict.

Moreover, we will use ``${rvOptions}`` to refer to the `RV-Predict options`_
used for controlling the RV-Predict tool, and ``${jvmOptions}`` to refer to
additional `JVM options`_ which can impact the overall performance
of the tool, both of them detailed below.
Note that for simpler examples, these options can be omitted altogether.

On the command line
~~~~~~~~~~~~~~~~~~~

RV-Predict is invoked as follows:

.. code-block:: none

        rv-predict ${rvOptions} [--] ${jvmOptions} class [args...]
            (to predict races in a class)
    or  rv-predict ${rvOptions} [--] ${jvmOptions} -jar jarfile [args...]
            (to predict races in an executable jar file)

Whenever it might cause confusion, the optional ``--`` can be used as a
terminator for the RV-Predict options.

The ``rv-predict`` script is itself just a wrapper for the Java command:

.. code-block:: none

    java -jar ${rvPath}/rv-predict.jar

and they can be used interchangeably.  The benefit of the script is that
if ${rvPath}/bin is added to the environment ``PATH``, ${rvPath} does not need
to be mentioned at each tool invocation anymore.

As an agent
~~~~~~~~~~~

Running RV-Predict as an agent along with your Java application simply
requires adding the ``-javaagent:${rvPath}/rv-predict.jar`` option to
your Java command line.
Passing options to the agent can be done as standard for agents:
using ``-javaagent:${rvPath}/rv-predict.jar="${rvOptions}"``.

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
              <argLine> ${jvmOptions} -javaagent:${rvPath}/rv-predict.jar="${rvOptions}" </argLine>
          </configuration>
          </plugin>
      ...
      </plugins>
  </build>

Replace ``${surefire-version}`` with the exact surefire plugin version
used by the project (e.g., ``2.16``).

Integration with IDEs
`````````````````````

Generic instructions
  Same options as above need to be added to the VM options of your IDE's
  Run/Debug Configurations.
Eclipse
  From the menu select **Run** -> **Run Configurations** ->
  (then you select the configuration that you are running) ->
  select **Arguments** tab -> enter into the **VM arguments** field

  - ``${jvmOptions} -javaagent:${rvPath}/rv-predict.jar="${rvOptions}"``

IntelliJ IDEA
  From the menu select **Run** -> **Edit Configurations** ->
  (then you select the configuration that you are running) -> enter
  into the **VM options** field

  - ``${jvmOptions} -javaagent:${rvPath}/rv-predict.jar="${rvOptions}"``

Using RV-Predict for all Java apps
``````````````````````````````````

If one wants to run RV-Predict for any invocation of the ``java`` tool,
one can simply update the environment variable ``JAVA_TOOL_OPTIONS``
to include the line

- ``${jvmOptions} -javaagent:${rvPath}/rv-predict.jar="${rvOptions}"``

RV-Predict options
------------------

The RV-Predict options are used for controlling the execution of RV-Predict
either in agent mode or in command-line mode.
The list of common options can be obtained by using the ``-h`` or ``--help``
option when invoking RV-Predict:


.. code-block:: none

    rv-predict -h

    Usage: rv-predict [rv_predict_options] [--] [java_options] <java_command_line>
      Common options (use -h -v for a complete list):

          --base-log-dir     The name of the base directory where RV-Predict
                             creates log directories
                             Default: /tmp

          --log-dirname      The name of the log directory where RV-Predict
                             stores log files

          --include          Comma separated list of packages to include

          --exclude          Comma separated list of packages to exclude

          --window           Window size (must be >= 64)
                             Default: 1000

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
-  the ``--base-log-dir <dir>`` option specifies the path of the directory
   where RV-Predict creates its log directories. This option defaults to
   the current temporary directory on your platform (usually set with the
   environment variable TMPDIR in Linux/Unix, or TMP or TEMP in Windows).
-  the ``--log-dirname <dirname>`` option specifies the name of the log
   directory where RV-Predict stores log files such as traces and metadatas.
   When this option is not specified, RV-Predict will create and use a fresh
   temporary directory.
-  the ``--include`` option tells RV-Predict to include the given packages
   in instrumentation; this option takes precedence over the following
   ``--exclude`` option.
-  the ``--exclude`` option tells RV-Predict to exclude the given packages
   from instrumentation.
-  the ``--window <size>`` (default: ``1000``) option tells RV-Predict to
   find races between events with the largest distance of ``<size>`` in the
   logged trace.  The larger the ``<size>`` is, the more races are expected
   to be detected, and the more time RV-Predict will take.
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

    rv-predict -h -v

As this list of advanced options is continuously evolving, we only list the
more common ones here.  Please feel free to contact us in case the explanations
displayed by invoking the tool are not sufficient:

-  the ``--log`` option tells RV-Predict to log the execution trace but skip
   the prediction phase.
-  the ``--predict <dir>`` option tells RV-Predict to skip the logging phase,
   using the logged trace from the ``<dir>`` directory to run the prediction
   algorithms. When using this option, all non-`RV-Predict options`_ will
   be disregarded.
-  the ``--profile`` option instructs RV-Predict to run in the profiling mode
   which does not perform any deep analysis. It is commonly used to estimate the
   number and distribution of events generated from the instrumented classes.
-  the ``--no-stacks`` option tells RV-Predict to not record call stack events
   that are used to compute stack traces in the race report.

Enhancing prediction power
~~~~~~~~~~~~~~~~~~~~~~~~~~

By default, RV-Predict tries to keep a good balance between efficiency
and prediction power.  Nevertheless, while the default settings were
engineered to work for most common cases, there might be cases where
user input could improve the prediction process.  We provide several
options for advanced users to tune RV-Predict:

#. Window size.  For efficiency reasons, RV-Predict splits the execution
   trace into segments (called windows) of a specified size.  The default
   window size is ``1000``.  Users can alter this size using
   the ``--window`` option, with the intuition that a larger size provides
   better coverage at the expense of increasing the analysis time.
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


JVM options
-----------

As RV-Predict instruments the code at runtime and records sequences of
events in the JVM memory, running RV-Predict on larger applications might
require adjusting the memory limits of the JVM.
For example, here are the initial options passed by our helper script when
invoking RV-Predict:

-  ``-Xss4m`` sets the thread stack size of the JVM to be 4MB,
-  ``-Xms64m`` sets the initial heap size to be 64MB
-  ``-Xmx1g`` sets the maximum heap size to be 1G

In addition, we strongly recommend adding the ``-XX:hashCode=1`` option if
running RV-Predict on larger projects; this significantly reduces the
possibility of false positives due to identity hash code collision.


.. _z3: http://z3.codeplex.com
.. _RV-Predict website: http://runtimeverification.com/predict
