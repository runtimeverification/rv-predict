.if ${TSAN:Uno} == "yes"
COPTS+=-fsanitize=thread
LDFLAGS+=-fsanitize=thread
.elif ${RV_PREDICT:Uyes} == "yes"
.if ${LEGACY:Uno} == "yes"
RUNTIME_DIR?=${HOME}/rv/rv-predict-legacy/llvm/build/runtime/lib/linux
PASS_DIR?=${HOME}/rv/rv-predict-legacy/llvm/build/pass
RV_PREDICT_CXX_LDADD+=-L${RUNTIME_DIR} -lclang_rt.tsan_cxx-x86_64
LDADD+=-L${RUNTIME_DIR} -lclang_rt.tsan-x86_64
LDADD+=-ldl -lrt
.else
PASS_DIR?=${HOME}/rv/rv-predict/llvm/build/pass
RUNTIME_DIR?=${HOME}/rv/rv-predict/llvm/ngrt
LDADD+=-L${RUNTIME_DIR} -lrvprt
LDADD+=-ldl
.endif
COPTS+=-Xclang -load -Xclang ${PASS_DIR}/rvpinstrument.so
.endif

LDADD+=-pthread
LDADD+=-g

DBG+=-g -O0
