/*
 *	SCCS: @(#)keys.h	1.2 (97/07/15)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1997 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

/************************************************************************

SCCS:   	@(#)keys.h	1.2 97/07/15 TETware release 3.8
NAME:		keys.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	January 1997

DESCRIPTION:
	a header file for use with the licence key routines

MODIFICATIONS:

************************************************************************/

/* -START-KEYS-CUT- */

#ifdef TET_KEYS
#  if defined(__STDC__) && !defined(PROTOTYPES)
#    define PROTOTYPES
#  endif

#  ifdef PROTOTYPES
#    define PROTOLIST(x)	x
#  else
#    define PROTOLIST(x)	()
#  endif
#endif

#define KEY_DIGITS	8
#define KEY_MASK	0xf
#define KEY_SHIFT(n)	(((KEY_DIGITS - 1) - (n)) * (32 / KEY_DIGITS))
#define KEY_CKSUM	3
#define KEY_REM		(~(~0 << (KEY_CKSUM * 4)))
#define	KEY_BASE	'a'
#define KEY_LEN		(KEY_DIGITS + KEY_CKSUM)

#define KEY_M0		'b'
#define KEY_M1		'P'
#define KEY_M2		'd'
#define KEY_M3		'd'
#define KEY_M4		'h'
#define KEY_M5		'o'
#define KEY_M6		'4'
#define KEY_M7		'3'
#define	KEY_M8		'c'
#define KEY_M9		'h'
#define KEY_MLEN	10

#define KEY_DEF		{ '\0', KEY_M0, KEY_M1, KEY_M2, KEY_M3, KEY_M4, \
				KEY_M5, KEY_M6, KEY_M7, KEY_M8, KEY_M9, \
				KEY_M0, KEY_M1, KEY_M2, KEY_M3, KEY_M4, \
				KEY_M5, KEY_M6, KEY_M7, KEY_M8, KEY_M9, \
				KEY_M0, KEY_M1, KEY_M2, KEY_M3, KEY_M4 \
			}

#define KEY_ISMAGIC(p)	(*(p) == KEY_M0 && \
				*((p) + 1) == KEY_M1 && \
				*((p) + 2) == KEY_M2 && \
				*((p) + 3) == KEY_M3 && \
				*((p) + 4) == KEY_M4 && \
				*((p) + 5) == KEY_M5 && \
				*((p) + 6) == KEY_M6 && \
				*((p) + 7) == KEY_M7 && \
				*((p) + 8) == KEY_M8)


#define DAY		(60 * 60 * 24)


/* extern function declarations */
extern unsigned char *tet_keyencode PROTOLIST((unsigned long));
extern unsigned long tet_keydecode PROTOLIST((unsigned char []));
extern char *tet_keyversion PROTOLIST((void));

/* -END-KEYS-CUT- */

