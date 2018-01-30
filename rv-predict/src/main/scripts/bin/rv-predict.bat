@ECHO off
SETLOCAL ENABLEEXTENSIONS
IF ERRORLEVEL 1 ECHO Unable to enable extensions
java -ea -jar "%~dp0\..\lib\rv-predict.jar" %*
ENDLOCAL
