if [ $1 ]
then
./rv-instrument $@
./rv-log $@
./rv-predict $@
else  
  echo "please input your target program: ./run yourprogram"
fi
