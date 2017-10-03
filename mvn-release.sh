#!/bin/sh

# release:clean

mvn -X -B release:prepare release:perform release:deploy \
    -Dusername=git -DskipTests -Dobfuscate -DpreparationGoals="clean install" \
    -Darguments="-DskipTests -Dobfuscate"
