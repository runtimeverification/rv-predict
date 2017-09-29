#!/bin/sh

# release:clean

mvn -X -B release:prepare \
    -Dusername=git -DskipTests -Dobfuscate \
    -Darguments="-DskipTests -Dobfuscate"
