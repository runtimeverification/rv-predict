#!/bin/sh

mvn -X -B release:prepare release:clean \
    -Dusername=git -DskipTests -Dobfuscate \
    -Darguments="-DskipTests -Dobfuscate"
