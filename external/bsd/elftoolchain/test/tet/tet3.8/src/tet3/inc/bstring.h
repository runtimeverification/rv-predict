/*
 *      SCCS:  @(#)bstring.h	1.7 (96/11/07) 
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

/************************************************************************

SCCS:   	@(#)bstring.h	1.7 96/11/07 TETware release 3.8
NAME:		bstring.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	definitions of the BSD bstring(3C) routines in terms of the
	SYSV memory(3C) routines

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., December 1993
	changed sense of tests so that NOBSTRING is the default

	Geoff Clare, UniSoft Ltd., August 1996
	Changed memory.h to string.h.

************************************************************************/

#if defined(BSD42) || defined(BSD43)
	/* nothing */
#else
#  define NOBSTRING
#endif


#ifdef NOBSTRING
#  include <sys/types.h>
#  include <string.h>
#  define bcopy(from, to, count)	((void) memcpy(to, from, (size_t)(count)))
#  define bzero(p, len)			((void) memset(p, '\0', (size_t)(len)))
#endif /* NOBSTRING */

