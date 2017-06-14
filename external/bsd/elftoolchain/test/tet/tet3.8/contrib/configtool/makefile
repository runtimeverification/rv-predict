#
#  Copyright 1996 X/Open Company Limited (X/Open)
# 
# 
##########################################################################
#
#    SCCS:            %W%
#    NAME:            configure tools
#    PRODUCT:         TETware (Test Environment Toolkit)
#    AUTHOR:          X/Open
#    DATE CREATED:    Sep 10 1997
#    TARGETS:         configure, GuessOS
#    MODIFICATIONS:
#                     "TETware Release"
#                     X/Open Company Ltd. September 1997.
#
##########################################################################

RM = rm
CP = cp -f

all:	GuessOS configure defines/decux4.mk defines/cray.mk defines/linuxthreads.mk defines/irix.mk
	@echo "Type make install"

install:
	-mkdir -p ../../src/helpers
	$(CP) GuessOS ../../src/helpers
	$(CP) configure ../../
	$(CP) defines/decux4.mk ../../src/defines/
	$(CP) defines/cray.mk ../../src/defines/
	$(CP) defines/linuxthreads.mk ../../src/defines/
	$(CP) defines/irix.mk ../../src/defines/


CLOBBER: clobber

clobber:
	$(RM) ../../src/helpers/GuessOS ../../configure ../../src/defines/decux4.mk

