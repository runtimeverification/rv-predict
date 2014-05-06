@echo off
SET RV_BENCH_BIN=%~dp0..\benchmarks\bin
SET RV_TEST_BIN=%~dp0..\tests\bin
SET RV_INST_BIN=%~dp0..\lib\rv-predict-inst.jar
SET RV_ENGINE_BIN=%~dp0..\lib\rv-predict-engine.jar
SET RV_LOG_BIN=%~dp0..\lib\rv-predict-log.jar

REM instrument
echo "instrumenting "%1
java -Xmx32g -cp %RV_BENCH_BIN%;%RV_TEST_BIN%;%RV_INST_BIN%;%RV_ENGINE_BIN% rvpredict.instrumentation.Main %1 -nosa
