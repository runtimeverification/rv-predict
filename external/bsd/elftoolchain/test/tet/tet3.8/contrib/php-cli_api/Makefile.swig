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
# PROJECT:	PhpTET
#
# AUTHOR:	Neil Moses
#
# DATE CREATED:	March 2005
#
# DESCRIPTION:	SWIG Makefile for the target phptet_ext.so
#
########################################################################

CC		= /usr/bin/gcc
CFLAGS		= -g -D_POSIX_SOURCE -DNSIG=_NSIG -DTET_LITE
LDFLAGS		= -shared -fPIC
PHP_DIR		= /usr/local/include/php
PHP_INCS	= -I$(PHP_DIR) -I$(PHP_DIR)/main -I$(PHP_DIR)/Zend -I$(PHP_DIR)/TSRM
SWIG		= /usr/local/bin/swig
SFLAGS		= -php4 -dlname tet.so
PHPTET_INT	= phptet.i
PHPTET_OBJ	= phptet_wrap.o
PHPTET_SRC	= phptet_wrap.c
PHPTET_LIB	= php_tet.so
TET_LIB_DIR	= $(TET_ROOT)/lib/tet3
TET_LIBS	= $(TET_LIB_DIR)/tcm.o $(TET_LIB_DIR)/libapi.a

all:		$(PHPTET_LIB)

$(PHPTET_SRC):	$(PHPTET_INT)
		$(SWIG) $(SFLAGS) $<

$(PHPTET_OBJ):	$(PHPTET_SRC)
		$(CC) $(PHP_INCS) -c $<

$(PHPTET_LIB):	$(PHPTET_OBJ)
		$(CC) $(LDFLAGS) -o $@ $< $(TET_LIBS)

clean:
		$(RM) $(PHPTET_SRC) $(PHPTET_OBJ) $(PHPTET_LIB)
