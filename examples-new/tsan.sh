#!/bin/bash

echo "Testing race detection for TSan examples"
printf "%0.s-" {1..22}
echo ""


mkdir rvtmp
cp ./racy-c-programs/tsan/Races/Success/* ./rvtmp/
cp ./racy-c-programs/tsan/Races/Fork/* ./rvtmp/
./folder.sh rvtmp
