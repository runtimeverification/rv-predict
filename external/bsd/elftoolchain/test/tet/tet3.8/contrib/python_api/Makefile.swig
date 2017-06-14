########################################################################
#
# Copyright (C) 2004 X/Open Company Limited
# 
# Permission is hereby granted, free of charge, to any person obtaining a
# copy of this software and associated documentation files (the "Software"), 
# to deal in the Software without restriction, including without limitation 
# the rights to use, copy, modify, merge, publish, distribute, sublicense, 
# and/or sell copies of the Software, and to permit persons to whom the 
# Software is furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included 
# in all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
# OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
# THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
# DEALINGS IN THE SOFTWARE.
#
########################################################################
# 
# NAME:		Makefile.swig
#
# PROJECT:	PyTET
#
# AUTHOR:	Neil Moses
#
# DATE CREATED:	May 2004
#
# DESCRIPTION:	SWIG Makefile for the target _pytet.so
#
########################################################################

TET_LIB_DIR	= $(TET_ROOT)/lib/tet3
TET_LIBS	= $(TET_LIB_DIR)/tcm.o $(TET_LIB_DIR)/libapi.a
PYTHON_INC	= /usr/include/python2.2
SWIG		= /usr/local/bin/swig
CC		= /usr/bin/gcc
CFLAGS		=  -D_POSIX_SOURCE -DNSIG=_NSIG -DTET_LITE
LDFLAGS		= -shared
PYTET_LIB	= _pytet.so

all:		$(PYTET_LIB)

pytet_wrap.c:	pytet.i
		$(SWIG) -python $<
		sed -f pytet_patches $@ > $@.tmp
		mv $@.tmp $@

pytet_wrap.o:	pytet_wrap.c
		$(CC) -I$(PYTHON_INC) -c $<

$(PYTET_LIB):	pytet_wrap.o
		$(CC) $(LDFLAGS) -o $@ $< $(TET_LIBS)
