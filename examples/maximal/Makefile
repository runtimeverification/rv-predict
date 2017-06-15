#
# This makefile uses BSD macro files.  You can get a rudimentary
# set of BSD macro files on POSIX-compliant platforms like Linux and
# Mac OS X by installing a Pkgsrc bootstrap kit from pkgsrc.org.
#
.-include "../mkbin.mk"

CC=clang++
CXX=clang++
CXXFLAGS+=-std=c++11

FILES=Makefile
FILES+=$(SRCS.example1)
FILES+=$(SRCS.example2)
FILESDIR=$(DATADIR)/examples/rv-predict-c/maximal
FILESMODE=0644

.if $(MKBIN:Uno) == "yes"
PROGS=	example1 example2
.endif

SRCS.example1=example1.cpp
SRCS.example2=example2.cpp

LDADD+=${RV_PREDICT_CXX_LDADD}

.include "../rvp.mk"
.include <mkc.prog.mk>
