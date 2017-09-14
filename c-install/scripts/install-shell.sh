#!/bin/sh

set -e

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

if ! xset q >/dev/null 2>&1; then
    # CLI install
    sh $SCRIPTPATH/install.sh $1
else
    # Graphical install
    echo "A terminal window will open, asking for sudo credentials."
    xterm -e "sh $SCRIPTPATH/install.sh $1"
fi

echo "Installation finished succesfully."
echo
echo
echo "Installation finished succesfully."
