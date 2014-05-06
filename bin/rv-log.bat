@echo off
SET RV_BENCH_BIN=%~dp0..\benchmarks\bin
SET RV_TEST_BIN=%~dp0..\tests\bin
SET RV_INST_BIN=%~dp0..\lib\rv-predict-inst.jar
SET RV_ENGINE_BIN=%~dp0..\lib\rv-predict-engine.jar
SET RV_LOG_BIN=%~dp0..\lib\rv-predict-log.jar
SET RV_RECORD_BIN=%~dp0..\tmp\record

REM record
echo "logging "%*
java -Xmx32g -cp %RV_RECORD_BIN%;%RV_BENCH_BIN%;%RV_TEST_BIN%;%RV_LOG_BIN%;%RV_ENGINE_BIN% edu.uiuc.run.Main %*
