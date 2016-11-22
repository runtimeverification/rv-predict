#PASS_DIR=${HOME}/rv/rv-predict-llvm/lib/Transforms/Instrumentation/build/pass
#PASS_DIR=${HOME}/tidy/build/pass
#RUNTIME_DIR=${HOME}/rv/build/lib/linux

#COPTS+=-Xclang -load -Xclang ${PASS_DIR}/rvpinstrument.so
COPTS+=-fsanitize=rvpredict

#LDADD+=-L${RUNTIME_DIR} -lclang_rt.tsan-x86_64 -ldl -lrt
#LDADD+=-pthread
LDADD+=-fsanitize=rvpredict -g

DBG=-g


