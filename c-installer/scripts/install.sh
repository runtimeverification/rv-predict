#!/bin/sh

set -e

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

echo $1 | sudo -S -k sh -c "RV_PREDICT_LICENSE_ACCEPTED='yes' dpkg -i \"$SCRIPTPATH/../rv-predict-c.deb\"" || apt-get install -f -y
echo $1 | sudo -S -k sh -c "RV_PREDICT_LICENSE_ACCEPTED='yes' dpkg -i \"$SCRIPTPATH/../rv-predict-c.deb\""
