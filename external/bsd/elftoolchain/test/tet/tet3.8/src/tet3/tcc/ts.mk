#
#	SCCS: @(#)litelib.mk	1.3 (96/12/19)
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
# SCCS:   	@(#)litelib.mk	1.3 96/12/19 TETware release 3.8
# NAME:		litelib.mk
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	October 1996
# 
# DESCRIPTION:
#	tcc TETware-Lite aux makefile
# 
# MODIFICATIONS:
# 
# ************************************************************************

# link only the the generic .o files
OFILES = $(OFILES_GN)

# link with tcclib in TETware-Lite
TCCLIB = ../tcclib/libtcc$A

