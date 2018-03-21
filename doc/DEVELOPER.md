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
sudo apt install m4
sudo apt install make # GNU make
sudo apt install maven mk-configure
sudo apt install opam openjdk-8-jdk-headless
sudo apt install pandoc
sudo apt install sed
sudo apt install devscripts debhelper dh-virtualenv
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
sudo apt install clang-4.0
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
