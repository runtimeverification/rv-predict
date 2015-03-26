@ECHO off
SETLOCAL ENABLEEXTENSIONS
IF ERRORLEVEL 1 ECHO Unable to enable extensions
call "%~dp0\..\lib\setenv.bat"
IF NOT DEFINED RV_OPTS SET RV_OPTS=-Xms64m -Xmx1024m -Xss32m
SET RV_JAR=%~dp0..\lib\rv-predict.jar
java %RV_OPTS% -jar "%RV_JAR%"
ENDLOCAL
