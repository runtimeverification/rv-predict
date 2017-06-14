#
#	SCCS: @(#)inetlib.mk	1.7 (97/03/26)
#
#	UniSoft Ltd., London, England
#
# (C) Copyright 1996 X/Open Company Limited
#
# All rights reserved.  No part of this source code may be reproduced,
# stored in a retrieval system, or transmitted, in any form or by any
# means, electronic, mechanical, photocopying, recording or otherwise,
# except as stated in the end-user licence agreement, without the prior
# permission of the copyright owners.
# A copy of the end-user licence agreement is contained in the file
# Licence which accompanies this distribution.
# 
# X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
# the UK and other countries.
#
# ************************************************************************
#
# SCCS:   	@(#)inetlib.mk	1.7 97/03/26 TETware release 3.8
# NAME:		inetlib.mk
# PRODUCT:	TETware
# AUTHOR:	Geoff Clare, UniSoft Ltd.
# DATE CREATED:	July 1996
#
# DESCRIPTION:
#	aux include file for INET-specific apithr files
# 
# MODIFICATIONS:
#
#	Geoff Clare, UniSoft Ltd., Sept 1996
#	Split out header dependencies.
#
#	Geoff Clare, UniSoft Ltd., Oct 1996
#	Use ../inetlib/shared.mk.
#
#	Geoff Clare, UniSoft Ltd., March 1997
#	Moved include ../servlib/shared.mk from makefile to here.
#
# ************************************************************************

# servlib/shared.mk contains a definition of SERV_OFILES
SERVSRC = ../servlib/
include ../servlib/shared.mk

INETSRC = ../inetlib/
include ../inetlib/shared.mk

