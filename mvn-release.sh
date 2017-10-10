#!/bin/sh

# release:clean
# -Dobfuscate
# -DpreparationGoals="clean install" 

mvn -X -B release:prepare release:perform \
    -Dusername=git -DskipTests \
    -Darguments="-DskipTests"
