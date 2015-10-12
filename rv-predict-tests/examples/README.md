This folder contains examples to illustrate RV-Predict.  They are precompiled
in `examples.jar`; their source code is in the `src` directory.  To execute
them normally, run the following commands:

    java -cp examples.jar account.Account
    java -cp examples.jar airlinetickets.Airlinetickets
    java -cp examples.jar benchmarks.JGFRayTracerBenchSizeA
    java -cp examples.jar benchmarks.JGFMonteCarloBenchSizeA
    java -cp examples.jar benchmarks.JGFMolDynBenchSizeA
    java -cp examples.jar examples.BrokenSpinningLoop
    java -cp examples.jar examples.RaceOnSynchronizedMap
    java -cp examples.jar examples.DoubleCheckedLocking
    java -cp examples.jar examples.WriteUnderReadLock
    java -cp examples.jar examples.SimpleRace
    java -cp examples.jar examples.RaceOnArrayList

To execute them with RV-Predict, either replace `java` with `rv-predict`
(make sure you already added the `bin` directory under the RV-Predict
installation directory to your `PATH`) or add the option
`-javaagent:../rv-predict.jar`, e.g:

    rv-predict -cp examples.jar account.Account

or

    java -javaagent:../rv-predict.jar -cp examples.jar account.Account

Read the RV-Predict documentation and blog articles reachable from the
[RV-Predict website](http://runtimeverification.com/predict).
https://runtimeverification.com/predict for more details.  Contact us using
the [Runtime Verification Support](http://runtimeverification.com/support)
page for problems, comments, suggestions.

