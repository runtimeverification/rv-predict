.include <bsd.own.mk>

PROGS=	dot-product square-sum memcpy

CC=clang

SRCS.dot-product=dot-product.c
SRCS.square-sum=square-sum.c
SRCS.memcpy=memcpy.c

# dot-product.bc: dot-product.c ${PASS_DIR}/RVPredictInstrumentation.so
# 	clang -Xclang -load -Xclang ${PASS_DIR}/RVPredictInstrumentation.so -g -S -emit-llvm -o dot-product.bc dot-product.c

# dot-product: dot-product.c ${PASS_DIR}/RVPredictInstrumentation.so ${RUNTIME_DIR}/libclang_rt.tsan-x86_64.a
# 	clang -Xclang -load -Xclang ${PASS_DIR}/RVPredictInstrumentation.so -pthread -g -o dot-product dot-product.c -L${RUNTIME_DIR} -lclang_rt.tsan-x86_64 -ldl -lrt -lm

.include <bsd.prog.mk>
