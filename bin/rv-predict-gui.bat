@echo off
SET RV_ENGINE_BIN=%~dp0..\lib\rv-predict-engine.jar
SET RV_ENGINE_ROOT=%~dp0..

REM gui
java -Xmx32g -cp "%RV_ENGINE_BIN%" rvpredict.engine.main.GUIMain "%RV_ENGINE_ROOT%"
