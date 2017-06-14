#
#      SCCS:  @(#)makefile	1.18 (03/03/31) 
#
#	UniSoft Ltd., London, England
#
# (C) Copyright 1992 X/Open Company Limited
#
# All rights reserved.  No part of this source code may be reproduced,
# stored in a retrieval system, or transmitted, in any form or by any
# means, electronic, mechanical, photocopying, recording or otherwise,
# except as stated in the end-user licence agreement, without the prior
# permission of the copyright owners.
#
# X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
# the UK and other countries.
#
#
# ************************************************************************
#
# SCCS:   	@(#)makefile	1.18 03/03/31 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	June 1992
#
# DESCRIPTION:
#	TETware makefile
# 
# MODIFICATIONS:
#	Denis McConalogue, UniSoft Limited, September 1993
#	specify transport library via "include" 
# 
#	Denis McConalogue, UniSoft Limited, September 1993
#	added ../xpg3sh/api to list of DIRS
# 
#	Denis McConalogue, UniSoft Limited, October 1993
#	change references to tet2 to dtet2
# 
#	Andrew Dingwall, UniSoft Limited, January 1994
#	added "first makefile rule" to overcome problems with makes
#	that want to make ALL the targets listed on the first rule line
# 
#	Geoff Clare, UniSoft Ltd., August 1996
#	Changes for TETWare.
#
#	Geoff Clare, UniSoft Ltd., Sept 1996
#	Changes for TETWare-Lite.
#
#	Geoff Clare, UniSoft Ltd., Oct 1996
#	Moved generic part of DIRS to here from *lib.mk
#	Moved tcm to the front of DIRS (it puts files in libapi)
#	Moved tcmthr likewise, and added tcmthr(C++).
#
#	Andrew Dingwall, UniSoft Ltd., October 1996
#	added tcclib
#
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	added support for shared libraries
#
#	Andrew Dingwall, UniSoft Ltd., August 1999
#	Added support for the Java API.
# 
#	Andrew Dingwall, The Open Group, November 2002
#	Added support for product options.
# 
# ************************************************************************

include ../defines.mk
include common.mk
include ts.mk

# the order of the library source directories is intended to produce
# optimal member ordering in libapi.a

DIRS = tcm apilib $(TS_LIBDIRS) dtet2lib tcclib tcc $(TS_BINDIRS)
THRDIRS = tcmthr apithr

# for the *SHLIBDIRS, we make the api library before the tcm library
# because on win32 systems making the api library generates a file
# (dynlink.gen) that is needed by the tcm library
SHLIBDIRS = apishlib tcmshlib
THRSHLIBDIRS = apithrshlib tcmthrshlib

# java API support
# if the first directory isn't present (japilib),
# we assume that the java API package isn't installed on this machine
JAVADIRS = japilib jtools

# other product options
# they might be present or they might not be (depends on the packaging)
OPTDIRS = grw


first_make_rule: all

# there follows a large in-line shell script
# remember that all shell commands need to be terminated by a ';'

all install CLEAN clean:
	@case $$MAKEFLAGS in						\
	*k*)								\
		set +e;							\
		;;							\
	esac;								\
									\
	: "tcm, api library and programs";				\
	for i in $(DIRS);						\
	do								\
		echo $$i:;						\
		(cd $$i && $(MAKE) $@);					\
	done;								\
									\
	: "thread-safe tcm and api library";				\
	case "$(THR_COPTS)" in						\
	*NOT_SUPPORTED*)						\
		;;							\
	*)								\
		for i in $(THRDIRS);					\
		do							\
			echo $$i:;					\
			(cd $$i && $(MAKE) $@);				\
		done;							\
		;;							\
	esac;								\
									\
	: "shared tcm and api libraries (not win32)";			\
	case "$(SHLIB_COPTS):`uname -s`" in				\
	*NOT_SUPPORTED*|*:Windows_??)					\
		;;							\
	*)								\
		for i in $(SHLIBDIRS);					\
		do							\
			echo $$i:;					\
			(cd $$i && $(MAKE) $@);				\
		done;							\
		;;							\
	esac;								\
									\
	: "thread-safe shared tcm and api libraries";			\
	case "$(SHLIB_COPTS)$(THR_COPTS)" in				\
	*NOT_SUPPORTED*)						\
		;;							\
	*)								\
		for i in $(THRSHLIBDIRS);				\
		do							\
			echo $$i:;					\
			(cd $$i && $(MAKE) $@);				\
		done;							\
		;;							\
	esac;								\
									\
	: "C++ tcm";							\
	case "$(C_PLUS):$@" in						\
	*NOT_SUPPORTED*)						\
		;;							\
	*:all)								\
		echo "tcm(C++):";					\
		(cd tcm && $(MAKE) allC);				\
		;;							\
	*:install)							\
		echo "tcm(C++):";					\
		(cd tcm && $(MAKE) installC);				\
		;;							\
	esac;								\
									\
	: "C++ thread-safe tcm";					\
	case "$(THR_COPTS)$(C_PLUS):$@"					\
	in *NOT_SUPPORTED*)						\
		;;							\
	*:all)								\
		echo "tcmthr(C++):";					\
		(cd tcmthr && $(MAKE) allC);				\
		;;							\
	*:install)							\
		echo "tcmthr(C++):";					\
		(cd tcmthr && $(MAKE) installC);			\
		;;							\
	esac;								\
									\
	: "C++ shared tcm (not win32)";					\
	case "$(SHLIB_COPTS)$(C_PLUS):`uname -s`:$@" in			\
	*NOT_SUPPORTED*|*:Windows_??:*)					\
		;;							\
	*:all)								\
		echo "tcmshlib(C++):";					\
		(cd tcmshlib && $(MAKE) allC);				\
		;;							\
	*:install)							\
		echo "tcmshlib(C++):";					\
		(cd tcmshlib && $(MAKE) installC);			\
		;;							\
	esac;								\
									\
	: "C++ thread-safe shared tcm";					\
	case "$(THR_COPTS)$(SHLIB_COPTS)$(C_PLUS):$@" in		\
	*NOT_SUPPORTED*)						\
		;;							\
	*:all)								\
		echo "tcmthrshlib(C++):";				\
		(cd tcmthrshlib && $(MAKE) allC);			\
		;;							\
	*:install)							\
		echo "tcmsthrhlib(C++):";				\
		(cd tcmthrshlib && $(MAKE) installC);			\
		;;							\
	esac;								\
									\
	: "the java API support library";				\
	case "$(JAVA_CDEFS):$(JAVA_COPTS)" in				\
	*NOT_SUPPORTED*)						\
		;;							\
	*)								\
		set $(JAVADIRS);					\
		if test -d $$1;						\
		then							\
			for i in $(JAVADIRS);				\
			do						\
				echo $$i:;				\
				(cd $$i && $(MAKE) $@);			\
			done;						\
		fi;							\
		;;							\
	esac;								\
									\
	: "other product options";					\
	for i in $(OPTDIRS);						\
	do								\
		if test -d $$i;						\
		then							\
			echo $$i:;					\
			(cd $$i && $(MAKE) $@);				\
		fi;							\
	done;


# below are two rules: the first line and then the multi-line script

CLOBBER clobber FORCE FRC:
	@set $(DIRS); set -x; cd $$1; rm -f $(LIBDAPI) $(LIBTHRAPI)
	@case $$MAKEFLAGS in						\
	*k*)								\
		set +e;							\
		;;							\
	esac;								\
	for i in $(DIRS) $(THRDIRS) $(SHLIBDIRS) $(THRSHLIBDIRS);	\
	do								\
		echo $$i:;						\
		(cd $$i && $(MAKE) $@);					\
	done;								\
	for i in $(JAVADIRS) $(OPTDIRS);				\
	do								\
		if test -d $$i;						\
		then							\
			echo $$i:;					\
			(cd $$i && $(MAKE) $@);				\
		fi;							\
	done;


