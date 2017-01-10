PASS_DIR?=${HOME}/rv/rv-predict/llvm/build/pass
#RUNTIME_DIR?=${HOME}/rv/rv-predict/llvm/build/runtime/lib/linux
RUNTIME_DIR?=${HOME}/rv/rv-predict/llvm/ngrt

.if ${TSAN:Uno} == "yes"
COPTS+=-fsanitize=thread
LDFLAGS+=-fsanitize=thread
.elif ${RV_PREDICT:Uyes} == "yes"
COPTS+=-Xclang -load -Xclang ${PASS_DIR}/rvpinstrument.so
#RV_PREDICT_CXX_LDADD+=-L${RUNTIME_DIR} -lclang_rt.tsan_cxx-x86_64
#LDADD+=-L${RUNTIME_DIR} -lclang_rt.tsan-x86_64
LDADD+=-L${RUNTIME_DIR} -lrvprt
LDADD+=-ldl -lrt
.endif

LDADD+=-pthread
LDADD+=-g

DBG=-g -O0
