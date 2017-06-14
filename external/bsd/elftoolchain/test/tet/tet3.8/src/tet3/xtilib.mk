#
#      SCCS:  @(#)xtilib.mk	1.8 (05/11/28) 
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
# SCCS:   	@(#)xtilib.mk	1.8 05/11/28
# NAME:		xtilib.mk
# PRODUCT:	TETware
# AUTHOR:	Denis McConalogue, UniSoft Ltd.
# DATE CREATED:	August 1993
#
# DESCRIPTION:
#	aux include file for xti specific build
# 
# MODIFICATIONS:
#	Geoff Clare, UniSoft Ltd., Sept 1996
#	Changes for TETWare-Lite.
# 
#	Geoff Clare, UniSoft Ltd., Oct 1996
#	Moved generic part of dir list to makefile
#
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Define Lite/Dist CDEFS here instead of in common.mk.
#
#	Geoff Clare, The Open Group, Nov 2005
#	Added kshapi directory
#
# ************************************************************************

TS_LIBDIRS = servlib xtilib
TS_BINDIRS = syncd tccd xresd kshapi

TS_CDEFS =

