@ECHO off
SETLOCAL ENABLEEXTENSIONS
IF ERRORLEVEL 1 ECHO Unable to enable extensions
java -ea -jar "%~dp0\..\rv-predict.jar" %*
ENDLOCAL
