/*
 *	SCCS: @(#)w32err.c	1.2 (97/07/21)
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
static char sccsid[] = "@(#)w32err.c	1.2 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)w32err.c	1.2 97/07/21 TETware release 3.8
NAME:		w32err.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1997

DESCRIPTION:
	function to convert a WIN32 error code to an errno value

MODIFICATIONS:

************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */

#include <stdio.h>
#include <windows.h>
#include <errno.h>
#include "dtmac.h"
#include "dtetlib.h"

#ifndef NOTRACE
#  include "ltoa.h"
#endif


/*
**	tet_w32err2errno() - convert a WIN32 error code to its
**		approximate errno equivalent
**
**	note that the meaning of ERROR_INVALID_HANDLE depends on
**	which system call generated this error;
**	the mapping provided here is only appropriate if the system
**	call is one which performs file i/o
*/

int tet_w32err2errno(errval)
register unsigned long errval;
{
	switch (errval) {

	case ERROR_ACCESS_DENIED:
		return(EACCES);

	case ERROR_ALREADY_EXISTS:
		return(EEXIST);

	case ERROR_ARENA_TRASHED:
		return(ENOMEM);

	case ERROR_AUTODATASEG_EXCEEDS_64k:
		return(ENOEXEC);

	case ERROR_BAD_COMMAND:
		return(EACCES);

	case ERROR_BAD_ENVIRONMENT:
		return(E2BIG);

	case ERROR_BAD_EXE_FORMAT:
		return(ENOEXEC);

	case ERROR_BAD_FORMAT:
		return(ENOEXEC);

	case ERROR_BAD_LENGTH:
		return(EACCES);

	case ERROR_BAD_NETPATH:
		return(ENOENT);

	case ERROR_BAD_NET_NAME:
		return(ENOENT);

	case ERROR_BAD_PATHNAME:
		return(ENOENT);

	case ERROR_BAD_THREADID_ADDR:
		return(EFAULT);

	case ERROR_BAD_UNIT:
		return(EACCES);

	case ERROR_BEGINNING_OF_MEDIA:
		return(ENXIO);

	case ERROR_BROKEN_PIPE:
		return(EPIPE);

	case ERROR_BUFFER_OVERFLOW:
		return(ENAMETOOLONG);

	case ERROR_BUSY:
		return(EAGAIN);

	case ERROR_BUSY_DRIVE:
		return(EBUSY);

	case ERROR_BUS_RESET:
		return(EIO);

	case ERROR_CALL_NOT_IMPLEMENTED:
		return(ENOSYS);

	case ERROR_CANCEL_VIOLATION:
		return(ENOLCK);

	case ERROR_CANNOT_MAKE:
		return(EACCES);

	case ERROR_CHILD_NOT_COMPLETE:
		return(ECHILD);

	case ERROR_CRC:
		return(EACCES);

	case ERROR_CURRENT_DIRECTORY:
		return(EACCES);

	case ERROR_DEVICE_IN_USE:
		return(EBUSY);

	case ERROR_DEV_NOT_EXIST:
		return(ENODEV);

	case ERROR_DIRECTORY:
		return(EINVAL);

	case ERROR_DIRECT_ACCESS_HANDLE:
		return(EBADF);

	case ERROR_DIR_NOT_EMPTY:
		return(EEXIST);

	case ERROR_DISCARDED:
		return(ENOLCK);

	case ERROR_DISK_FULL:
		return(ENOSPC);

	case ERROR_DISK_OPERATION_FAILED:
		return(EIO);

	case ERROR_DISK_RECALIBRATE_FAILED:
		return(EIO);

	case ERROR_DISK_RESET_FAILED:
		return(EIO);

	case ERROR_DLL_INIT_FAILED:
		return(ENOEXEC);

	case ERROR_DLL_NOT_FOUND:
		return(ENOENT);

	case ERROR_DRIVE_LOCKED:
		return(EACCES);

	case ERROR_DYNLINK_FROM_INVALID_RING:
		return(ENOEXEC);

	case ERROR_EAS_DIDNT_FIT:
		return(ENOSPC);

	case ERROR_EAS_NOT_SUPPORTED:
		return(ENOSYS);

	case ERROR_EA_ACCESS_DENIED:
		return(EACCES);

	case ERROR_EA_TABLE_FULL:
		return(ENOSPC);

	case ERROR_END_OF_MEDIA:
		return(ENXIO);

	case ERROR_EOM_OVERFLOW:
		return(ENOSPC);

	case ERROR_EXCL_SEM_ALREADY_OWNED:
		return(EACCES);

	case ERROR_EXE_MARKED_INVALID:
		return(ENOEXEC);

	case ERROR_FAIL_I24:
		return(EACCES);

	case ERROR_FILENAME_EXCED_RANGE:
		return(ENOENT);

	case ERROR_FILE_CORRUPT:
		return(EIO);

	case ERROR_FILE_EXISTS:
		return(EEXIST);

	case ERROR_FILE_NOT_FOUND:
		return(ENOENT);

	case ERROR_FLOPPY_BAD_REGISTERS:
		return(EIO);

	case ERROR_FLOPPY_ID_MARK_NOT_FOUND:
		return(EIO);

	case ERROR_FLOPPY_UNKNOWN_ERROR:
		return(EIO);

	case ERROR_FLOPPY_WRONG_CYLINDER:
		return(EIO);

	case ERROR_FULLSCREEN_MODE:
		return(ENOTTY);

	case ERROR_GEN_FAILURE:
		return(EACCES);

	case ERROR_GROUP_EXISTS:
		return(EEXIST);

	case ERROR_HANDLE_DISK_FULL:
		return(ENOSPC);

	case ERROR_HOOK_NOT_INSTALLED:
		return(ENOENT);

	case ERROR_HOTKEY_NOT_REGISTERED:
		return(ENOENT);

	case ERROR_ILL_FORMED_PASSWORD:
		return(EINVAL);

	case ERROR_INFLOOP_IN_RELOC_CHAIN:
		return(ENOEXEC);

	case ERROR_INSUFFICIENT_BUFFER:
		return(EINVAL);

	case ERROR_INVALID_ACCESS:
		return(EINVAL);

	case ERROR_INVALID_BLOCK:
		return(ENOMEM);

	case ERROR_INVALID_CATEGORY:
		return(ENOTTY);

	case ERROR_INVALID_DLL:
		return(ENOEXEC);

	case ERROR_INVALID_DRIVE:
		return(ENOENT);

	case ERROR_INVALID_EXE_SIGNATURE:
		return(ENOEXEC);

	case ERROR_INVALID_FUNCTION:
		return(EINVAL);

	case ERROR_INVALID_HANDLE:
		return(EBADF);

	case ERROR_INVALID_MINALLOCSIZE:
		return(ENOEXEC);

	case ERROR_INVALID_MODULETYPE:
		return(ENOEXEC);

	case ERROR_INVALID_PARAMETER:
		return(EINVAL);

	case ERROR_INVALID_SEGDPL:
		return(ENOEXEC);

	case ERROR_INVALID_STACKSEG:
		return(ENOEXEC);

	case ERROR_INVALID_STARTING_CODESEG:
		return(ENOEXEC);

	case ERROR_INVALID_TARGET_HANDLE:
		return(EBADF);

	case ERROR_IOPL_NOT_ENABLED:
		return(ENOEXEC);

	case ERROR_IO_DEVICE:
		return(EIO);

	case ERROR_IRQ_BUSY:
		return(EBUSY);

	case ERROR_IS_JOIN_PATH:
		return(ENOMEM);

	case ERROR_ITERATED_DATA_EXCEEDS_64k:
		return(ENOEXEC);

	case ERROR_LABEL_TOO_LONG:
		return(ENAMETOOLONG);

	case ERROR_LICENSE_QUOTA_EXCEEDED:
		return(EAGAIN);

	case ERROR_LOCK_FAILED:
		return(EACCES);

	case ERROR_LOCK_VIOLATION:
		return(EACCES);

	case ERROR_MAX_THRDS_REACHED:
		return(EAGAIN);

	case ERROR_MOD_NOT_FOUND:
		return(ENOENT);

	case ERROR_MORE_DATA:
		return(EAGAIN);

	case ERROR_NEGATIVE_SEEK:
		return(EINVAL);

	case ERROR_NESTING_NOT_ALLOWED:
		return(EAGAIN);

	case ERROR_NETNAME_DELETED:
		return(ENOENT);

	case ERROR_NETWORK_ACCESS_DENIED:
		return(EACCES);

	case ERROR_NETWORK_BUSY:
		return(EAGAIN);

	case ERROR_NET_WRITE_FAULT:
		return(EIO);

	case ERROR_NOACCESS:
		return(EFAULT);

	case ERROR_NOT_DOS_DISK:
		return(EACCES);

	case ERROR_NOT_ENOUGH_MEMORY:
		return(ENOMEM);

	case ERROR_NOT_ENOUGH_QUOTA:
		return(ENOMEM);

	case ERROR_NOT_LOCKED:
		return(EACCES);

	case ERROR_NOT_OWNER:
		return(EACCES);

	case ERROR_NOT_READY:
		return(EACCES);

	case ERROR_NOT_SAME_DEVICE:
		return(EXDEV);

	case ERROR_NO_LOG_SPACE:
		return(ENOSPC);

	case ERROR_NO_MEDIA_IN_DRIVE:
		return(ENXIO);

	case ERROR_NO_MORE_DEVICES:
		return(ENOSPC);

	case ERROR_NO_MORE_FILES:
		return(ENOENT);

	case ERROR_NO_PROC_SLOTS:
		return(EAGAIN);

	case ERROR_OPEN_FAILED:
		return(ENOENT);

	case ERROR_OPERATION_ABORTED:
		return(EINTR);

	case ERROR_OUTOFMEMORY:
		return(ENOMEM);

	case ERROR_OUT_OF_PAPER:
		return(EACCES);

	case ERROR_OUT_OF_STRUCTURES:
		return(ENOMEM);

	case ERROR_PARTITION_FAILURE:
		return(EIO);

	case ERROR_PATH_BUSY:
		return(EAGAIN);

	case ERROR_PATH_NOT_FOUND:
		return(ENOENT);

	case ERROR_PIPE_BUSY:
		return(EAGAIN);

	case ERROR_PIPE_NOT_CONNECTED:
		return(EPIPE);

	case ERROR_POSSIBLE_DEADLOCK:
		return(EDEADLOCK);

	case ERROR_PRINTQ_FULL:
		return(ENOSPC);

	case ERROR_PRIVILEGE_NOT_HELD:
		return(EPERM);

	case ERROR_PROC_NOT_FOUND:
		return(ESRCH);

	case ERROR_READ_FAULT:
		return(EACCES);

	case ERROR_RELOC_CHAIN_XEEDS_SEGLIM:
		return(ENOEXEC);

	case ERROR_REM_NOT_LIST:
		return(ENOENT);

	case ERROR_RING2SEG_MUST_BE_MOVABLE:
		return(ENOEXEC);

	case ERROR_RMODE_APP:
		return(ENOEXEC);

	case ERROR_SAME_DRIVE:
		return(EEXIST);

	case ERROR_SECTOR_NOT_FOUND:
		return(EACCES);

	case ERROR_SEEK:
		return(ENXIO);

	case ERROR_SEEK_ON_DEVICE:
		return(EACCES);

	case ERROR_SHARING_BUFFER_EXCEEDED:
		return(EACCES);

	case ERROR_SHARING_PAUSED:
		return(EAGAIN);

	case ERROR_SHARING_VIOLATION:
		return(EACCES);

	case ERROR_STACK_OVERFLOW:
		return(ENOMEM);

	case ERROR_STATIC_INIT:
		return(ENOEXEC);

	case ERROR_SUCCESS:
		return(0);

	case ERROR_SWAPERROR:
		return(EIO);

	case ERROR_SYSTEM_TRACE:
		return(ENOSYS);

	case ERROR_TOO_MANY_MODULES:
		return(EMFILE);

	case ERROR_TOO_MANY_OPEN_FILES:
		return(EMFILE);

	case ERROR_TOO_MANY_TCBS:
		return(ENOSPC);

	case ERROR_WAIT_NO_CHILDREN:
		return(ECHILD);

	case ERROR_WRITE_FAULT:
		return(EACCES);

	case ERROR_WRITE_PROTECT:
		return(EACCES);

	case ERROR_WRONG_DISK:
		return(EACCES);

	default:
		TRACE2(tet_Ttrace, 2,
			"w32err2errno(): no equivalent for error %s",
			tet_i2a(errval));
		return(EINVAL);
	}
}

#else		/* -END-WIN32-CUT- */

int w32err_c_not_used;

#endif		/* -WIN32-CUT-LINE- */

