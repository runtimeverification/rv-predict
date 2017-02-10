#
# This makefile uses BSD macro files.  You can get a rudimentary
# set of BSD macro files on POSIX-compliant platforms like Linux and
# Mac OS X by installing a Pkgsrc bootstrap kit from pkgsrc.org.
#
.include <bsd.own.mk>

CC=clang++
CXX=clang++
CXXFLAGS+=-std=c++11

PROGS=	example1 example2

SRCS.example1=example1.cpp
SRCS.example2=example2.cpp

LDADD+=${RV_PREDICT_CXX_LDADD}

.include <bsd.prog.mk>
