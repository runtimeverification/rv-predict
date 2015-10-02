@ECHO off
SETLOCAL ENABLEEXTENSIONS
IF ERRORLEVEL 1 ECHO Unable to enable extensions
SET RV_JAR=%~dp0..\rv-predict.jar
java -cp "%RV_JAR%" com.runtimeverification.rvpredict.engine.main.GUIMain
ENDLOCAL
