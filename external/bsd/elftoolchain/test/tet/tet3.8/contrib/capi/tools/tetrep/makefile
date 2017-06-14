#
#  Copyright 1996 X/Open Company Limited (X/Open)
# 
# 
##########################################################################
#
#    SCCS:            %W%
#    NAME:            TETware report writer Makefile
#    PRODUCT:         TETware (Test Environment Toolkit)
#    AUTHOR:          X/Open
#    DATE CREATED:    4/23/96
#    TARGETS:         tetrep
#    MODIFICATIONS:
#                     "TETware Release"
#                     X/Open Company Ltd. April 1996.
#
##########################################################################

# C compiler command
# If you use a compiler other than cc you'll need to change this, some
# examples are given below:
CC =		cc

# For AIX use c89
# CC =		c89
#
# GNU gcc
# CC =		gcc

# Compiler options: 
# COPTS , options to the CC compiler
# DEFINES, additional #defines.

#COPTS =
# For SVR4.0 COPTS=-Xc
#COPTS =	 -Xc 
#for HPUX
COPTS =	

DEFINES =	-D_XOPEN_SOURCE 

# Library building commands: if $(AR) adds symbol tables itself, or the
# system has ranlib, set LORDER=echo and TSORT=cat.  If ranlib is not
# required, set RANLIB=echo.
AR =		ar cr
RANLIB =	echo
LORDER =	echo
TSORT =		cat
RM =		rm -f

# For SunOS4 and Ultrix 4.2, RANLIB=ranlib
#RANLIB =	ranlib


CFLAGS     = $(INC) $(COPTS) $(DEFINES)

OBJS	= tetrep.o html.o misc.o
FILES	= tetrep.c html.c misc.c
BIN=../../bin

all:	tetrep 

install: tetrep
	cp tetrep $(BIN)/tetrep

tetrep:	$(OBJS)
	$(CC) -o tetrep $(OBJS)

$(OBJS):	$(FILES)

CLEAN:	clean

clean:
	$(RM) $(OBJS)

CLOBBER: clobber

clobber:
	$(RM) $(OBJS) tetrep $(BIN)/tetrep

	

