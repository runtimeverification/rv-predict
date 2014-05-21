@echo off
if DEFINED RV_TEST_DIR (
  echo User defined the root directory of program to test (%RV_TEST_DIR%) 
  SET PROGRAM_CLASSPATH=%RV_TEST_DIR%
) else (
  SET RV_BENCH_BIN=%~dp0..\benchmarks\bin
  SET RV_TEST_BIN=%~dp0..\tests\bin
  echo User did not defined the root directory of program to test. 
  echo Will use the default (benchmarks + tests) 

  SET PROGRAM_CLASSPATH=%RV_BENCH_BIN%;%RV_TEST_BIN%
)

SET RV_AGENT_BIN=%~dp0..\lib\rv-predict-agent.jar
SET JAVA_AGENT_LIB=%~dp0..\lib\iagent.jar

REM agent logging
echo logging %*
java -Xmx32g -javaagent:%JAVA_AGENT_LIB% -cp %RV_AGENT_BIN%;%PROGRAM_CLASSPATH% %*
