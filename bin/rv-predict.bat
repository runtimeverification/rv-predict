@echo off
SET RV_ENGINE_BIN=%~dp0..\lib\rv-predict-engine.jar

REM predict
SET ARGS=
for %%a in (%*) do call :expand %%a
goto End

:expand
SET ARG=%1
SET first=%ARG:~0,1% 
if %first%==- (
   SET ARGS=%ARGS% "%1_opt"
) else (
   SET ARGS=%ARGS% "%1"
)
goto :eof

:End
java -Xmx32g -cp %RV_ENGINE_BIN% rvpredict.engine.main.Main %ARGS%
