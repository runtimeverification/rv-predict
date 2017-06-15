#
# This makefile uses BSD macro files.  You can get a rudimentary
# set of BSD macro files on POSIX-compliant platforms like Linux and
# Mac OS X by installing a Pkgsrc bootstrap kit from pkgsrc.org.
#
.-include "../mkbin.mk"

FILESDIR=$(DATADIR)/examples/rv-predict-c/cpp11

FILES=Makefile
FILES+=$(SRCS.BrokenSpinningLoop)
FILES+=$(SRCS.DoubleCheckedLocking)
FILES+=$(SRCS.RaceOnArrayList)
FILES+=$(SRCS.SimpleRace)

FILES+=$(SRCS.double-checked-locking)
FILES+=$(SRCS.local-variable-race)
FILES+=$(SRCS.simple-state-machine)
FILES+=$(SRCS.spinning-loop)
FILES+=$(SRCS.stack)
FILES+=$(SRCS.unsafe-vector)

FILESMODE=0644

CC=clang++
CXX=clang++
CPPFLAGS+=-std=c++11

.if $(MKBIN:Uno) == "yes"
PROGS=	BrokenSpinningLoop DoubleCheckedLocking RaceOnArrayList SimpleRace
PROGS=	double-checked-locking local-variable-race
PROGS+= simple-state-machine spinning-loop stack
PROGS+=	unsafe-vector
.endif

SRCS.BrokenSpinningLoop=BrokenSpinningLoop.cpp
SRCS.DoubleCheckedLocking=DoubleCheckedLocking.cpp
SRCS.RaceOnArrayList=RaceOnArrayList.cpp
SRCS.SimpleRace=SimpleRace.cpp

SRCS.double-checked-locking=double-checked-locking.cpp
SRCS.local-variable-race=local-variable-race.cpp
SRCS.simple-state-machine=simple-state-machine.cpp
SRCS.spinning-loop=spinning-loop.cpp
SRCS.stack=stack.cpp
SRCS.unsafe-vector=unsafe-vector.cpp

LDADD+=${RV_PREDICT_CXX_LDADD}

.include "../rvp.mk"
.include <mkc.prog.mk>
