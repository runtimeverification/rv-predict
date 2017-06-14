/*
 *      SCCS:  @(#)trace.h	1.4 (96/08/15) 
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

SCCS:   	@(#)trace.h	1.4 96/08/15 TETware release 3.8
NAME:		trace.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	a header file for internal use by the trace subsystem

MODIFICATIONS:

************************************************************************/


/* trace flags structure */
struct tflags {
	char tf_name;		/* flag name */
	int *tf_vp;		/* ptr to Tflag value */
	int tf_value;		/* saved flag value */
	long tf_sys;		/* flag destination ptype (bit field) */
};

/* macros to set, clear and test bits in tf_sys */
#define TF_SET(sys, ptype)	((sys) |= (1 << (ptype)))
#define TF_CLEAR(sys, ptype)	((sys) &= ~(1 << (ptype)))
#define TF_ISSET(sys, ptype)	((sys) & (1 << (ptype)))

/* map of trace flag cmd line system names to process types */
struct stype {
	char st_name;		/* trace flag name */
	short st_ptype;		/* process type */
};


extern void tet_tfopen(), tet_tftrace();

