@echo off
if %1.==. (
	echo "please input your target program: ./run yourprogram"
) else (
	call %~dp0\rv-instrument.bat %*
	call %~dp0\rv-log.bat %*
	call %~dp0\rv-predict.bat %*
)
