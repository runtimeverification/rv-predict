#
#	SCCS: @(#)inetlib.mk	1.1 (98/09/01)
#
#	UniSoft Ltd., London, England
#
# Copyright (c) 1998 The Open Group
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
# SCCS:   	@(#)inetlib.mk	1.1 98/09/01 TETware release 3.8
# NAME:		inetlib.mk
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	August 1998
#
# DESCRIPTION:
#	aux include file for INET-specific apishlib files
# 
# MODIFICATIONS:
#
# ************************************************************************

# servlib/shared.mk contains a definition of SERV_SHARED_OFILES
SERVSRC = ../servlib/
include ../servlib/shared.mk

# inetlib/shared.mk contains a definition of TS_SHARED_OFILES
INETSRC = ../inetlib/
include ../inetlib/shared.mk

