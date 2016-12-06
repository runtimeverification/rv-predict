.include <bsd.own.mk>

PROGS=	dot-product lpcq memcpy spcq square-sum

#CC=${HOME}/rv/raw/local/bin/clang
CC=clang
CPPFLAGS.spcq+=-D__STDC_NO_ATOMICS__

SRCS.dot-product=dot-product.c
SRCS.square-sum=square-sum.c
SRCS.memcpy=memcpy.c
SRCS.lpcq=lpcq.c lpcq_main.c
SRCS.spcq=spcq.c spcq_main.c

# dot-product.bc: dot-product.c ${PASS_DIR}/RVPredictInstrumentation.so
# 	clang -Xclang -load -Xclang ${PASS_DIR}/RVPredictInstrumentation.so -g -S -emit-llvm -o dot-product.bc dot-product.c

# dot-product: dot-product.c ${PASS_DIR}/RVPredictInstrumentation.so ${RUNTIME_DIR}/libclang_rt.tsan-x86_64.a
# 	clang -Xclang -load -Xclang ${PASS_DIR}/RVPredictInstrumentation.so -pthread -g -o dot-product dot-product.c -L${RUNTIME_DIR} -lclang_rt.tsan-x86_64 -ldl -lrt -lm

.include <bsd.prog.mk>
