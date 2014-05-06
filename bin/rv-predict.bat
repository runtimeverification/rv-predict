@echo off
SET RV_ENGINE_BIN=%~dp0..\lib\rv-predict-engine.jar

REM predict
echo "predicting races"
java -Xmx32g -cp %RV_ENGINE_BIN% NewRVPredict %*
