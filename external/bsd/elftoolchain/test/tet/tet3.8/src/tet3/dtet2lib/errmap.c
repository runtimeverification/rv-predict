/*
 *      SCCS:  @(#)errmap.c	1.9 (00/03/27)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1993 X/Open Company Limited
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
static char sccsid[] = "@(#)errmap.c	1.9 (00/03/27) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)errmap.c	1.9 00/03/27 TETware release 3.8
NAME:		errmap.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	January 1993

DESCRIPTION:
	error map - used to map between:
		1) common errno values and DTET message reply codes in
			maperr() and unmaperr()
		2) errno values and symbolic errno names in tet_errname()

	if a DTET message reply code does not have a corresponding errno
	value on a particular system, then a dummy map entry must appear

	the symbolic errno names are collected from <errno.h> files
	on several systems - however, your mileage may vary

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., February 1998
	Corrected mapping of ENOENT.

	Andrew Dingwall, UniSoft Ltd., March 2000
	Changed the value of em_repcode that means "no equivalent DTET
	reply code" from 0 to 1 so that ER_OK can be correctly identified.

************************************************************************/

#include <errno.h>
#ifndef TET_LITE	/* -START-LITE-CUT- */
#  ifdef _WIN32		/* -START-WIN32-CUT- */
#    include <winsock.h>
#  endif /* _WIN32 */	/* -END-WIN32-CUT- */
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
#include "dtmac.h"
#include "dtmsg.h"
#include "errmap.h"


struct errmap tet_errmap[] = {

	/* first, the values that have DTET message reply code equivalents */
 	{ 0, ER_OK, "No Error" },

#ifdef E2BIG
	{ E2BIG, ER_E2BIG, "E2BIG" },
#else
	{ -1, ER_E2BIG, "" },
#endif

#ifdef EACCES
	{ EACCES, ER_EACCES, "EACCES" },
#else
	{ -1, ER_EACCES, "" },
#endif

#ifdef EAGAIN
	{ EAGAIN, ER_EAGAIN, "EAGAIN" },
#else
	{ -1, ER_EAGAIN, "" },
#endif

#ifdef EBADF
	{ EBADF, ER_EBADF, "EBADF" },
#else
	{ -1, ER_EBADF, "" },
#endif

#ifdef EBUSY
	{ EBUSY, ER_EBUSY, "EBUSY" },
#else
	{ -1, ER_EBUSY, "" },
#endif

#ifdef ECHILD
	{ ECHILD, ER_ECHILD, "ECHILD" },
#else
	{ -1, ER_ECHILD, "" },
#endif

#ifdef EEXIST
	{ EEXIST, ER_EEXIST, "EEXIST" },
#else
	{ -1, ER_EEXIST, "" },
#endif

#ifdef EFAULT
	{ EFAULT, ER_EFAULT, "EFAULT" },
#else
	{ -1, ER_EFAULT, "" },
#endif

#ifdef EFBIG
	{ EFBIG, ER_EFBIG, "EFBIG" },
#else
	{ -1, ER_EFBIG, "" },
#endif

#ifdef EINTR
	{ EINTR, ER_EINTR, "EINTR" },
#else
	{ -1, ER_EINTR, "" },
#endif

#ifdef EINVAL
	{ EINVAL, ER_EINVAL, "EINVAL" },
#else
	{ -1, ER_EINVAL, "" },
#endif

#ifdef EIO
	{ EIO, ER_EIO, "EIO" },
#else
	{ -1, ER_EIO, "" },
#endif

#ifdef EISDIR
	{ EISDIR, ER_EISDIR, "EISDIR" },
#else
	{ -1, ER_EISDIR, "" },
#endif

#ifdef EMFILE
	{ EMFILE, ER_EMFILE, "EMFILE" },
#else
	{ -1, ER_EMFILE, "" },
#endif

#ifdef EMLINK
	{ EMLINK, ER_EMLINK, "EMLINK" },
#else
	{ -1, ER_EMLINK, "" },
#endif

#ifdef ENFILE
	{ ENFILE, ER_ENFILE, "ENFILE" },
#else
	{ -1, ER_ENFILE, "" },
#endif

#ifdef ENODEV
	{ ENODEV, ER_ENODEV, "ENODEV" },
#else
	{ -1, ER_ENODEV, "" },
#endif

#ifdef ENOENT
	{ ENOENT, ER_ENOENT, "ENOENT" },
#else
	{ -1, ER_NOENT, "" },
#endif

#ifdef ENOEXEC
	{ ENOEXEC, ER_ENOEXEC, "ENOEXEC" },
#else
	{ -1, ER_ENOEXEC, "" },
#endif

#ifdef ENOMEM
	{ ENOMEM, ER_ENOMEM, "ENOMEM" },
#else
	{ -1, ER_ENOMEM, "" },
#endif

#ifdef ENOSPC
	{ ENOSPC, ER_ENOSPC, "ENOSPC" },
#else
	{ -1, ER_ENOSPC, "" },
#endif

#ifdef ENOTBLK
	{ ENOTBLK, ER_ENOTBLK, "ENOTBLK" },
#else
	{ -1, ER_ENOTBLK, "" },
#endif

#ifdef ENOTDIR
	{ ENOTDIR, ER_ENOTDIR, "ENOTDIR" },
#else
	{ -1, ER_ENOTDIR, "" },
#endif

#ifdef ENOTEMPTY
	{ ENOTEMPTY, ER_ENOTEMPTY, "ENOTEMPTY" },
#else
	{ -1, ER_ENOTEMPTY, "" },
#endif

#ifdef ENOTTY
	{ ENOTTY, ER_ENOTTY, "ENOTTY" },
#else
	{ -1, ER_ENOTTY, "" },
#endif

#ifdef ENXIO
	{ ENXIO, ER_ENXIO, "ENXIO" },
#else
	{ -1, ER_ENXIO, "" },
#endif

#ifdef EPERM
	{ EPERM, ER_EPERM, "EPERM" },
#else
	{ -1, ER_EPERM, "" },
#endif

#ifdef EPIPE
	{ EPIPE, ER_EPIPE, "EPIPE" },
#else
	{ -1, ER_EPIPE, "" },
#endif

#ifdef EROFS
	{ EROFS, ER_EROFS, "EROFS" },
#else
	{ -1, ER_EROFS, "" },
#endif

#ifdef ESPIPE
	{ ESPIPE, ER_ESPIPE, "ESPIPE" },
#else
	{ -1, ER_ESPIPE, "" },
#endif

#ifdef ESRCH
	{ ESRCH, ER_ESRCH, "ESRCH" },
#else
	{ -1, ER_ESRCH, "" },
#endif

#ifdef ETXTBSY
	{ ETXTBSY, ER_ETXTBSY, "ETXTBSY" },
#else
	{ -1, ER_ETXTBSY, "" },
#endif

#ifdef EXDEV
	{ EXDEV, ER_EXDEV, "EXDEV" },
#else
	{ -1, ER_EXDEV, "" },
#endif

	/*
	** then, the rest of the errno values;
	** the em_repcode value of 1 means "no equivalent DTET reply code"
	** (valid DTET reply codes are zero or -ve values)
	*/

#ifdef EADDRINUSE
	{ EADDRINUSE, 1, "EADDRINUSE" },
#endif

#ifdef EADDRNOTAVAIL
	{ EADDRNOTAVAIL, 1, "EADDRNOTAVAIL" },
#endif

#ifdef EADV
	{ EADV, 1, "EADV" },
#endif

#ifdef EAFNOSUPPORT
	{ EAFNOSUPPORT, 1, "EAFNOSUPPORT" },
#endif

#ifdef EALREADY
	{ EALREADY, 1, "EALREADY" },
#endif

#ifdef EBADE
	{ EBADE, 1, "EBADE" },
#endif

#ifdef EBADFD
	{ EBADFD, 1, "EBADFD" },
#endif

#ifdef EBADMSG
	{ EBADMSG, 1, "EBADMSG" },
#endif

#ifdef EBADR
	{ EBADR, 1, "EBADR" },
#endif

#ifdef EBADRQC
	{ EBADRQC, 1, "EBADRQC" },
#endif

#ifdef EBADSLT
	{ EBADSLT, 1, "EBADSLT" },
#endif

#ifdef EBFONT
	{ EBFONT, 1, "EBFONT" },
#endif

#ifdef ECANCELED
	{ ECANCELED, 1, "ECANCELED" },
#endif

#ifdef ECHRNG
	{ ECHRNG, 1, "ECHRNG" },
#endif

#ifdef ECLONEME
	{ ECLONEME, 1, "ECLONEME" },
#endif

#ifdef ECOMM
	{ ECOMM, 1, "ECOMM" },
#endif

#ifdef ECONNABORTED
	{ ECONNABORTED, 1, "ECONNABORTED" },
#endif

#ifdef ECONNREFUSED
	{ ECONNREFUSED, 1, "ECONNREFUSED" },
#endif

#ifdef ECONNRESET
	{ ECONNRESET, 1, "ECONNRESET" },
#endif

#ifdef EDEADLK
	{ EDEADLK, 1, "EDEADLK" },
#endif

#ifdef EDEADLOCK
	{ EDEADLOCK, 1, "EDEADLOCK" },
#endif

#ifdef EDESTADDRREQ
	{ EDESTADDRREQ, 1, "EDESTADDRREQ" },
#endif

#ifdef EDIST
	{ EDIST, 1, "EDIST" },
#endif

#ifdef EDOM
	{ EDOM, 1, "EDOM" },
#endif

#ifdef EDOTDOT
	{ EDOTDOT, 1, "EDOTDOT" },
#endif

#ifdef EDQUOT
	{ EDQUOT, 1, "EDQUOT" },
#endif

#ifdef EFORMAT
	{ EFORMAT, 1, "EFORMAT" },
#endif

#ifdef EHOSTDOWN
	{ EHOSTDOWN, 1, "EHOSTDOWN" },
#endif

#ifdef EHOSTUNREACH
	{ EHOSTUNREACH, 1, "EHOSTUNREACH" },
#endif

#ifdef EIDRM
	{ EIDRM, 1, "EIDRM" },
#endif

#ifdef EILSEQ
	{ EILSEQ, 1, "EILSEQ" },
#endif

#ifdef EINIT
	{ EINIT, 1, "EINIT" },
#endif

#ifdef EINPROGRESS
	{ EINPROGRESS, 1, "EINPROGRESS" },
#endif

#ifdef EISCONN
	{ EISCONN, 1, "EISCONN" },
#endif

#ifdef EISNAM
	{ EISNAM, 1, "EISNAM" },
#endif

#ifdef EL2HLT
	{ EL2HLT, 1, "EL2HLT" },
#endif

#ifdef EL2NSYNC
	{ EL2NSYNC, 1, "EL2NSYNC" },
#endif

#ifdef EL3HLT
	{ EL3HLT, 1, "EL3HLT" },
#endif

#ifdef EL3RST
	{ EL3RST, 1, "EL3RST" },
#endif

#ifdef ELIBACC
	{ ELIBACC, 1, "ELIBACC" },
#endif

#ifdef ELIBBAD
	{ ELIBBAD, 1, "ELIBBAD" },
#endif

#ifdef ELIBEXEC
	{ ELIBEXEC, 1, "ELIBEXEC" },
#endif

#ifdef ELIBMAX
	{ ELIBMAX, 1, "ELIBMAX" },
#endif

#ifdef ELIBSCN
	{ ELIBSCN, 1, "ELIBSCN" },
#endif

#ifdef ELNRNG
	{ ELNRNG, 1, "ELNRNG" },
#endif

#ifdef ELOOP
	{ ELOOP, 1, "ELOOP" },
#endif

#ifdef EMEDIA
	{ EMEDIA, 1, "EMEDIA" },
#endif

#ifdef EMEDIUMTYPE
	{ EMEDIUMTYPE, 1, "EMEDIUMTYPE" },
#endif

#ifdef EMSGSIZE
	{ EMSGSIZE, 1, "EMSGSIZE" },
#endif

#ifdef EMULTIHOP
	{ EMULTIHOP, 1, "EMULTIHOP" },
#endif

#ifdef ENAMETOOLONG
	{ ENAMETOOLONG, 1, "ENAMETOOLONG" },
#endif

#ifdef ENAVAIL
	{ ENAVAIL, 1, "ENAVAIL" },
#endif

#ifdef ENET
	{ ENET, 1, "ENET" },
#endif

#ifdef ENETDOWN
	{ ENETDOWN, 1, "ENETDOWN" },
#endif

#ifdef ENETRESET
	{ ENETRESET, 1, "ENETRESET" },
#endif

#ifdef ENETUNREACH
	{ ENETUNREACH, 1, "ENETUNREACH" },
#endif

#ifdef ENOANO
	{ ENOANO, 1, "ENOANO" },
#endif

#ifdef ENOATTR
	{ ENOATTR, 1, "ENOATTR" },
#endif

#ifdef ENOBUFS
	{ ENOBUFS, 1, "ENOBUFS" },
#endif

#ifdef ENOCONNECT
	{ ENOCONNECT, 1, "ENOCONNECT" },
#endif

#ifdef ENOCSI
	{ ENOCSI, 1, "ENOCSI" },
#endif

#ifdef ENODATA
	{ ENODATA, 1, "ENODATA" },
#endif

#ifdef ENOLCK
	{ ENOLCK, 1, "ENOLCK" },
#endif

#ifdef ENOLINK
	{ ENOLINK, 1, "ENOLINK" },
#endif

#ifdef ENOMEDIUM
	{ ENOMEDIUM, 1, "ENOMEDIUM" },
#endif

#ifdef ENOMSG
	{ ENOMSG, 1, "ENOMSG" },
#endif

#ifdef ENONET
	{ ENONET, 1, "ENONET" },
#endif

#ifdef ENOPKG
	{ ENOPKG, 1, "ENOPKG" },
#endif

#ifdef ENOPROTOOPT
	{ ENOPROTOOPT, 1, "ENOPROTOOPT" },
#endif

#ifdef ENOSR
	{ ENOSR, 1, "ENOSR" },
#endif

#ifdef ENOSTR
	{ ENOSTR, 1, "ENOSTR" },
#endif

#ifdef ENOSYS
	{ ENOSYS, 1, "ENOSYS" },
#endif

#ifdef ENOTCONN
	{ ENOTCONN, 1, "ENOTCONN" },
#endif

#ifdef ENOTNAM
	{ ENOTNAM, 1, "ENOTNAM" },
#endif

#ifdef ENOTREADY
	{ ENOTREADY, 1, "ENOTREADY" },
#endif

#ifdef ENOTRUST
	{ ENOTRUST, 1, "ENOTRUST" },
#endif

#ifdef ENOTSOCK
	{ ENOTSOCK, 1, "ENOTSOCK" },
#endif

#ifdef ENOTSUP
	{ ENOTSUP, 1, "ENOTSUP" },
#endif

#ifdef ENOTUNIQ
	{ ENOTUNIQ, 1, "ENOTUNIQ" },
#endif

#ifdef EOPCOMPLETE
	{ EOPCOMPLETE, 1, "EOPCOMPLETE" },
#endif

#ifdef EOPNOTSUPP
	{ EOPNOTSUPP, 1, "EOPNOTSUPP" },
#endif

#ifdef EOVERFLOW
	{ EOVERFLOW, 1, "EOVERFLOW" },
#endif

#ifdef EPATHREMOTE
	{ EPATHREMOTE, 1, "EPATHREMOTE" },
#endif

#ifdef EPFNOSUPPORT
	{ EPFNOSUPPORT, 1, "EPFNOSUPPORT" },
#endif

#ifdef EPOWERFAIL
	{ EPOWERFAIL, 1, "EPOWERFAIL" },
#endif

#ifdef EPROCLIM
	{ EPROCLIM, 1, "EPROCLIM" },
#endif

#ifdef EPROTO
	{ EPROTO, 1, "EPROTO" },
#endif

#ifdef EPROTONOSUPPORT
	{ EPROTONOSUPPORT, 1, "EPROTONOSUPPORT" },
#endif

#ifdef EPROTOTYPE
	{ EPROTOTYPE, 1, "EPROTOTYPE" },
#endif

#ifdef ERANGE
	{ ERANGE, 1, "ERANGE" },
#endif

#ifdef EREMCHG
	{ EREMCHG, 1, "EREMCHG" },
#endif

#ifdef EREMDEV
	{ EREMDEV, 1, "EREMDEV" },
#endif

#ifdef EREMOTE
	{ EREMOTE, 1, "EREMOTE" },
#endif

#ifdef EREMOTEIO
	{ EREMOTEIO, 1, "EREMOTEIO" },
#endif

#ifdef EREMOTERELEASE
	{ EREMOTERELEASE, 1, "EREMOTERELEASE" },
#endif

#ifdef ERESTART
	{ ERESTART, 1, "ERESTART" },
#endif

#ifdef ERFACOMPLETE
	{ ERFACOMPLETE, 1, "ERFACOMPLETE" },
#endif

#ifdef ERREMOTE
	{ ERREMOTE, 1, "ERREMOTE" },
#endif

#ifdef ESAD
	{ ESAD, 1, "ESAD" },
#endif

#ifdef ESHUTDOWN
	{ ESHUTDOWN, 1, "ESHUTDOWN" },
#endif

#ifdef ESOCKTNOSUPPORT
	{ ESOCKTNOSUPPORT, 1, "ESOCKTNOSUPPORT" },
#endif

#ifdef ESOFT
	{ ESOFT, 1, "ESOFT" },
#endif

#ifdef ESRMNT
	{ ESRMNT, 1, "ESRMNT" },
#endif

#ifdef ESTALE
	{ ESTALE, 1, "ESTALE" },
#endif

#ifdef ESTRPIPE
	{ ESTRPIPE, 1, "ESTRPIPE" },
#endif

#ifdef ETIME
	{ ETIME, 1, "ETIME" },
#endif

#ifdef ETIMEDOUT
	{ ETIMEDOUT, 1, "ETIMEDOUT" },
#endif

#ifdef ETOOMANYREFS
	{ ETOOMANYREFS, 1, "ETOOMANYREFS" },
#endif

#ifdef EUCLEAN
	{ EUCLEAN, 1, "EUCLEAN" },
#endif

#ifdef EUNATCH
	{ EUNATCH, 1, "EUNATCH" },
#endif

#ifdef EUSERS
	{ EUSERS, 1, "EUSERS" },
#endif

#ifdef EWOULDBLOCK
	{ EWOULDBLOCK, 1, "EWOULDBLOCK" },
#endif

#ifdef EWRPROTECT
	{ EWRPROTECT, 1, "EWRPROTECT" },
#endif

#ifdef EXFULL
	{ EXFULL, 1, "EXFULL" },
#endif


#ifndef TET_LITE	/* -START-LITE-CUT- */
#  ifdef _WIN32		/* -START-WIN32-CUT- */

	/* finally, the winsock errors on WIN32 */

#    ifdef WSAEACCES
	{ WSAEACCES, ER_EACCES, "WSAEACCES" },
#    endif

#    ifdef WSAEADDRINUSE
	{ WSAEADDRINUSE, 1, "WSAEADDRINUSE" },
#    endif

#    ifdef WSAEADDRNOTAVAIL
	{ WSAEADDRNOTAVAIL, 1, "WSAEADDRNOTAVAIL" },
#    endif

#    ifdef WSAEAFNOSUPPORT
	{ WSAEAFNOSUPPORT, 1, "WSAEAFNOSUPPORT" },
#    endif

#    ifdef WSAEALREADY
	{ WSAEALREADY, 1, "WSAEALREADY" },
#    endif

#    ifdef WSAEBADF
	{ WSAEBADF, ER_EBADF, "WSAEBADF" },
#    endif

#    ifdef WSAECONNABORTED
	{ WSAECONNABORTED, 1, "WSAECONNABORTED" },
#    endif

#    ifdef WSAECONNREFUSED
	{ WSAECONNREFUSED, 1, "WSAECONNREFUSED" },
#    endif

#    ifdef WSAECONNRESET
	{ WSAECONNRESET, 1, "WSAECONNRESET" },
#    endif

#    ifdef WSAEDESTADDRREQ
	{ WSAEDESTADDRREQ, 1, "WSAEDESTADDRREQ" },
#    endif

#    ifdef WSAEDISCON
	{ WSAEDISCON, 1, "WSAEDISCON" },
#    endif

#    ifdef WSAEDQUOT
	{ WSAEDQUOT, 1, "WSAEDQUOT" },
#    endif

#    ifdef WSAEFAULT
	{ WSAEFAULT, ER_EFAULT, "WSAEFAULT" },
#    endif

#    ifdef WSAEHOSTDOWN
	{ WSAEHOSTDOWN, 1, "WSAEHOSTDOWN" },
#    endif

#    ifdef WSAEHOSTUNREACH
	{ WSAEHOSTUNREACH, 1, "WSAEHOSTUNREACH" },
#    endif

#    ifdef WSAEINPROGRESS
	{ WSAEINPROGRESS, 1, "WSAEINPROGRESS" },
#    endif

#    ifdef WSAEINTR
	{ WSAEINTR, ER_EINTR, "WSAEINTR" },
#    endif

#    ifdef WSAEINVAL
	{ WSAEINVAL, ER_EINVAL, "WSAEINVAL" },
#    endif

#    ifdef WSAEISCONN
	{ WSAEISCONN, 1, "WSAEISCONN" },
#    endif

#    ifdef WSAELOOP
	{ WSAELOOP, 1, "WSAELOOP" },
#    endif

#    ifdef WSAEMFILE
	{ WSAEMFILE, ER_EMFILE, "WSAEMFILE" },
#    endif

#    ifdef WSAEMSGSIZE
	{ WSAEMSGSIZE, 1, "WSAEMSGSIZE" },
#    endif

#    ifdef WSAENAMETOOLONG
	{ WSAENAMETOOLONG, 1, "WSAENAMETOOLONG" },
#    endif

#    ifdef WSAENETDOWN
	{ WSAENETDOWN, 1, "WSAENETDOWN" },
#    endif

#    ifdef WSAENETRESET
	{ WSAENETRESET, 1, "WSAENETRESET" },
#    endif

#    ifdef WSAENETUNREACH
	{ WSAENETUNREACH, 1, "WSAENETUNREACH" },
#    endif

#    ifdef WSAENOBUFS
	{ WSAENOBUFS, 1, "WSAENOBUFS" },
#    endif

#    ifdef WSAENOPROTOOPT
	{ WSAENOPROTOOPT, 1, "WSAENOPROTOOPT" },
#    endif

#    ifdef WSAENOTCONN
	{ WSAENOTCONN, 1, "WSAENOTCONN" },
#    endif

#    ifdef WSAENOTEMPTY
	{ WSAENOTEMPTY, 1, "WSAENOTEMPTY" },
#    endif

#    ifdef WSAENOTSOCK
	{ WSAENOTSOCK, 1, "WSAENOTSOCK" },
#    endif

#    ifdef WSAEOPNOTSUPP
	{ WSAEOPNOTSUPP, 1, "WSAEOPNOTSUPP" },
#    endif

#    ifdef WSAEPFNOSUPPORT
	{ WSAEPFNOSUPPORT, 1, "WSAEPFNOSUPPORT" },
#    endif

#    ifdef WSAEPROCLIM
	{ WSAEPROCLIM, 1, "WSAEPROCLIM" },
#    endif

#    ifdef WSAEPROTONOSUPPORT
	{ WSAEPROTONOSUPPORT, 1, "WSAEPROTONOSUPPORT" },
#    endif

#    ifdef WSAEPROTOTYPE
	{ WSAEPROTOTYPE, 1, "WSAEPROTOTYPE" },
#    endif

#    ifdef WSAEREMOTE
	{ WSAEREMOTE, 1, "WSAEREMOTE" },
#    endif

#    ifdef WSAESHUTDOWN
	{ WSAESHUTDOWN, 1, "WSAESHUTDOWN" },
#    endif

#    ifdef WSAESOCKTNOSUPPORT
	{ WSAESOCKTNOSUPPORT, 1, "WSAESOCKTNOSUPPORT" },
#    endif

#    ifdef WSAESTALE
	{ WSAESTALE, 1, "WSAESTALE" },
#    endif

#    ifdef WSAETIMEDOUT
	{ WSAETIMEDOUT, 1, "WSAETIMEDOUT" },
#    endif

#    ifdef WSAETOOMANYREFS
	{ WSAETOOMANYREFS, 1, "WSAETOOMANYREFS" },
#    endif

#    ifdef WSAEUSERS
	{ WSAEUSERS, 1, "WSAEUSERS" },
#    endif

#    ifdef WSAEWOULDBLOCK
	{ WSAEWOULDBLOCK, 1, "WSAEWOULDBLOCK" },
#    endif

#  endif /* _WIN32 */	/* -END-WIN32-CUT- */
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

};

int tet_Nerrmap = sizeof tet_errmap / sizeof tet_errmap[0];

