/*
 *	SCCS: @(#)TetThread.java	1.1 (99/09/03)
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
 * The thread class which test purposes should use when running multi-threaded
 * tests.
 *
 * @version	@(#)TetThread.java	1.1 99/09/03 TETware release 3.8
 * @author	From JETpack 1.02 source
 * @author	Matthew Hails, UniSoft Ltd.
 */
public class TetThread extends Thread
{
	static final String sccsid = "@(#)TetThread.java	1.1 (99/09/03) TETware release 3.8";

	/*
	 * Default value for waitTime field.
	 */
	private static final long DEFWAITTIME = 10000L;

	/*
	 * Number of milliseconds for the TestSession to wait in cleanup code
	 * before interrupting this thread.
	 */
	private long waitTime;

	/**
	 * Create a new thread for use with a TET <code>TestSession</code>.
	 * <p>
	 * Any threads created in a test purpose using this constructor will be
	 * automatically cleaned up by the TCM at the end of the test purpose.
	 *
	 * @param	ts		the <code>TestSession</code> object for
	 *				the current test run.
	 * @param	target		the target.
	 * @param	name		the name of the thread.
	 * @param	waitTime	number of milliseconds for which the
	 *				TCM cleanup code will wait for this
	 *				thread to die after the main thread
	 *				returns.
	 */
	public TetThread(TestSession ts, Runnable target, String name,
		long waitTime)
	{
		super(ts.threadgroup(), target, name);
		this.waitTime = waitTime;
	}

	/**
	 * Create a new thread for use with a TET <code>TestSession</code>.
	 * <p>
	 * Any threads created in a test purpose using this constructor will be
	 * automatically cleaned up by the TCM at the end of the test purpose.
	 *
	 * @param	ts		the <code>TestSession</code> object for
	 *				the current test run.
	 * @param	target		the target.
	 * @param	name		the name of the thread.
	 */
	public TetThread(TestSession ts, Runnable target, String name)
	{
		this(ts, target, name, DEFWAITTIME);
	}

	/**
	 * Create a new thread for use with a TET <code>TestSession</code>.
	 * <p>
	 * Any threads created in a test purpose using this constructor will be
	 * automatically cleaned up by the TCM at the end of the test purpose.
	 *
	 * @param	ts		the <code>TestSession</code> object for
	 *				the current test run.
	 * @param	target		the target.
	 * @param	waitTime	number of milliseconds for which the
	 *				TCM cleanup code will wait for this
	 *				thread to die after the main thread
	 *				returns.
	 * @see		TetThread#TetThread(TestSession, Runnable, String, long)
	 */
	public TetThread(TestSession ts, Runnable target, long waitTime)
	{
		super(ts.threadgroup(), target);
		this.waitTime = waitTime;
	}

	/**
	 * Create a new thread for use with a TET <code>TestSession</code>.
	 * <p>
	 * Any threads created in a test purpose using this constructor will be
	 * automatically cleaned up by the TCM at the end of the test purpose.
	 *
	 * @param	ts		the <code>TestSession</code> object for
	 *				the current test run.
	 * @param	target		the target.
	 * @see		TetThread#TetThread(TestSession, Runnable, String)
	 */
	public TetThread(TestSession ts, Runnable target)
	{
		this(ts, target, DEFWAITTIME);
	}

	/**
	 * Create a new thread for use with a TET <code>TestSession</code>.
	 * <p>
	 * Any threads created in a test purpose using this constructor will be
	 * automatically cleaned up by the TCM at the end of the test purpose.
	 *
	 * @param	ts		the <code>TestSession</code> object for
	 *				the current test run.
	 * @param	name		the name of the thread.
	 * @param	waitTime	number of milliseconds for which the
	 *				TCM cleanup code will wait for this
	 *				thread to die after the main thread
	 *				returns.
	 * @see		TetThread#TetThread(TestSession, Runnable, String, long)
	 */
	public TetThread(TestSession ts, String name, long waitTime)
	{
		super(ts.threadgroup(), name);
		this.waitTime = waitTime;
	}

	/**
	 * Create a new thread for use with a TET <code>TestSession</code>.
	 *
	 * @param	ts	the <code>TestSession</code> with which the
	 *			thread is being used.
	 * @param	name	the name of the thread.
	 * @see		TetThread#TetThread(TestSession, Runnable, String)
	 */
	public TetThread(TestSession ts, String name)
	{
		this(ts, name, DEFWAITTIME);
	}

	/**
	 * Create a new thread. It is recommended that all instances are
	 * created using one of the constructors which take a
	 * <code>TestSession</code> argument.
	 *
	 * @param	group	the thread group.
	 * @param	target	the target.
	 * @param	name	the name of the thread.
	 */
	public TetThread(ThreadGroup group, Runnable target, String name)
	{
		super(group, target, name);
		waitTime = DEFWAITTIME;
	}

	/**
	 * Create a new thread. See recommendations for
	 * <code>TetThread.TetThread(ThreadGroup, Runnable, String)</code>.
	 *
	 * @param	group	the thread group.
	 * @param	target	the target.
	 * @see		TetThread#TetThread(ThreadGroup, Runnable, String)
	 */
	public TetThread(ThreadGroup group, Runnable target)
	{
		super(group, target);
		waitTime = DEFWAITTIME;
	}

	/**
	 * Create a new thread. See recommendations for
	 * <code>TetThread.TetThread(ThreadGroup, Runnable, String)</code>.
	 *
	 * @param	group	the thread group.
	 * @param	name	the name of the thread.
	 * @see		TetThread#TetThread(ThreadGroup, Runnable, String)
	 */
	public TetThread(ThreadGroup group, String name)
	{
		super(group, name);
		waitTime = DEFWAITTIME;
	}

	/*
	 * Retrieve the amount of time this thread would like to be given to
	 * finish after the main thread has completed.
 	 *
	 * @return	the number of milliseconds to wait for this thread to
	 *		finish before interrupting it.
	 */
	long getWaitTime()
	{
		return waitTime;
	}
}
