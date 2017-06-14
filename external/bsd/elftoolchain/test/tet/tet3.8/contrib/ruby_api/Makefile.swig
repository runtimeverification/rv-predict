########################################################################
#
# Copyright (C) 2005 X/Open Company Limited
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
# NAME:		Makefile
#
# PROJECT:	RbTET
#
# AUTHOR:	Neil Moses
#
# DATE CREATED:	March 2005
#
# DESCRIPTION:	SWIG Makefile for the target rbtet_ext.so
#
########################################################################

CC		= /usr/bin/gcc
CFLAGS		= -D_POSIX_SOURCE -DNSIG=_NSIG -DTET_LITE
LDFLAGS		= -shared -fPIC
RUBY_INC	= /usr/local/lib/ruby/1.8/i686-linux
SWIG		= /usr/local/bin/swig
SFLAGS		= -ruby
RBTET_INT	= rbtet.i
RBTET_OBJ	= rbtet_wrap.o
RBTET_SRC	= rbtet_wrap.c
RBTET_LIB	= rbtet_ext.so
TET_LIB_DIR	= $(TET_ROOT)/lib/tet3
TET_LIBS	= $(TET_LIB_DIR)/tcm.o $(TET_LIB_DIR)/libapi.a

all:		$(RBTET_LIB)

$(RBTET_SRC):	$(RBTET_INT)
		$(SWIG) $(SFLAGS) $<

$(RBTET_OBJ):	$(RBTET_SRC)
		$(CC) -I$(RUBY_INC) -c $<

$(RBTET_LIB):	$(RBTET_OBJ)
		$(CC) $(LDFLAGS) -o $@ $< $(TET_LIBS)

clean:		$(RBTET_SRC) $(RBTET_OBJ) $(RBTET_LIB)
		$(RM) $(RBTET_SRC) $(RBTET_OBJ) $(RBTET_LIB)
