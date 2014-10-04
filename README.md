It is recommended to add RV-Predict/bin to your PATH.
	
Invoke `rv-predict` on a class as you would invoke the Java interpreter:

    rv-predict [options] class [args...]        #(predict in a class), or
    rv-predict [options] -jar jarfile [args...] #(predict in executable jar)

where `[options]` include both RV-Predict and Java specific options.

You can also invoke `rv-predict` as a Java agent:

    java -javaagent:<rvPath>/lib/rv-predict.jar ...

Additional documentation can be found online on the
[RV-Predict website](http://runtimeverification.com/predict/docs).

For support and bug reports, contact predict@runtimeverification.com or visit
[Runtime Verification Support](http://runtimeverification.com/support/predict).
