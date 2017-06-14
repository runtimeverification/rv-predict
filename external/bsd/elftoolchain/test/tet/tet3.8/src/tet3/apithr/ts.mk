#
#	SCCS: @(#)litelib.mk	1.7 (98/08/28)
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
# SCCS:   	@(#)litelib.mk	1.7 98/08/28 TETware release 3.8
# NAME:		litelib.mk
# PRODUCT:	TETware
# AUTHOR:	Geoff Clare, UniSoft Ltd.
# DATE CREATED:	Sept 1996
#
# DESCRIPTION:
#	aux include file for TETware-Lite specific apithr files
# 
# MODIFICATIONS:
#	Geoff Clare, UniSoft Ltd., Oct 1996
#	Enable tracing in TETware-Lite.
#
#	Geoff Clare, UniSoft Ltd., Oct 1996
#	Set TET_CFLAGS and DTET_CFLAGS.
#
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Added support for shared API libraries.
# 
# ************************************************************************

# there are no transport-specific files or servlib files in the
# TETware-Lite thread-safe api library
TS_OFILES =
SERV_OFILES =

