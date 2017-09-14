#!/bin/sh

set -e
set -x

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

sudo sh -c "RV_PREDICT_LICENSE_ACCEPTED='yes' dpkg -i \"$SCRIPTPATH/../rv-predict-c-$1.deb\"" || sudo apt-get install -f -y
sudo sh -c "RV_PREDICT_LICENSE_ACCEPTED='yes' dpkg -i \"$SCRIPTPATH/../rv-predict-c-$1.deb\""
