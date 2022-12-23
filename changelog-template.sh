#!/bin/sh

set -e
set -u

. /etc/lsb-release

major=$1
minor=${2:-0}
teeny=${3:-0}
after_dash=${4:-1}

cat<<EOF
rv-predict-c (${major}.${minor}.${teeny}-${after_dash}) ${DISTRIB_CODENAME}; urgency=medium

  * Describe a change here.
  * Describe a change here.
  * Describe a change here.

 -- Runtime Verification, Inc. <support@runtimeverification.com>  $(date +"%a, %d %h %G %H:%M:%S %z")

EOF
