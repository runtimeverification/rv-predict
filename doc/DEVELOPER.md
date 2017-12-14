# RV-Predict Developer Guide

## Configuring a build environment

On Ubuntu, install prerequisite packages:

```
sudo apt install m4
sudo apt install make # GNU make
sudo apt install maven mk-configure
sudo apt install opam openjdk-8-jdk-headless
sudo apt install pandoc
sudo apt install sed
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

Configure an object directory if you're going to use one.

At the top level of the RV-Predict sources, configure OCaml by running

```
./opam-setup.sh
```

After it finishes, run the following command to update the environment:

```        
eval $(opam config env)
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
mkcmake depend
mkcmake
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
mkcmake depend
mkcmake PREFIX=$HOME install
```

## Creating the installer for RV-Predict

```bash
# Installer for RV-Predict/Java
cd rv-predict/installer
mvn clean install # the installer will be placed in `installer/target`

# Installer for RV-Predict/C
cd rv-predict/c-installer
mvn clean install  # the installer will be placed in `c-installer/target`
```

### Modifying rv-install 

Installer for rv-predict is based on [rv-install](https://github.com/runtimeverification/rv-install). It is included in `installer/pom.xml` and `c-installer/pom.xml`. 

Sometimes, you may want to modify `rv-install`, then build the installer. 
To do this, run the following commands:

```bash
# remove `rv-install` local cache.  
rm -rf ~/.m2/repository/com/runtimeverification/install/;

cd rv-install 

ant # build rv-install

# install to local cache
# you may need to replace `1.5.2-SNAPSHOT` to the correct version.   
mvn install:install-file -DgroupId=com.runtimeverification.install -DartifactId=rv-install -Dversion=1.5.2-SNAPSHOT -Dpackaging=jar -Dfile=../rv-install/dist/rv-install-1.5.2-SNAPSHOT.jar
```

Then you can start building installer for RV-Predict (See previous section).  

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

3. Add `ftp.runtimeverification.com` to `known_hosts` by command:

```bash 
ssh-keyscan -t rsa ftp.runtimeverification.com >> ~/.ssh/known_hosts
```

4. Deploy installer:

```bash
sh mvn-release.sh
```

