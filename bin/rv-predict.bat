@echo off
SET RV_ENGINE_BIN=%~dp0..\lib\rv-predict-engine.jar
SET ANT_LIB=%~dp0..\rv-predict-engine\lib\ant.jar

REM predict
echo "predicting races"
java -Xmx32g -cp %RV_ENGINE_BIN%;%ANT_LIB% rvpredict.engine.main.NewRVPredict %*
