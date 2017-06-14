#
#      SCCS:  @(#)litelib.mk	1.5 (98/09/01) 
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
# SCCS:   	@(#)litelib.mk	1.5 98/09/01
# NAME:		litelib.mk
# PRODUCT:	TETware
# AUTHOR:	Geoff Clare, UniSoft Ltd.
# DATE CREATED:	Sept 1996
#
# DESCRIPTION:
#	aux include file for TETware-Lite specific build
# 
# MODIFICATIONS:
# 
#	Geoff Clare, UniSoft Ltd., Oct 1996
#	Moved generic part of dir list to makefile
#
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Define Lite/Dist CDEFS here instead of in common.mk.
#
# ************************************************************************

TS_LIBDIRS =
TS_BINDIRS =

TS_CDEFS = -DTET_LITE

