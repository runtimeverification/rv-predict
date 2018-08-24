# RV-Predict/C User's Manual

RV-Predict/C is a data-race detector for C programs.  It compiles
and runs your program with instrumentation that produces a trace of
variable accesses and other important events.  Then, it analyzes the
trace to predict data races.  RV-Predict/C's data-race predictions
are both *sound* and *maximal*.  *Sound* means that there are no false
positives: RV-Predict/C will not predict a data race if one cannot occur.
*Maximal* means that no technique can predict more data races from the
same program trace.

RV-Predict/C can predict data races in programs that contain a mixture
of threads and UNIX signal handlers.  In this document, the generic
term "sequence of control" refers to a thread, a signal handler, or an
interrupt service routine (ISR).

## Using RV-Predict/C

### Getting started

Make sure that the RV-Predict/C `bin` directory is in your PATH.
Ordinarily, the RV-Predict/C programs will be in `/usr/bin/`, and
`/usr/bin/` will be part of the system's default PATH.

### Compiling and running programs with RV-Predict/C

The first step in using RV-Predict/C to detect data races is to compile
your C program with the RV-Predict/C instrumentation and runtime.
RV-Predict/C provides a script, `rvpc`, that runs your system's existing
copy of `clang`, adding options that run the instrumentation plug-in
and link with the RV-Predict/C runtime.  You can use `rvpc` as a drop-in
replacement for `clang`.  Read the `clang` manual page for details about
how to use `clang`.

`rvpc++` is a drop-in replacement for `clang++`.

Here is a typical `rvpc` invocation that produces an executable binary,
`lpcq`:

```
$ rvpc -o lpcq lpcq.c lpcq_main.c signals.c
```

When you run the binary, it waits for your application code to finish,
then it performs the data-race prediction and writes a report to the
standard error stream:

```
$ ./lpcq
read item 0
read item 1
read item 2
read item 3
read item 4
Data race on [0x00000000020d1fe0]:
    Read in thread 2
      > in lpcq_get at .../c11/lpcq.c:34:2
	in consume at .../c11/lpcq_main.c:104
    Thread 2 created by thread 1
	in main at .../c11/lpcq_main.c:230

    Write in thread 1
      > in lpcq_put at .../c11/lpcq.c:49
	in produce at .../c11/lpcq_main.c:164
	in main at .../c11/lpcq_main.c:243
    Thread 1 is the main thread


Data race on q.tailp at lpcq_main.c;main:
    Read in thread 2
      > in lpcq_get at .../c11/lpcq.c:26:19
	in consume at .../c11/lpcq_main.c:104
    Thread 2 created by thread 1
	in main at .../c11/lpcq_main.c:230

    Write in thread 1
      > in lpcq_put at .../c11/lpcq.c:48
	in produce at .../c11/lpcq_main.c:164
	in main at .../c11/lpcq_main.c:243
    Thread 1 is the main thread
```

If you would prefer to capture your program's RV-Predict/C event trace
to analyze later, then you can do that.  Use the `RVP_TRACE_ONLY`
environment variable to change the program's operating mode, like this:

```
$ RVP_TRACE_ONLY=yes ./lpcq
read item 0
read item 1
read item 2
read item 3
read item 4
```

Your program writes a trace of the program execution to the file
`rvpredict.trace` in the current directory.

You can analyze the trace using `rvpa`, which prints race reports to its
standard error output.  `rvpa` requires one argument, the name of the
binary.  It expects to find `rvpredict.trace` in the current directory.

```
$ rvpa ./lpcq
Data race on [0x00000000020d1fe0]:
    Read in thread 2
      > in lpcq_get at .../c11/lpcq.c:34:2
	in consume at .../c11/lpcq_main.c:104
    Thread 2 created by thread 1
	in main at .../c11/lpcq_main.c:230

    Write in thread 1
      > in lpcq_put at .../c11/lpcq.c:49
	in produce at .../c11/lpcq_main.c:164
	in main at .../c11/lpcq_main.c:243
    Thread 1 is the main thread


Data race on q.tailp at lpcq_main.c;main:
    Read in thread 2
      > in lpcq_get at .../c11/lpcq.c:26:19
	in consume at .../c11/lpcq_main.c:104
    Thread 2 created by thread 1
	in main at .../c11/lpcq_main.c:230

    Write in thread 1
      > in lpcq_put at .../c11/lpcq.c:48
	in produce at .../c11/lpcq_main.c:164
	in main at .../c11/lpcq_main.c:243
    Thread 1 is the main thread
```

`rvpa` accepts options that disable conversions applied
to the raw data-race reports.  `--no-symbol` disables conversion of data
addresses to variable names, and instruction address to line and column
numbers.  When `--no-trim` is given, data-race reports will show stack
frames belonging to the RV-Predict/C runtime as well as stack frames
below each thread's start routine and below `main`.  `--no-signal`
suppresses conversion of signal numbers `S1..Sn` to UNIX symbols like
SIGHUP and SIGSEGV.  `--no-shorten` stops paths from being shortened by
replacing one or more leading components with a single ellipsis, `...`.

### Understanding data-race reports

If RV-Predict/C finds a feasible schedule of your program where two
different sequences of control access the same variable, one of the
accesses is a write, and there is no synchronization between the accesses,
then it reports a data race.

A data-race report begins with the text "Data race on" starting in the
first column.  Subsequent lines of the same report are indented by four
or more spaces.

```
Data race on [0x00007ffd19f94658]:
    Write in thread 2
      > in lpcq_get at .../c11/lpcq.c:36
	in consume at .../c11/lpcq_main.c:104
    Thread 2 created by thread 1
	in main at .../c11/lpcq_main.c:230

    Read in signal SIGALRM
      > in lpcq_put at .../c11/lpcq.c:47:19
	in handler at .../c11/lpcq_main.c:139:1
    Interrupting thread 2
      > in lpcq_get at .../c11/lpcq.c:36
	in consume at .../c11/lpcq_main.c:104
    Thread 2 created by thread 1
	in main at .../c11/lpcq_main.c:230
```

Each report contains two "stanzas" that describe the accesses that
were involved in the data race, and where those accesses occurred.
The first line of each stanza tells what kind of access occurred (a Read
or a Write), and which sequence of control was involved---"thread 2",
for example, or "signal SIGALRM".  The following lines in each stanza
provide additional context, beginning with a stack trace (a '>' marks
the top of stack).  If the access occurred in a signal handler, then
following the stack trace is a clause such as "Interrupting thread 2", and
following that, another stack trace.

One signal can interrupt another.  When one signal interrupts another
and then participates in a data race, its report stanza will contain
one or more lines starting "Interrupting signal", followed by the name
of the interrupted signal and a stack trace.

By default, a data-race report is "symbolized".  That is, instruction
addresses are converted to a filename, line number, and optional
column number of a C/C++ statement or expression, and data addresses
are converted to a variable name.  Addresses that cannot be symbolized
are printed as hexadecimal numbers inside of brackets.  Curly braces,
`{}` are used for instruction addresses.  Square brackets, `[]`, are
used for data addresses.  Thus the report above describes a race on the
data address at 0x00007ffd19f94658.

RV-Predict/C is capable of symbolizing some data addresses that reside
on the stack---e.g., local variables.  It qualifies the name of the
variable with the name of the function where it is declared, and the name
of the source file where the function appears.  In this way, a data-race
report beginning

```
Data race on q.tailp at lpcq_main.c;main:
```

is about the local variable `q` of function `main`.

## Building projects with RV-Predict/C

The RV-Predict/C instrumenting compiler, `rvpc`, is a drop-in
replacement for `clang` in most projects.  It is only necessary to
change the variable(s) that control which compiler is used.  Sometimes
it is necessary to indicate a different linker than the default, too.
Here are examples for a few popular programs.

### CMake

The variables `CMAKE_C_COMPILER` and `CMAKE_CXX_COMPILER` control the
compiler that a CMake project uses.  On the cmake command line, pass
`-DCMAKE_C_COMPILER=rvpc` to compile with RV-Predict/C instrumentation
instead of using the default C compiler.  To use RV-Predict/C instead
of the default C++ compiler, pass `-DCMAKE_CXX_COMPILER=rvpc++`.

### UNIX make(1) utility

Frequently it is possible to override the compiler and linker that a
Makefile uses by setting CC and LD variables on the make(1) command line.
To replace the standard compiler and linker with RV-Predict/C:

```
make CC=rvpc LD=rvpld
```

### GNU autoconf scripts (./configure)

To configure a GNU autoconf project to compile with RV-Predict/C
instrumentation, set CC and LD in the configure script's environment.
For example, in Bourne shell (and bash):

```
CC=rvpc LD=rvpld ./configure
```

In csh/tcsh:

```
env CC=rvpc LD=rvpld ./configure
```

## Package contents

The RV-Predict/C programs ordinarily are installed in `/usr`,
especially when they are installed from the Debian binary package.
(The Debian binary package is installed by the GUI installer.)
`/usr/share/rv-predict-c/` is where files that support RV-Predict/C
are installed.  Those files must be present for RV-Predict/C to work,
but the user does not ordinarily need to do anything with them.

Documentation ordinarily resides at `/usr/share/doc/rv-predict-c/`.
This manual is ordinarily found under that directory, where it is called
USERS-MANUAL.md.

Examples are installed at `/usr/share/examples/rv-predict-c/`.  There are
a variety of examples in C and C++.  Look at the README files under
`examples/rv-predict-c/` for more information.

The most important programs for the everyday RV-Predict/C user are `rvpc`
and `rvpa`:

`rvpc`: a wrapper for `clang` that compiles and links programs with
    RV-Predict/C instrumentation.  When a program built with `rvpc` runs, it
    will ordinarily leave a trace of its activity called `rvpredict.trace`
    in the current working directory.

`rvpc++`: like `rvpc`, only for `clang++`.

`rvpa`: a program for analyzing event traces that were recorded by
    RV-Predict/C instrumentation.  If you ran your program like
    `RVP_TRACE_ONLY=yes <command> <arguments>`, then run `rvpa <command>`
    to see data-race reports. `rvpa` analyzes the `rvpredict.trace` in
    the current directory for data races, reporting them on the standard
    error stream.

Advanced users may find a couple of other programs in the package useful.
Those programs are `rvpsymbolize` and `rvpdump`.  Note that those programs
may not appear in subsequent releases of RV-Predict/C:

`rvpsymbolize`: a filter that takes the name of an ELF executable file,
    `program` as an argument. `rvpsymbolize` copies its standard input
    to its standard output with data and instruction addresses converted
    to symbols: using the DWARF debug information in `program`,
    `rvpsymbolize` replaces each hexadecimal instruction pointer
    enclosed in curly braces, {0x...}, with the corresponding function
    name, file, line number, and optional column number.  It also
    replaces each hexadecimal data address enclosed in square brackets,
    [0x...], with the corresponding symbol name.  For an address inside
    of a structure, `rvpsymbolize` makes a best effort to tell the data
    member's name, and for an address inside of an array, `rvpsymbolize`
    makes a best effort to tell the array index.

`rvpdump`: a program that will read an RV-Predict/C compact trace
    file, `rvpredict.trace`, and print it to standard output in a
    human-readable format.  Either provide the name of the trace file on
    the command line, `rvpdump rvpredict.trace`, or feed the trace into
    standard input, `rvpdump < rvpredict.trace`.

    Use the option `-t symbol-friendly` to produce human-readable output
    that is suitable for symbolization using `rvpsymbolize`.

    A user does not ordinarily need to run `rvpdump`.

## Known issues

RV-Predict/C does not always filter system include files and libraries
from data-race reports.  This is especially apparent in C++ programs.
This will be fixed in the 2.1 release.

## Support

Users are invited to discuss RV-Predict/C with Runtime Verification,
Inc., and members of the public on the
[https://groups.google.com/a/runtimeverification.com/d/forum/predict-users](predict-users) mailing list.

For support and bug reports please visit
[Runtime Verification Support](http://runtimeverification.com/support).

## Further reading

The following papers best describe the technology in RV-Predict/C:

> Jeff Huang, Patrick O'Neil Meredith, and Grigore Rosu. 2014.
> [ "Maximal sound predictive race detection with control flow abstraction." ][msp]
> In Proceedings of the 35th ACM SIGPLAN Conference on Programming Language
> Design and Implementation (PLDI '14).  ACM, New York, NY, USA, 337-348.
> 
> Traian-Florin Serbanuta, Feng Chen, and Grigore Rosu. 2012.
> [ "Maximal Causal Models for Sequentially Consistent Systems." ][mcm]
> In Proceedings of the 3rd International Conference on Runtime Verification
> (RV '12).  LNCS Volume 7687, 2013, pp. 136-150

[msp]: http://dx.doi.org/10.1145/2594291.2594315
[mcm]: http://dx.doi.org/10.1007/978-3-642-35632-2_16
