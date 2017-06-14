########################################################################
#
# SCCS: @(#)makefile.swig	1.2 (05/10/07)
#
# Copyright (C) 2004 The Open Group
# 
# All rights reserved.  No part of this source code may be reproduced,
# stored in a retrieval system, or transmitted, in any form or by any
# means, electronic, mechanical, photocopying, recording or otherwise,
# except as stated in the end-user licence agreement, without the prior
# permission of the copyright owners.
#
# The Open Group and Boundaryless Information Flow are trademarks and
# UNIX is a registered trademark of The Open Group in the United States
# and other countries.
#
########################################################################
# 
# NAME:		makefile.swig
#
# PROJECT:	PyTET
#
# AUTHOR:	Neil Moses
#
# DATE CREATED:	May 2004
#
# DESCRIPTION:	SWIG Makefile for the target pytet_wrap.c
#
########################################################################

SWIG		= swig

all:		pytet_wrap.c

pytet_wrap.c:	pytet.i
		$(SWIG) -python pytet.i
		sed -f pytet_patches $@ > $@.tmp
		mv $@.tmp $@
