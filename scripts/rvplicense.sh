#!/bin/sh

set -e
set -u

sharedir=$(dirname $0)/../share

java -classpath ${sharedir}/rv-predict-c/rv-predict.jar com.runtimeverification.environment.SetupEnvironment rvpl predict
