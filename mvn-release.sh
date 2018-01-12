#!/bin/sh

# -DpreparationGoals="clean install" 
# release:perform release:clean
# for debug output, add -X
mvn -U -B release:prepare release:perform \
    -DpreparationGoals="clean install" \
    -Dusername=git -DskipTests -Dobfuscate \
    -Darguments="-DskipTests -Dobfuscate" "$@"
