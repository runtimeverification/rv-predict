/*
 *      SCCS:  @(#)sigmap.h	1.5 (96/11/04) 
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

SCCS:   	@(#)sigmap.h	1.5 96/11/04 TETware release 3.8
NAME:		sigmap.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	signal map structure

	this table maps between machine-specific signal numbers (as defined
	in the local <signal.h> file), and DTET signal numbers that may be
	sent to a TCCD in an OP_KILL message

MODIFICATIONS:

************************************************************************/


/* structure of the signal map */
struct sigmap {
	int sig_local;		/* local signal number */
	int sig_dtet;		/* dtet signal number */
};

