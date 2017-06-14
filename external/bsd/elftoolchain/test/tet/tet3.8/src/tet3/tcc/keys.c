/*
 *	SCCS: @(#)keys.c	1.4 (02/01/18)
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

#ifndef lint
static char sccsid[] = "@(#)keys.c	1.4 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)keys.c	1.4 02/01/18 TETware release 3.8
NAME:		keys.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	January 1997

DESCRIPTION:
	licence key routines

MODIFICATIONS:

************************************************************************/

/* -START-KEYS-CUT- */

#include <stdio.h>
#include <string.h>
#include "dtmac.h"
#include "keys.h"

static char key[] = "3.8";

char *tet_keyversion()
{
	return(key);
}

unsigned char *tet_keyencode(val)
unsigned long val;
{
	static unsigned char s[KEY_LEN + 1];
	char *kp = key;
	int cksum = 0;
	int n;

	for (n = 0; n < KEY_DIGITS; n++) {
		s[n] = (((unsigned char) ((val >> KEY_SHIFT(n)) & 0xff) ^ *kp++) & KEY_MASK) + KEY_BASE;
		cksum += s[n];
		if (!*kp)
			kp = key;
	}
	(void) sprintf((char *) &s[n], "%*.*x",
		KEY_CKSUM, KEY_CKSUM, cksum & KEY_REM);

	return(s);
}

unsigned long tet_keydecode(s)
unsigned char s[];
{
	unsigned long val = 0L;
	int cksum = 0, n;
	char *kp = key;
	int x;

	if (strlen((char *) s) != KEY_LEN)
		return(0L);

	for (n = 0; n < KEY_DIGITS; n++) {
		cksum += s[n];
		val |= ((unsigned long) ((s[n] - KEY_BASE) ^ *kp++) & KEY_MASK) << KEY_SHIFT(n);
		if (!*kp)
			kp = key;
	}

	if (sscanf((char *) &s[KEY_DIGITS], "%x", &x) != 1 ||
		(cksum & KEY_REM) != x)
			val = 0L;

	return(val);
}

/* -END-KEYS-CUT- */

#ifndef KEY_DIGITS
int tet_keys_c_not_used;
#endif

