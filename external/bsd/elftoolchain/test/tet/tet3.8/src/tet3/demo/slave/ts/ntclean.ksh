#
#	SCCS: @(#)ntclean.ksh	1.1 (96/11/10)
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
# SCCS:   	@(#)ntclean.ksh	1.1 96/11/10 TETware release 3.8
# NAME:		ntclean.ksh
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	November 1996
# 
# DESCRIPTION:
#	clean tool for use when the distributed demo suite is to be cleaned
#	on a Windows NT system
# 
# MODIFICATIONS:
# 
# ************************************************************************

args=

while test $# -gt 1
do
	args="$args $1"
	shift
done

exec rm $args $1.exe

