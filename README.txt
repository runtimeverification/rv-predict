Runtime Verification Prediction
-------------------------------
RVPredict is a sound dynamic predictive race detection tool that detects data races
through constraint solving. A salient feature of RVPredict is that all reported races
are verified to be real, i.e., RVPredict never reports false alarms.

RVPredict runs through these phases:

Instrumentation    - The program is instrumented at runtime using a java agent.
Logging            - The instrumented program generates execution logs.
Prediction         - Prediction occurs. For race detection this consists of an
                     un-ordered read-write or write-write conflict to a shared 
                     variable.
                     
-- Prepare
RV Prediction uses Z3 or Yices to verify races. 
To install Z3, follow the instruction at 
          http://z3.codeplex.com/SourceControl/latest#README
To install Yices, follow the instruction at
         http://yices.csl.sri.com/download-yices2.shtml
Remember to add the z3 or yices (or both) binaries to your PATH

-- Compiling
ant                                           - Compile the tool and tests

-- Running 
#Invoke rv-predict on a class as you would invoke the Java interpreter
bin/rv-predict <rv_options> <MainClass> <program_options>

<rv_options> can be common options for the java interpreter (e.g., -cp) 
or rv-predic specific options (run rv-predict --help for more information).

-- Example
bin/rv-predict -cp tests/bin demo.Example     - Predict races

----------------------------------------
More online documents: http://fsl.cs.illinois.edu/rvpredict/

--------------------------------------
#HOWTO run RVPredict on Eclipse
cd ECLIPSE
./runeclipse  -- it will execute all steps automatically and detect races

#files:

eclipse.jar -- the target eclipse jar file (just replace it by your version)

dacapo-eclipse.jar -- a test driver for running eclipse from Dacapo benchmarks
(http://www.dacapobench.org/). You can of course replace it by your driver.

booster.jar poa.jar -- utilities for concrectizing Java reflections from Soot
rv-predict-inst.jar -- for instrumenting the target application
rv-predict-log.jar -- for logging a runtime trace into database
rv-predict-engine.jar -- perform race detection (see the usage below)

Usage: java -jar rv-predict-engine.jar rvpredict.engine.main.NewRVPredict [options] classname

General Options:
  -maxlen SIZE                 set window size to SIZE 
  -noschedule                  disable generating racey schedule 
  -nobranch                    disable control flow (MCM) 
  -novolatile                  exclude races on volatile variables 
  -allconsistent               require all read-write consistency (Said) 
  -smtlib1                     use smtlib v1 format (by default uses Yices)
  -outdir PATH                 constraint file directory to PATH 
  -solver_timeout TIME         set solver timeout to TIME seconds 
  -solver_memory MEMORY        set memory used by solver to MEMORY megabytes 
  -timeout TIME                set rvpredict timeout to TIME seconds

A key parameter to RVPredict is the window size (-maxlen SIZE), which means
the largest distance of two events in the trace that RVPredict will analyze
to find races. By default, it is set to 1000. 
The larger the size is, the more races are expected to be detected,
and more time RVPredict will take. In runeclipse, we set the window size to 100000.
