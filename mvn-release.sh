#!/bin/sh

set -e
set -u

if [ $(git status -bs | grep -v '^??' | wc -l) != 1 ]; then
	cat 1>&2 <<EOF
$(basename $0): you appear to have uncommitted changes.  Commit them
before you run this script.
EOF
	exit 1
fi

if [ ${SSH_AUTH_SOCK:-none} = none ]; then
	if [ ${_RECURSE:-ok} != ok ]; then
		echo "$(basename $0): cannot start SSH key agent, ssh-agent" \
		    1>&2
		exit 1
	fi
	export _RECURSE=no
	exec ssh-agent ./mvn-release.sh
fi

chmod go-rwx keys/release-key
ssh-add keys/release-key

# -DpreparationGoals="clean install" 
# release:perform release:clean
# for debug output, add -X
mvn -U -B release:prepare release:perform \
    -DpreparationGoals="clean install" \
    -Dusername=git -DskipTests -Dobfuscate -Dskip_installer_test \
    -Darguments="-DskipTests -Dobfuscate -Dskip_installer_test" "$@"
