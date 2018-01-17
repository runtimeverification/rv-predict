#!/bin/sh

set -e
set -u

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
version=$1

apt-get update

sh -c "RV_PREDICT_LICENSE_ACCEPTED='yes' dpkg -i \"$SCRIPTPATH/../rv-predict-c_${version}-1_amd64.deb\"" || apt-get install -f -y
sh -c "RV_PREDICT_LICENSE_ACCEPTED='yes' dpkg -i \"$SCRIPTPATH/../rv-predict-c_${version}-1_amd64.deb\""
