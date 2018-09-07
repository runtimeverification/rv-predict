# RV-Predict Developer Guide

## Configuring a build environment

Some of the Maven targets (e.g., `install`) perform testing in an
LXC container.  On some of RV's development hosts (e.g., rvwork-1),
you must belong to group `lxd` to use the containers.  To make sure
that you belong to the right groups, type

```
groups <your username>
```

If your session started before you were added to the `lxd` group,
then you may need to start a new session.  To see which groups
your session effectively belongs to, type

```
groups
```

On Ubuntu, install the prerequisite packages:

```
sudo apt install -y m4
sudo apt install -y make cmake
sudo apt install -y maven mk-configure
sudo apt install -y opam openjdk-8-jdk-headless
sudo apt install -y pandoc
sudo apt install -y sed
sudo apt install -y devscripts debhelper dh-virtualenv
```

Here are some other programs that must be available:

```
awk
clang+llvm-4.0 with development components (.h, .a/.so files)
grep
lex
libarchive
yacc
```

For clang you can try:

```
sudo apt install -y clang-4.0
sudo update-alternatives --install /usr/bin/clang++ clang++ /usr/lib/llvm-4.0/bin/clang++ 100
sudo update-alternatives --install /usr/bin/clang clang /usr/lib/llvm-4.0/bin/clang 100
```

Configure an object directory if you're going to use one.

At the top level of the RV-Predict sources, create the wrapper
script, `rvpmake`, with which you should build, by running:

```
sh ./setup.sh
```

Make sure that you have `clang 4.0` in your PATH.

*Other notes*:

OPAM packages required by the unified error reporting in errors/:
* Probably NOT needed: zarith mlgmp
* Definitely needed: csv uri ocamlbuild-atdgen ocp-ocamlres
* Maybe needed: uuidm camomile atdgen atdj ctypes ctypes-foreign
* Install with `opam install -y <packages here>`.

## Compiling RV-Predict

```bash
cd rv-predict
rvpmake depend
rvpmake
```

## Building RV-Predict for QNX

Variables `TARGET_CC`, `PREDICT_CC`, and `HOST_CC` tell what compiler
to use to build binaries for the target platform, target binaries with
Predict instrumentation, and host binaries, respectively.

At the QNX port's current stage of development, it's very important to
*clean* your directory before building for QNX.

```bash
cd rv-predict
rvpmake cleandir
rvpmake PREDICT_CC=qrvpc TARGET_CC=qclang HOST_CC=clang OS=QNX
```

## Creating the RV-Predict distribution

```bash
cd rv-predict
mvn package
```

The distribution is packaged at `target/release/rv-predict`

Below are a few useful options:

* `-DskipDocs` to skip generating documentation
* `-DskipTests`  to skip tests (esp. good for mvn install, as it skips the lengthy failsafe tests)
* `-Dobfuscate` if one want to run obfuscation as part of the process (disabled by default)

## Installing to $HOME

```bash
cd rv-predict
rvpmake depend
rvpmake PREFIX=$HOME install
```

## Examining RV-Predict/C runtime statistics

If you set `RVP_INFO_ATEXIT` to `yes`, then the RV-Predict/C runtime
will print some statistics about its performance when an instrumented
program exits:

```
t1: ring sleeps 0 spins 0 i-ring spins 0
joined threads: ring sleeps 103 spins 0 i-ring spins 0
```

Those statistics tell us that thread 1 (`t1`), which was still running
when the program exited, had slept 0 times waiting for an event ring to
empty, it had spun in its busy-wait loop 0 times waiting for an event
ring to empty, and it had spun 0 times waiting to start new rings when
interrupts occurred.

(The enters a busy-wait loop instead of sleeping when it is running in
an interrupt/signal context, where sleeping is not allowed.)

The `joined threads` statistics are the sum of statistics for threads
that were previously joined: in total, joined threads slept 103 times
while they waited for their rings to empty.

Sleeps/spins indicate that a thread fills its event rings faster than the
serialization thread can empty them.  That may mean that more buffering is
needed (i.e., the rings should be longer), that serialization is too slow,
or that online analysis cannot keep up with the rate of event production.

It's also possible to get an instrumented program to write the statistics
to its standard error stream by sending it a SIGPWR signal.

## Debugging RV-Predict/C process supervision

RV-Predict/C ordinarily forks a new process that runs the user's
application code while RV-Predict/C either waits for the application
to finish or else performs "online" data-race analysis.  You can
see various messages printed to the standard error stream if you set
`RVP_DEBUG_SUPERVISOR` to `yes` in the environment.

## Creating the installer for RV-Predict

```bash
# Installer for RV-Predict/Java
cd rv-predict/installer
mvn clean package # the installer will be placed in `installer/target`

# Installer for RV-Predict/C
cd rv-predict/c-installer
mvn clean package  # the installer will be placed in `c-installer/target`
```

### Modifying rv-install

The installer for rv-predict is based on [rv-install](https://github.com/runtimeverification/rv-install). It is included in `installer/pom.xml` and `c-installer/pom.xml`. 

Sometimes, you may want to modify `rv-install`, then build the installer.
To do this, run the following commands:

```bash
# remove `rv-install` local cache.
rm -rf ~/.m2/repository/com/runtimeverification/install/;

cd rv-install

mvn clean install
```

Then you can start building the installer for RV-Predict (see the previous
section).

## Deploying installer

1. Generate a new SSH key and add it to your GitHub account.
    * [How to generate SSH key](https://help.github.com/articles/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent/)
    * [How to add SSH key to GitHub account](https://help.github.com/articles/adding-a-new-ssh-key-to-your-github-account/)

2. Configure `~/.m2/settings.xml` to make sure that your computer can connect to company server. Ask the administrator for `username` and `password`.  

```xml
<!-- replace `***` with real username and password  -->
<settings>
  <servers>
    <server>
      <id>runtime-verification</id>
      <username>***</username>
      <password>***</password>
    </server>
    <server>
      <id>runtime-verification.snapshots</id>
      <username>***</username>
      <password>***</password>
    </server>
    <server>
      <id>rv-site</id>
      <username>***</username>
      <password>***</password>
    </server>
  </servers>
</settings>
```

3. Add `ftp.runtimeverification.com` to your `known_hosts` file.

Advice on the web suggests adding a remote host's public key to
`known_hosts` like this:

```bash
ssh-keyscan -t rsa ftp.runtimeverification.com >> ~/.ssh/known_hosts
```

The `ssh-keyscan` manual page warns that by adding keys in this way,
you are susceptible to man-in-the-middle (MITM) attacks.  After adding a
key to your `known_hosts` in that way, always use a trustworthy secondary
source to verify a host key.

For example, examine the fingerprint of the key added by `ssh-keyscan`
by running this command:

```bash
ssh-keygen -l -F ftp.runtimeverification.com -f ~/.ssh/known_hosts
```

If you are confident that this manual has not been tampered with, then you
can verify the `ftp.runtimeverification.com` host key by comparing the
fingerprint printed by `ssh-keygen` with one of these fingerprints---it
should match one:

```
MD5 fingerprint:       86:0d:6b:d3:91:95:f0:45:b6:26:7c:13:81:65:a6:f1
SHA256 fingerprint:    4kbAX2bLe5oHofL0sUqyPDuaAiKgt0JGXTodWiVnGdM
```

If there is no match, then immediately delete the key from your
`known_hosts` file.

4. Deploy installer:

First you will need to install credentials in `$HOME/.m2/settings.xml` for AWS
and the `scp`ing to ftp.runtimeverification.com.  Then run:

```bash
./mvn-release.sh
```

That will build and upload a Debian package (`.deb`) for RV-Predict/C,
the GUI installers (`.jar`) for RV-Predict/C and RV-Predict/Java,
and documentation to the website, increase the version number, tag the
release sources, and add the new release to `debian/changelog`.

You should commit the changes to `debian/changelog` right away.

Without arguments, `mvn-release.sh` increases the number after the
second decimal point (the "teeny" version).  To make a release under a
particular version number---for example, 1.9---run this:

```bash
./mvn-release.sh -DreleaseVersion=1.9 -DdevelopmentVersion=1.9.1-SNAPSHOT
```

The `-DdevelopmentVersion=1.9.1-SNAPSHOT` argument tells where to
start the "teeny" releases.

## Java

### Idea setup

1. Download Intellij Idea (the Community version is free) from
   https://www.jetbrains.com/idea/ and follow the installation instructions
   on the download page. Note that clicking the 'Download' button takes you to
   a page that does not have those instructions, so you'll have to go back to
   see them. At the time of writing this, you just have to unpack the archive
   to a convenient place and run `<unpack-place>/bin/idea.sh`
1. Successfully compile the entire rv-predict project with rvpmake.
1. Open a new project.
   ```File -> Open ... -> select the rv-predict root directory -> Ok```
1. Run all the tests.

   ```Right hand pane -> right click the 'jar' subproject -> Run all tests```

   You'll likely get some
   `java.lang.RuntimeException: java.io.IOException: Stream closed`
   errors that you need to fix, see the next point.
1. Run `mvn package` to put the z3 libraries in the place where Idea expects
   them to be. It usually does not matter if the `mvn package` command fails.
1. Re-run the tests to make sure that they pass now.
1. Enjoy. `Ctrl-b` takes you to the definition, `Ctrl-n` searches for classes,
   `Alt-enter` is a very powerful wizard for fixing errors and warnings.

### Adding a new event type

1. The starting point will be the `CompactEventReader` class, so open that file
   (ctrl-n } write part of the class name and select it).

   Add a new entry in the enum. The enum value takes two arguments, the
   field number and an object that can read that field.

   You should decide if you want to use an existing reader or you want a new
   one. In the following I'll assume that you need a new one.

2. Readers live in the `com.runtimeverification.rvpredict.log.compact.readers`
   package, so you should add a new one there. Most of them look almost
   identical, so you can use an example, but most likely you will need to:

   1. Make a `TraceElement` class in your reader that extends
      `ReadableAggregateData`. This will contain information
      on how to parse the trace data. It usually contains some fields
      (see the `com.runtimeverification.rvpredict.log.compact.datatypes`
      package to see what's available) and a constructor which initializes them
      and then calls `setData` to tell the parent class what to parse.

      *If you only need to parse a single field, you can skip the
      `TraceElement` class and use the field directly, see `BlockSignalsReader`
      as an example*

   1. Make a `createReader` function that returns a `CompactEventReader.Reader`.
      This function usually creates a `SimpleDataReader<>` with two callbacks:
      * one produces objects which can parse the trace data (`TraceElement`).
      * one that takes a `TraceElement` object and some additional configuration
        and produces the internal representation of the event. This one should
        call a function on the provided `compactEventFactory` to create the
        event. It should pass the actual data needed to construct the object,
        not the `TraceElement` representation.

      *The latter callback returns a list of events, so you can just return an
      empty list if you just want to skip the event, see the `jump` event
      implementation*

   1. Hook this reader into the `CompactEventReader` enum if you didn't do that
      already.

1. Add a function to the `CompactEventFactory` class that creates your event(s)
   (or reuse an existing one). You have to create a new `CompactEvent` object,
   overriding whatever functions your new event type needs. Don't forget
   to override the `toString` method so that you'll be able to debug stuff
   later. If you need new functionality, see the point below.

1. The event hierarchy is `CompactEvent` and `Event` extend `ReadonlyEvent`,
   which extends `ReadonlyEventInterface`.

   * `ReadonlyEventInterface` is what the backend code uses, except when
     reading the data / creating it from instrumentation. If you want your
     new event to have functionality that does not fit in the existing methods,
     you should probably add it here.

   * `ReadonlyEvent` implements the event functionality that can be computed
     from the event data (e.g. see `ReadonlyEvent.isLock`).

   * `CompactEvent` represents a `C` event, while `Event` is a `Java` event.
     Note that `CompactEvent` implements all the `ReadonlyEventInterface`
     methods by throwing an exception. This allows us to easily implement only
     what we need when creating an event, while not allowing us to use methods
     which we didn't override by mistake.

### Race detection

Race detection works something like this:
  * Something (the java instrumentation or the trace reader) gets a window's
    worth of data and puts it in a `Trace` object.
    * When reading data, the `TraceCache.getTraceWindow` reads the raw data
      and sends it to `TraceState.initNextTraceWindow`
    * `TraceState.initNextTraceWindow` will call `processWindow` which, among
      other things, initializes the trace producers, then it initializes and
      returns the trace.
  * `MaximalRaceDetector.run` receives a a window's worth of events in the
    `Trace` object. It calls `computeUnknownRaceSuspects` to find out the
    possible races for this window, then it will create a new
    `MaximalCausalModel`, calling `checkRaceSuspects` on it
  * `MaximalCausalModel.create` will call `addConstraints` to initialize the
    constraints to be sent to z3 (read events read the right value,
    thread events cannot be executed before the start event and so on).
  * `MaximalCausalModel.checkRaceSuspects` will filter some of the race
    suspects, then it will start processing the possible races with some degree
    of parallelism. For efficiency purposes, it first tests race suspects with
    some inaccurate, but faster constrains, then it will recheck some of them
    with accurate constraints. As soon as a race is found, it will stop
    checking others which are very similar.

### Constraint generation

Constraints are generated in `MaximalCausalModel.addConstraints`. There are
a few things to keep in mind:

  * Each kind of constraint has a `ConstraintSource`. The `ConstraintSource`
    classes help isolating the various constraint functionality, which makes
    the code easier to follow and makes testing easier.
  * Each source can, in principle, generate two kinds of constraints:
    * fast, but which may detect more races than there are
    * accurate, i.e. detects all races, and only when they exist.

    In practice, most of the sources generate the same constraints in both
    cases.
  * Read-write consistency is handled in a special way, to allow races involving
    only part of the trace

That being said, the current constraint types (except read-write consistency)
are:
  * Happens-before. These make sure that:
    * a thread's events are in the right order.
    * Thread events are executed between the start and end events (if any).
  * Locks: this makes sure that two sections with the same lock can't overlap
  * Signal interrupt locations - makes sure that signals can interrupt only when
    they are enabled
  * Threads at depth 0 - makes sure that thread events run at signal depth 0.
  * Signal depth limit - makes sure that we don't generate check races involving
    signal-interrupts-signal-interrupts-signal... with a depth more than a given
    limit
  * Signal start mask - attempts to compute the signal masks with which signals
    start. This is used by the "signal interrupt locations" constraint to
    decide whether to allow a signal to interrupt another one before any
    explicit signal-enable event on the interrupted signal.
  * Non-overlapping signals - signals interrupting the same thread can't
    have simultaneous events.

Read-write consistency is handled by `getRaceAssertion`, which asserts that
the two events involved in a race are reachable without requiring that the
events are consistent with the previous stuff (`getPhiAbs`) and that they
can occur at the same time (the `INT_EQUAL` part)

`getPhiAbs` says that an event is reachable through a consistent execution,
but it does not require that the event itself is consistent.

`getPhiConc` says that an event is reachable through a consistent execution
and the event itself is consistent.

`getPhiSC` takes all the write events that could occur before a read event
and checks that the read can occur after one of the compatible ones, and that
one of the compatible ones is itself reachable through consistent execution.