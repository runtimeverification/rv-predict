#!/bin/sh

# release:clean
# -Dobfuscate

mvn -X -B release:prepare release:perform \
    -Dusername=git -DskipTests -DpreparationGoals="clean install" \
    -Darguments="-DskipTests"
