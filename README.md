#Short Description

RV-Predict is the only dynamic data race detector that is both sound and 
maximal. 
*Dynamic* means that it executes the program in order to extract an execution 
trace to analyze. *Sound* means that it only reports races which are real (i.e., 
no false positives). And *maximal* means that it finds all the races that can be 
found by any other sound race detector analyzing the same execution trace. The 
technology underlying RV-Predict is best explained in this
[PLDI'14 paper](http://dx.doi.org/10.1145/2594291.2594315). 

# Prerequisites

RV-Predict requires Java Runtime Environment 1.7 or higher. 

Moreover, RV-Predict relies on an SMT solver with support for the 
SMT-LIB v1.2 language and model generation for solving constraints. Please install such an SMT solver prior to installing RV-Predict.  RV-Predict uses [z3](http://z3.codeplex.com) with the `-smt` option by default,
but this behavior can be altered by the user.  Please see the options below.

# Installation

Download the installer from the
[RV-Predict website](http://runtimeverification.com/predict/rv-predict-install.jar)
and execute 

    java -jar rv-predict-install.jar 
Then follow the installation instructions.  Remember to add the `bin` directory 
under the RV-Predict installation directory to your `PATH` environment variable.

# Running RV-Predict

RV-Predict is designed as a drop-off replacement for the `java` command
line.  It is invoked with `rv-predict.bat` on Windows, and `rv-predict`
on Linux and UNIX platforms.

## Basic Usage

Invoke `rv-predict` on a class as you would invoke the Java interpreter:

    rv-predict [options] class [args...]        #(to predict races in a class), or
    rv-predict [options] -jar jarfile [args...] #(to predict races in an executable jar)
where `[options]` include both RV-Predict and Java specific options.

### Example

Running command:

    rv-predict -cp examples/bin account.Account

Output: 

    ----------------Instrumented execution to record the trace-----------------
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

## Interpreting the results

Upon invoking RV-Predict on a class or a jar file, one should expect a normal
execution of the class/jar (albeit slower, as the execution is logged),
followed by a list of races (if any) that were discovered during the execution.
Although some races might be benign for a particular program, all reported
races could actually occur under a different thread interleaving.  Benign
races can become problematic when the memory model or the platform changes,
so it is good practice to eliminate them from your code anyway.

For the example above, the `Account` example is executed, and what we observe 
in the standard output stream is a normal interaction which exhibits no 
data race, also indicated by the fact that the records match at the end of 
the session.

The analysis performed on the logged trace exhibits 4 violations 
which could have occurred if the thread scheduling would have been different.

A race description usually follows the syntax 

    Race on field <field_name> between:
            <method_name>(<file_name>:<line_no>)
            <method_name>(<file_name>:<line_no>)
Which presents the fully qualified name of the field on which the race occurred
(`<field_name>`) and the two locations in race identified as frames on the
method call stack: fully qualified name of the method (`<method_name>`), file 
containing the location (`<file_name>`) and line number where the unprotected
field access occurred (`<line_no>`).

If the race occurrs between accesses at the same location, the syntax is:

    Race on field <field_name> between two instances of:
            <method_name>(<file_name>:<line_no>)

Finally, if the race is due to an array access, the text `field <field_name>` 
is replaced by `an array access` in the messages above.

If no races are found, then the message `No races found.` is displayed.

## Fine Tuning the Execution

Although the basic usage should be sufficient for most scenarios, 
RV-Predict provides several options to allow advanced users to tune 
the execution, analysis, and the produced output.

### Common options

The list of common options can be obtained by using the `-h` or `--help` 
option when invoking RV-Predict:
 		
    $ rv-predict -h
    Usage: rv-predict [rv_predict_options] [--] [java_options] <java_command_line>
      Common options (use -h -v for a complete list):

          --log                 record execution in given directory (no prediction)

          --predict             run prediction on logs from given directory

      -v, --verbose             generate more verbose output

      -h, --help                print help info

- the `--log` option can used to tell RV-Predict that the execution should be
logged in the given directory and that the prediction phase should be skipped.
- the `--predict` option can used to tell RV-Predict to skip the logging phase,
using the logged trace in the given directory to run the prediction algorithms 
on. When using this option specifying the java command is no longer necessary.
- `--` can be used as a terminator for the RV-Predict options.

### Advanced options

The complete list of RV-Predict options can be obtained by
combining the `-h` and `-v` options when invoking RV-Predict:

    rv-predict -h -v

As this list is always evolving, we refrain from listing all these 
options here.  However, we would like to mention the following:

- the `--solver` option instructs RV-Predict to use a different SMT solver 
command for handling SMT queries. The solver command needs to be such that it 
takes a file containing a formula in the SMT-LIB v1.2 language and produces a 
model if the formula is satisfiable.  
The default value for `--solver` is `z3 -smt`.
- the `--output` option controls how the output of the program being 
analyzed should be handled.  The current options are `yes` for displaying 
the output, `no` for hiding the output, or a `<name>`, for redirecting the standard output to `<name>.out` and the standard error to `<name>.err`.
The default value for `--output` is `yes`.

----------
Additional online documentation can be found on the 
[RV-Predict website](http://runtimeverification.com/predict)
