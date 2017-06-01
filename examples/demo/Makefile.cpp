CC=clang++
CXX=clang++
CXXFLAGS+=-std=c++11

PROGS=	double-checked-locking local-variable-race 
PROGS+= simple-state-machine spinning-loop stack
PROGS+=	unsafe-vector

SRCS.double-checked-locking=double-checked-locking.cpp
SRCS.local-variable-race=local-variable-race.cpp
SRCS.simple-state-machine=simple-state-machine.cpp
SRCS.spinning-loop=spinning-loop.cpp
SRCS.stack=stack.cpp
SRCS.unsafe-vector=unsafe-vector.cpp

LDADD+=${RV_PREDICT_CXX_LDADD}

.include "../rvp.mk"
.include <mkc.prog.mk>
