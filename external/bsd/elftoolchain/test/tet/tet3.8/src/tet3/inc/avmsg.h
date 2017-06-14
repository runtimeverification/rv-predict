/*
 *      SCCS:  @(#)avmsg.h	1.14 (05/06/27) 
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

SCCS:   	@(#)avmsg.h	1.14 05/06/27 TETware release 3.8
NAME:		avmsg.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	a header file describing the structure and usage of the
	DTET interprocess character string message

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, August 1993
	Added definitions for OP_RCOPY (recursive copy)

	Denis McConalogue, UniSoft Limited, September 1993
	added AV_SAVEDIR argument to OP_TSFILES request message

	Andrew Dingwall, UniSoft Ltd., August 1996
	Added support for OP_MKALLDIRS and OP_RMALLDIRS

	Geoff Clare, UniSoft Ltd., Oct 1996
	Portability fixes.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.

	Andrew Dingwall, The Open Group, March 2003
	Added support for OP_UTIME, OP_TSFTYPE and OP_FTIME.

	Geoff Clare, The Open Group, June 2005
	Added "full timestamp flag" to description of OP_XROPEN.
 
************************************************************************/


/*
**	structure of a prototypical character string message
**
**	NOTE:
**	if you change this structure, be sure to update the element sizes,
**	dummy structure and initialisation code defined below,
**	and change the version number in dtmsg.h as well
*/

#define AV_NVALUE	3		/* no of av_values */

struct avmsg {
	long av_value[AV_NVALUE];	/* request-specific values */
	unsigned short av_argc;		/* no of strings */
	char *av_argv[1];		/* start of string ptr list */
};

/*
**	an avmsg is packed on to a byte stream as follows
**
**		av_value[AV_NVALUE]
**		av_argc
**		(argc * 2) bytes of string offsets
**		null-separated strings
*/

/* avmsg element positions for use on machine-independent data streams */
/* fixed part */
#define AV_VALUE	0
#define AV_ARGC		(AV_VALUE + (AV_NVALUE * LONGSIZE))
#define AV_ARGVSTART	(AV_ARGC + SHORTSIZE)
/* variable part */
#define AV_ARGV		0
#define AV_ARGVSZ	(AV_ARGV + SHORTSIZE)

/* size of a machine-indepdendent message containing n argv elements -
	the strings themselves start after here */
#define AV_AVMSGSZ(n)	(AV_ARGVSTART + ((n) * AV_ARGVSZ))

/* size of an avmsg structure containing n argv elements */
#define avmsgsz(n)	((int) ((sizeof (struct avmsg) + \
				(sizeof (char *) * ((n) - 1)))))

#if TET_LDST

/* avmsg structure description - fixed part */
#define AVMSG_DESC	{ ST_LONG(AV_NVALUE),	AV_VALUE }, \
			{ ST_USHORT(1),		AV_ARGC }
/* variable part - one unsigned short */
#define OFFSET_DESC	{ ST_USHORT(1),		0 }

/* stdesc initialisation for the fixed part of an avmsg structure */
#define AVMSG_INIT(st, ap, n, fixed) \
		st[n++].st_stoff = (char *) &ap->av_value[0] - (char *) ap; \
		st[n++].st_stoff = (char *) &ap->av_argc - (char *) ap; \
		fixed = n;
#define OFFSET_INIT(offst) \
		offst[0].st_stoff = 0;

#endif /* TET_LDST */



/*
**	definitions of avmsg elements and message sizes for use with various
**		requests
**
**	if you change any of these definitions, be sure to increment the
**	version number in dtmsg.h as well
**	please be very careful when changing existing definitions,
**		particularly those used by more than one request
*/

/* macros for use with requests that pass file lines -
	AV_NLINE is a suggested number of lines to send per message;
	the actual number of lines sent must be determined using the OP_*_ARGC
	macro for the message type being received */
#define AV_NLINE		10
#define AV_FLAG(mp)		((mp)->av_value[0])
#define AV_MORE			1
#define AV_DONE			2

/* OP_EXEC requests a flag, a snid, an xrid, a path, an outfile and n args */
/* #define AV_FLAG(mp)		((mp)->av_value[0]) defined above */
#define AV_SNID(mp)		((mp)->av_value[1])
#define AV_XRID(mp)		((mp)->av_value[2])
#define AV_PATH(mp)		((mp)->av_argv[0])
#define AV_OUTFILE(mp)		((mp)->av_argv[1])
#define AV_EFIXED		2
#define AV_ARG(mp, n)		((mp)->av_argv[(n) + AV_EFIXED])
#define OP_EXEC_NARG(mp)	((int) (mp)->av_argc - AV_EFIXED)
#define OP_EXEC_ARGC(n)		((n) + AV_EFIXED)
/* exec flag values */
#define AV_EXEC_TEST		1	/* sync NO on exec fail */
#define AV_EXEC_USER		2	/* cd to TET_EXECUTE or TET_ROOT,
					   sync NO on fail */
#define AV_EXEC_MISC		3	/* no cd, no sync on fail */

/* OP_XROPEN requests an xfname and a full timestamp flag,
   OP_TFOPEN requests a tfname, a binflag and a mode */
/* #define AV_FLAG(mp)		((mp)->av_value[0]) defined above */
#define AV_MODE(mp)		((mp)->av_value[1])
#define AV_XFNAME(mp)		((mp)->av_argv[0])
#define AV_TFNAME		AV_XFNAME
#define OP_XROPEN_ARGC		1
#define OP_TFOPEN_ARGC		1

/* OP_XRES requests an xrid and some xres lines */
/* #define AV_XRID(mp)		((mp)->av_value[2]) in OP_EXEC above */
#define AV_XLINE(mp, n)		((mp)->av_argv[n])
#define OP_XRES_NLINE(mp)	((int) (mp)->av_argc)
#define OP_XRES_ARGC(n)		(n)

/* OP_CFNAME requests some config file names (to TCCD and XRESD)
   OP_RCFNAME returns some config file names (from XRESD) */
#define AV_CFNAME(mp, n)	((mp)->av_argv[n])
#define OP_CFNAME_NCFNAME(mp)	((int) (mp)->av_argc)
#define OP_CFNAME_ARGC(n)	(n)
/* number of cfnames sent to/from TCCD and XRESD in these requests */
#define TC_NCFNAME		1
#define XD_NCFNAME		3

/* OP_CODESF request to pass file name containing tet result codes (to XRESD) */
#define AV_CODESF(mp, n)	((mp)->av_argv[n])
#define OP_CODESF_NCODESF(mp)	((int) (mp)->av_argc)
#define OP_CODESF_ARGC(n)	(n)
/* number of files sent to XRESD in this request */
#define XD_NCODESF		1


/* OP_SNDCONF requests a flag and some config lines
   OP_RCVCONF return a flag and some config lines
   OP_CONFIG requests a flag, a mode and some config lines;
   (mode values are defined in config.h) */
/* #define AV_FLAG(mp)		((mp)->av_value[0]) defined above */
/* #define AV_MODE(mp)		((mp)->av_value[1]) in OP_TFOPEN above */
#define AV_CLINE(mp, n)		((mp)->av_argv[n])
#define OP_CONF_NLINE(mp)	((int) (mp)->av_argc)
#define OP_CONF_ARGC(n)		(n)

/* OP_PUTENV requests some environment strings */
#define AV_ENVAR(mp, n)		((mp)->av_argv[n])
#define OP_PUTENV_NLINE(mp)	((int) (mp)->av_argc)
#define OP_PUTENV_ARGC(n)	(n)

/* OP_ACCESS requests a path name and a mode */
/* #define AV_MODE(mp)		((mp)->av_value[1]) in OP_TFOPEN above */
/* #define AV_PATH(mp)		((mp)->av_argv[0]) in OP_EXEC above */
#define OP_ACCESS_ARGC		1

/* OP_MKDIR, OP_MKALLDIRS, OP_RMDIR, OP_RMALLDIRS and OP_CHDIR request a
   directory name
   OP_MKTMPDIR both requests and returns a directory name */
#define AV_DIR(mp)		((mp)->av_argv[0])
#define OP_DIR_ARGC		1

/* OP_MKSDIR requests a directory name and a suffix,
	and returns a directory name */
/* #define AV_DIR(mp)		((mp)->av_argv[0]) in OP_MKDIR above */
#define AV_SUFFIX(mp)		((mp)->av_argv[1])
#define OP_MKSDIR_ARGC		2

/* OP_FOPEN requests a file name and a type ("r", "w" etc.)
   OP_UNLINK requests a file name */
#define AV_FNAME(mp)		((mp)->av_argv[0])
#define AV_FTYPE(mp)		((mp)->av_argv[1])
#define OP_FOPEN_ARGC		2
#define OP_UNLINK_ARGC		1

/* OP_GETS returns a flag and some lines
   OP_PUTS requests a file ID and some lines */
/* #define AV_FLAG(mp)		((mp)->av_value[0]) defined above */
#define AV_FID(mp)		((mp)->av_value[1])
#define AV_FLINE(mp, n)		((mp)->av_argv[n])
#define OP_GETS_NLINE(mp)	((int) (mp)->av_argc)
#define OP_PUTS_NLINE		OP_GETS_NLINE
#define OP_GETS_ARGC(n)		(n)
#define OP_PUTS_ARGC		OP_GETS_ARGC

/* OP_LOCKFILE requests a file name and a timeout
   OP_SHARELOCK requests a directory name and a timeout
	and returns a file name */
/* #define AV_FNAME(mp)		((mp)->av_argv[0]) in OP_FOPEN above */
/* #define AV_DIR(mp)		((mp)->av_argv[0]) in OP_MKDIR above */
#define AV_TIMEOUT(mp)		((mp)->av_value[0])
#define OP_LOCKFILE_ARGC	1
#define OP_SHARELOCK_ARGC	OP_LOCKFILE_ARGC

/* OP_RXFILE requests a source and destination file name */
#define AV_XFROM(mp)		((mp)->av_argv[0])
#define AV_XTO(mp)		((mp)->av_argv[1])
#define OP_RXFILE_ARGC		2

/* OP_RCOPY requests a file copy from source to destination */
/* #define AV_XFROM(mp)		((mp)->av_argv[0]) in OP_RXFILE above */
/* #define AV_XTO(mp)		((mp)->av_argv[1]) in OP_RXFILE above */
#define OP_RCOPY_ARGC		2

/* OP_TSFILES requests a flag, a subdir and some file names */
/* #define AV_FLAG(mp)		((mp)->av_value[0]) defined above */
#define AV_SUBDIR(mp)		((mp)->av_argv[0])
#define AV_SAVEDIR(mp)		((mp)->av_argv[1])
#define AV_TFIXED		2
#define AV_TSFILE(mp, n)	((mp)->av_argv[(n) + AV_TFIXED])
#define OP_TSFILES_ARGC(n)	((n) + AV_TFIXED)
#define OP_TSFILES_NFILES(mp)	((int) (mp)->av_argc - AV_TFIXED)
/* tsfiles flag values */
#define AV_TS_LOCAL		1	/* save files on local system */
#define AV_TS_MASTER		2	/* save files on master system */

/* OP_UTIME requests a filename and two times */
/* #define AV_FNAME(mp)		((mp)->av_argv[0]) in OP_FOPEN above */
#define AV_ATIME(mp)		((mp)->av_value[0])
#define AV_MTIME(mp)		((mp)->av_value[1])
#define OP_UTIME_ARGC		1

/* OP_TSFTYPE requests some file type info lines */
#define AV_TSFTYPE(mp, n)	((mp)->av_argv[n])
#define OP_TSFTYPE_NTSFTYPE(mp)	((int) (mp)->av_argc)
#define OP_TSFTYPE_ARGC(n)	(n)

/* OP_FTIME requests a path name */
/* #define AV_PATH(mp)		((mp)->av_argv[0]) in OP_EXEC above */
#define OP_FTIME_ARGC		1

/* extern function declarations */
TET_IMPORT_FUNC(int, tet_avmsg2bs, PROTOLIST((struct avmsg *, char *)));
TET_IMPORT_FUNC(int, tet_avmsgbslen, PROTOLIST((struct avmsg *)));
TET_IMPORT_FUNC(int, tet_bs2avmsg,
	PROTOLIST((char *, int, struct avmsg **, int *)));

