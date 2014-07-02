#Short Description

RV Predict is a sound dynamic predictive race detection tool that detects 
data races through constraint solving. 
A salient feature of RV-Predict is that all reported races are verified to be 
real, i.e., **RV-Predict never reports false alarms**.

#Installation

Download and execute `rv-predict-install.jar` from the 
[RV Predict website](http://runtimeverification.com/predict) 
and follow the installation instructions.

## Prerequisites

RV Predict relies on an SMT solver for solving constraints. Please download and 
install [Z3](http://z3.codeplex.com) prior to running the RV installer. Although it was mostly tested with Z3, RV Predict supports the smtlib1 language 
(currently only for Yices).  Please check the options below.

## Post installation

Add the `bin` directory under the RV Predict installation directory to your 
`PATH` environment variable.

# Running RV Predict

RV Predict is designed as a replacement for the `java` command line. 

## Basic Usage

Invoke rv-predict on a class as you would invoke the Java interpreter

    rv-predict [options] class [args...]        #(to predict races in a class), or
    rv-predict [options] -jar jarfile [args...] #(to predict races in an executable jar)
where [options] include both RV-Predict and java specific options.

### Example

Running command:

    C:\Program Files\RV-Predict> rv-predict -cp benchmarks\bin account.Account
Standard output:

    Bank system started
    loop: 2
    loop: 2
    sum: -174
    sum: 256
    sum: -33
    sum: 76
    ..
    End of the week.
    Bank records = 125, accounts balance = 125.
    Records match.

Standard error:

    Race: account.BankAccount|go([Ljava.lang.String;)V|account.BankAccount.Balance|67 - account.BankAccount|Service(II)V|account.BankAccount.Balance|97
    Race: account.Account|Service(II)V|account.Account.Bank_Total|98 - account.Account|Service(II)V|account.Account.Bank_Total|98
    Race: account.Account|checkResult(I)V|account.Account.Bank_Total|75 - account.Account|Service(II)V|account.Account.Bank_Total|98
    Race: account.Account|checkResult(I)V|account.Account.Bank_Total|76 - account.Account|Service(II)V|account.Account.Bank_Total|98  

## Interpreting the results

Upon invoking RV Predict on a class or a jar file, one should expect a normal 
execution of the class/jar (albeit slower, as the execution is logged), 
followed by a list of races (if any) that were discovered as potential during 
the execution.  Though some races might be benign, all reported races could 
actually occur under a different thread interleaving.

For the example above, the `Account` example is executed, and what we observe 
in the standard output stream is a normal interaction which exhibits no 
datarace, also indicated by the fact that the records match at the end of 
the session.

The standard error stream output shows the results of the analysis performed 
on the logged trace which exhibits 4 possible violations which could have 
occurred if the thread scheduling would have been different.

A race description is introduced by the `Race: ` keyword, followed by the 
two strings identifying the locations in race, separated by ` - `. 
A location descriptor consists of 4 components separated by `|`.  
These components are:

- `account.BankAccount` — the fully qualified name of the class where the
conflict occurred
- `go([Ljava.lang.String;)V` — the signature of the method in which the 
conflict occurred 
- `account.BankAccount.Balance` — the fully qualified name of the field 
involved in the race
- `67` — the line number for this location

Thus, the first race description can be read as follows:
> There is a race between the `Ballance` field of the `account.BankAccount`
> class accessed in method `go` at line `67` and the access of the same field 
> in method `Service` at line `97`.

If no races are found, then the message `No races found.` is appended to the
standard output stream.

## Fine Tuning the Execution

Although the basic usage should be sufficient for most scenarios, 
RV Predict provides several options to allow advanced users to tune 
the execution, analysis, and the produced output.

### Common options

The list of common options can be obtained by using the `-h` or `--help` 
option when invoking RV Predict:
 		
    rv-predict --help
    Usage: rv-predict [rv_predict_options] [java_options] <command_line>
        Common options (use -h -v for a complete list):

        --agent             Run only the logging stage [false]
        --dir               output directory [null]
    -h, --help              print help info [false]
        --java              optional separator for java arguments [false]
        --predict           Run only the prediction stage [false]
        --timeout           rv-predict timeout in seconds [3600]
    -v, --verbose           generate more verbose output [false]


- the `--dir` option can be used to specify the output directory of the tool.
- the `--agent` option can used (usualy in conjunction with the `--dir` option 
to tell RV Predict that the prediction phase should be skipped.
- the `--predict` option can used (usualy in conjunction with the `--dir` option 
to tell RV Predict that the logging phase should be skipped, reusing an already
logged trace to run the prediction algorithms on.
- the `--timeout` option controls the total execution time we allow for the 
prediction phase.
- the `--java` option can be used as the final RV-specific option, to separate
the RV parameters from the java ones.  This is especially useful if the command 
line of the program being run uses arguments with the same syntax as 
the RV Predict ones.

### Advanced options

The complete list of RV Predict options can be obtained by
combining the `-h` and `-v` options when invoking RV Predict:

    rv-predict.bat -h -v

As this list is subject to evolution, we refrain from listing all these 
options here.  However, we would like to mention `--smtlib1` which instructs
RV Predict to format SMT queries in the smtlib1 language and use Yices 
(`yices-smt`) to check them.

----------
Additional online documentation can be found on the 
[RV Predict website](http://runtimeverification.com/predict)
