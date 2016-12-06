PASS_DIR=${HOME}/rv/rv-predict-llvm/lib/Transforms/Instrumentation/build/pass
RUNTIME_DIR=${HOME}/rv/build/lib/linux

COPTS+=-Xclang -load -Xclang ${PASS_DIR}/rvpinstrument.so

LDADD+=-L${RUNTIME_DIR} -lclang_rt.tsan-x86_64 -ldl -lrt
LDADD+=-pthread
LDADD+=-g

DBG=-g
