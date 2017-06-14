#
#	SCCS: @(#)makefile	1.1 (00/09/05)
#
#	UniSoft Ltd., London, England
#
# Copyright (c) 2000 The Open Group
# All rights reserved.
#
# No part of this source code may be reproduced, stored in a retrieval
# system, or transmitted, in any form or by any means, electronic,
# mechanical, photocopying, recording or otherwise, except as stated in
# the end-user licence agreement, without the prior permission of the
# copyright owners.
# A copy of the end-user licence agreement is contained in the file
# Licence which accompanies this distribution.
# 
# Motif, OSF/1, UNIX and the "X" device are registered trademarks and
# IT DialTone and The Open Group are trademarks of The Open Group in
# the US and other countries.
#
# X/Open is a trademark of X/Open Company Limited in the UK and other
# countries.
#
# ************************************************************************
#
# SCCS:   	@(#)makefile	1.1 00/09/05 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	July 2000
#
# DESCRIPTION:
#	tccdsrv makefile
# 
# MODIFICATIONS:
# 
# ************************************************************************


include ../../defines.mk

ALL = tccdsrv$E

first_make_rule: all

all install CLEAN clean CLOBBER clobber FORCE FRC:
	@set -x;							\
	case `uname -s` in						\
	Windows_*)							\
		$(MAKE) -f win32.mk $@;					\
		;;							\
	*)								\
		$(MAKE) -f unix.mk $@;					\
		;;							\
	esac

$(ALL): all

