.include <bsd.own.mk>

CC=clang++
CXX=clang++
CXXFLAGS+=-std=c++11

PROGS=	BrokenSpinningLoop DoubleCheckedLocking RaceOnArrayList SimpleRace

SRCS.BrokenSpinningLoop=BrokenSpinningLoop.cpp
SRCS.DoubleCheckedLocking=DoubleCheckedLocking.cpp
SRCS.RaceOnArrayList=RaceOnArrayList.cpp
SRCS.SimpleRace=SimpleRace.cpp

LDADD+=${RV_PREDICT_CXX_LDADD}

.include <bsd.prog.mk>
