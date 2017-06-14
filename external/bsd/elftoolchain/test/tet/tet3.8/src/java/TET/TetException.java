/*
 *	SCCS: @(#)TetException.java	1.1 (99/09/03)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1999 The Open Group
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * Motif, OSF/1, UNIX and the "X" device are registered trademarks and
 * IT DialTone and The Open Group are trademarks of The Open Group in
 * the US and other countries.
 *
 * X/Open is a trademark of X/Open Company Limited in the UK and other
 * countries.
 *
 */

package TET;

import java.lang.*;

/**
 * An exception thrown by a TestSession object.
 *
 * @version	@(#)TetException.java	1.1 99/09/03 TETware release 3.8
 * @author	From JETpack 1.02 source
 * @author	Matthew Hails, UniSoft Ltd.
 */
public class TetException extends Exception
{
	static final String sccsid = "@(#)TetException.java	1.1 (99/09/03) TETware release 3.8";

	/**
	 * The value of <code>tet_errno</code> when this exception occurred.
	 */
	public int tet_errno;

	/**
	 * The value of <code>errno</code> in native code when this exception
	 * occurred. Will always be intitialized to 0, since this is now
	 * deprecated.
	 *
	 * @deprecated	Not part of the published TETware API.
	 */
	public int errno;

	/**
	 * The value of <code>tet_tcerrno</code> when this exception occurred.
	 * Will always be initialized to 0, since this is now deprecated.
	 *
	 * @deprecated	Not part of the published TETware API.
	 */
	public int tet_tcerrno;

	/**
	 * The value of <code>tet_sderrno</code> when this exception occurred.
	 * Will always be intitialized to 0, since this is now deprecated.
	 *
	 * @deprecated	Not part of the published TETware API.
	 */
	public int tet_sderrno;

	/**
	 * The value of <code>tet_xderrno</code> when this exception occurred.
	 * Will always be initialized to 0, since this is now deprecated.
	 *
	 * @deprecated	Not part of the published TETware API.
	 */
	public int tet_xderrno;

	/**
	 * The sync point at which this exception occurred.
	 */
	public long syncpt;

	/**
	 * The state of synchronisation when this exception occurred.
	 */
	public SyncState[] sync_state;

	/**
	 * Constructs a new <code>TetException</code>.
	 *
	 * @param	te	the value of <code>tet_errno</code>.
	 * @param	msg	the detail message for this exception.
	 * @param	spt	the current sync point.
	 * @param	states	the current state of synchronisation.
	 */
	TetException(int te, String msg, long spt, SyncState[] state)
	{
		super((msg == null) ? "(TET exception " + te + ")" : msg);

		this.errno = 0;
		this.tet_errno = te;
		this.tet_tcerrno = 0;
		this.tet_sderrno = 0;
		this.tet_xderrno = 0;
		this.syncpt = spt;
		this.sync_state = state;
	}
}
