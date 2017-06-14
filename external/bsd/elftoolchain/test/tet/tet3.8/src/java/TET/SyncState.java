/*
 *	SCCS: @(#)SyncState.java	1.1 (99/09/03)
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

/**
 * The SyncState class gives the status of one system participating in a
 * synchronization event.
 *
 * @version	@(#)SyncState.java	1.1 99/09/03 TETware release 3.8
 * @author	From JETpack 1.02 source
 * @author	Matthew Hails, UniSoft Ltd.
 */
public class SyncState
{
	static final String sccsid = "@(#)SyncState.java	1.1 (99/09/03) TETware release 3.8";

	/* ===================== Values for tsy_state ====================== */

	/**
	 * Value for <code>tsy_state</code>, indicating that the
	 * synchronization request was not received.
	 */
	public static final int TET_SS_NOTSYNCED = 1;

	/**
	 * Value for <code>tsy_state</code>, indicating that the system voted
	 * YES.
	 */
	public static final int TET_SS_SYNCYES   = 2;

	/**
	 * Value for <code>tsy_state</code>, indicating that the system voted
	 * NO.
	 */
	public static final int TET_SS_SYNCNO    = 3;

	/**
	 * Value for <code>tsy_state</code>, indicating that the system timed
	 * out.
	 */
	public static final int TET_SS_TIMEDOUT  = 4;

	/**
	 * Value for <code>tsy_state</code>, indicating that the process
	 * exited.
	 */
	public static final int TET_SS_DEAD      = 5;

	/* ======================= Public data items ======================= */

	/**
	 * System ID.
	 */
	public int tsy_sysid;

	/**
	 * State of synchronization. Consists of a bit mask of TET_SS_..
	 * values.
	 */
	public int tsy_state;

	/* ========================= Constructors ========================== */

	/**
	 * Constructs a new <code>SyncState</code> and initializes it.
	 *
	 * @param	sysid	the system ID.
	 * @param	state	state of synchronization.
	 */
	SyncState(int sysid, int state)
	{
		tsy_sysid = sysid;
		tsy_state = state;
	}
}
