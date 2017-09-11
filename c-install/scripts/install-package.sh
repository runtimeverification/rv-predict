#!/bin/sh

set -e

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

dpkg -i "$SCRIPTPATH/../rv-predict-c-$1.deb"
apt-get install -i
