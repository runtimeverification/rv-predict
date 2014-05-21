RVPredict with agent runs through two phases:

Dynamic logging    - The program is instrumented online using ASM and the
                     trace is logged into the database.
Prediction         - Prediction occurs. For race detection this consists of an
                     un-ordered read-write or write-write conflict to a shared 
                     variable.
-- Running 
#Suppose the main class of the target program is demo.Example

ant                           - Compile the test programs in test/src
bin/rv-agent demo.Example     - Log a trace of the program execution
bin/rv-predict demo.Example   - Predict races
