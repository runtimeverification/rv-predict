#!/bin/sh

# -DpreparationGoals="clean install" 
# release:perform release:clean
# -X
mvn -X -U -B release:prepare release:perform \
    -DpreparationGoals="clean install" \
    -Dusername=git -DskipTests -Dobfuscate \
    -Darguments="-DskipTests -Dobfuscate" "$@"
