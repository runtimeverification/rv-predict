@ECHO off
SETLOCAL ENABLEEXTENSIONS
IF ERRORLEVEL 1 ECHO Unable to enable extensions
IF NOT DEFINED RV_OPTS SET RV_OPTS=-Xms64m -Xmx1024m -Xss4m
java %RV_OPTS% -ea -cp "%~dp0\..\rv-predict.jar" com.runtimeverification.rvpredict.engine.main.Main %*
ENDLOCAL
