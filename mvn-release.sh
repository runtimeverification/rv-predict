#!/bin/sh

# -DpreparationGoals="clean install" 
# release:perform
# -X

mvn -B release:prepare release:clean \
    -Dusername=git -DskipTests -DskipDocs -Dobfuscate \
    -Darguments="-DskipTests -DskipDocs -Dobfuscate"
