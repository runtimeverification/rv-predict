/*
 *      SCCS:  @(#)bitset.h	1.1 (97/06/02) 
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
 *
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

/************************************************************************

SCCS:   	@(#)bitset.h	1.1 97/06/02 TETware release 3.8
NAME:		bitset.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1997

DESCRIPTION:
	macros for manipulating bits in an array of longs

MODIFICATIONS:

************************************************************************/

#define BITSPERLONG	(sizeof (long) * 8)
#define NEEDELEM(n)	(((n) + (BITSPERLONG - 1)) / BITSPERLONG)
#define BITSET(n, a)	(a[(n) / BITSPERLONG] |= (1 << ((n) % BITSPERLONG)))
#define BITCLR(n, a)	(a[(n) / BITSPERLONG] &= ~(1 << ((n) % BITSPERLONG)))
#define ISSET(n, a)	(a[(n) / BITSPERLONG] & (1 << ((n) % BITSPERLONG)))

