#
# This makefile uses BSD macro files.  You can get a rudimentary
# set of BSD macro files on POSIX-compliant platforms like Linux and
# Mac OS X by installing a Pkgsrc bootstrap kit from pkgsrc.org.
#

CC=clang++
CXX=clang++
CPPFLAGS+=-std=c++11

PROGS=	BrokenSpinningLoop DoubleCheckedLocking RaceOnArrayList SimpleRace

SRCS.BrokenSpinningLoop=BrokenSpinningLoop.cpp
SRCS.DoubleCheckedLocking=DoubleCheckedLocking.cpp
SRCS.RaceOnArrayList=RaceOnArrayList.cpp
SRCS.SimpleRace=SimpleRace.cpp

LDADD+=${RV_PREDICT_CXX_LDADD}

.include "../rvp.mk"
.include <mkc.prog.mk>
