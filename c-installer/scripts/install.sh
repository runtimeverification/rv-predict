#!/bin/sh

set -e
set -u

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

version=$1

sh -c "RV_PREDICT_LICENSE_ACCEPTED='yes' dpkg -i \"$SCRIPTPATH/../rv-predict-c_version{1}-1_amd64.deb\"" || apt-get install -f -y
sh -c "RV_PREDICT_LICENSE_ACCEPTED='yes' dpkg -i \"$SCRIPTPATH/../rv-predict-c_${version}-1_amd64.deb\""
