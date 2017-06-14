/*
 *      SCCS:  @(#)amsg.c	1.6 (98/08/28) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 *
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

#ifndef lint
static char sccsid[] = "@(#)amsg.c	1.6 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)amsg.c	1.6 98/08/28 TETware release 3.8
NAME:		amsg.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	a message for use with the ASSERT() macro

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "error.h"
#include "dtetlib.h"

TET_IMPORT char tet_assertmsg[] = "Assertion failed:";

