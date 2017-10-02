#!/bin/sh

# release:clean

mvn -X -B release:prepare release:perform \
    -Dusername=git -DskipTests -Dobfuscate -DpreparationGoals="clean install" \
    -Darguments="-DskipTests -Dobfuscate"
