/*
 *	SCCS: @(#)TestSession.java	1.1 (99/09/03)
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
import java.util.*;
import java.io.*;

/**
 * The <code>TestSession</code> class provides TETware API services to Java
 * test cases.
 *
 * @version	@(#)TestSession.java	1.1 99/09/03 TETware release 3.8
 * @author	From JETpack 1.02 source
 * @author	Matthew Hails, UniSoft Ltd.
 */
public final class TestSession
{
	static final String sccsid = "@(#)TestSession.java	1.1 (99/09/03) TETware release 3.8";

	/* --------------------- Implementation notes -------------------------
	 *
	 * This class is essentially a wrapper around calls to the TETware C
	 * TCM and API. The native method calls access a shared library
	 * containing the C TCM and API code. Because of the differing
	 * threading packages used in Java implementations, the non-thread safe
	 * C API is used, and the Java code prevents simultaneous access by
	 * multiple threads. Only one instance of this class can exist, and
	 * the implicit instance synchronization object is used to control
	 * access to the API and TCM services. Hence all native methods which
	 * call functions or set variables in the TETware C API or TCM must be
	 * declared synchronized.
	 *
	 * --------------------------------------------------------------------
	 */

	// -------------------- Standard result codes -------------------------

	/**
	 * Result code for PASS result.
	 */
	public static final int TET_PASS	= 0;

	/**
	 * Result code for FAIL result.
	 */
	public static final int TET_FAIL	= 1;

	/**
	 * Result code for UNRESOLVED result.
	 */
	public static final int TET_UNRESOLVED	= 2;

	/**
	 * Result code for NOTINUSE result.
	 */
	public static final int TET_NOTINUSE	= 3;

	/**
	 * Result code for UNSUPPORTED result.
	 */
	public static final int TET_UNSUPPORTED	= 4;

	/**
	 * Result code for UNTESTED result.
	 */
	public static final int TET_UNTESTED	= 5;

	/**
	 * Result code for UNINITIATED result.
	 */
	public static final int TET_UNINITIATED	= 6;

	/**
	 * Result code for NORESULT result.
	 */
	public static final int TET_NORESULT	= 7;

	// ------------------------ Sync votes --------------------------------

	/**
	 * Synchronisation "yes" vote.
	 */
	public static final int TET_SV_YES	= 1;

	/**
	 * Synchronisation "no" vote.
	 */
	public static final int TET_SV_NO	= 2;

	// ---------------------- tet_errno values ----------------------------

	/**
 	 * <code>tet_errno</code> value for ok, success.
	 */
	public static final int TET_ER_OK		= 0;

	/**
	 * <code>tet_errno</code> value for general error code.
	 */
	public static final int TET_ER_ERR		= 1;

	/**
	 * <code>tet_errno</code> value for a bad magic number.
	 */
	public static final int TET_ER_MAGIC		= 2;

	/**
	 * <code>tet_errno</code> value for not logged on.
	 */
	public static final int TET_ER_LOGON		= 3;

	/**
	 * <code>tet_errno</code> value for a receive message error.
	 */
	public static final int TET_ER_RCVERR		= 4;

	/**
	 * <code>tet_errno</code> value for an unknown request code.
	 */
	public static final int TET_ER_REQ		= 5;

	/**
	 * <code>tet_errno</code> value for request timed out.
	 */
	public static final int TET_ER_TIMEDOUT		= 6;

	/**
	 * <code>tet_errno</code> value for request contained duplicate IDs.
	 */
	public static final int TET_ER_DUPS		= 7;

	/**
	 * <code>tet_errno</code> value for sync completed unsuccessfully.
	 */
	public static final int TET_ER_SYNCERR		= 8;

	/**
	 * <code>tet_errno</code> value for invalid request parameter.
	 */
	public static final int TET_ER_INVAL		= 9;

	/**
	 * <code>tet_errno</code> value for tracing not configured.
	 */
	public static final int TET_ER_TRACE		= 10;

	/**
	 * <code>tet_errno</code> value for process not terminated.
	 */
	public static final int TET_ER_WAIT		= 11;

	/**
	 * <code>tet_errno</code> value for bad xrid in xresd request.
	 */
	public static final int TET_ER_XRID		= 12;

	/**
	 * <code>tet_errno</code> value for bad snid in syncd request.
	 */
	public static final int TET_ER_SNID		= 13;

	/**
	 * <code>tet_errno</code> value for sysid not in system name list.
	 */
	public static final int TET_ER_SYSID		= 14;

	/**
	 * <code>tet_errno</code> value for event in progress.
	 */
	public static final int TET_ER_INPROGRESS	= 15;

	/**
	 * <code>tet_errno</code> value for event finished or already happened.
	 */
	public static final int TET_ER_DONE		= 16;

	/**
	 * <code>tet_errno</code> value for request out of context.
	 */
	public static final int TET_ER_CONTEXT		= 17;

	/**
	 * <code>tet_errno</code> value for priv request/kill error.
	 */
	public static final int TET_ER_PERM		= 18;

	/**
	 * <code>tet_errno</code> value for can't fork.
	 */
	public static final int TET_ER_FORK		= 19;

	/**
	 * <code>tet_errno</code> value for no such file or directory.
	 */
	public static final int TET_ER_NOENT		= 20;

	/**
	 * <code>tet_errno</code> value for no such process.
	 */
	public static final int TET_ER_PID		= 21;

	/**
	 * <code>tet_errno</code> value for a bad signal number.
	 */
	public static final int TET_ER_SIGNUM		= 22;

	/**
	 * <code>tet_errno</code> value for a bad file id.
	 */
	public static final int TET_ER_FID		= 23;

	/**
	 * <code>tet_errno</code> value for a server internal error.
	 */
	public static final int TET_ER_INTERN		= 24;

	/**
	 * <code>tet_errno</code> value for abort TCM on TP end.
	 */
	public static final int TET_ER_ABORT		= 25;

	/**
	 * <code>tet_errno</code> value for argument list too long.
	 */
	public static final int TET_ER_2BIG		= 26;

	// -------------------- Private fields --------------------------------

	/*
	 * Length of time to wait for each thread to complete, in milliseconds.
	 */
	private static long THREADTIMEOUT = 5000;

	/*
	 * Number of instances of this class.
	 */
	private static int nInstances = 0;

	/*
	 * The list of error strings which describe each value of tet_errno.
	 */
	private String[] errList;

	/*
	 * The value of TET_ROOT in the current environment.
	 */
	private String tet_root;

	/*
	 * Table of per-thread data.
	 * (Can use java.lang.ThreadLocal when update to Java 1.2).
	 */
	private Hashtable threadDataTable;

	/*
	 * Next block number.
	 */
	private long nextBlock;

	/*
	 * Copy of tet_pname. This copy is kept here, rather than making a
	 * native method call, to speed up access.
	 */
	private String pname;

	/*
	 * Current ThreadGroup when executing a test purpose. It is null when
	 * not executing a test purpose.
	 */
	private ThreadGroup tgroup;

	// --------------------- Creating new objects -------------------------

	/*
	 * Constructs a new TestSession.
	 *
	 *	pname	Value to use for program name in TET API.
	 */
	private TestSession(String pname)
	{
		this.errList = getErrList();
		this.tet_root = getenv("TET_ROOT");
		this.threadDataTable = new Hashtable();
		this.nextBlock = 0;
		this.pname = pname;
		this.tgroup = null;
	}

	/*
	 * Create a new TestSession object. All constructors are private,
	 * and this method is the only way to create a TestSession object.
	 * Only one TestSession object can exist at once, any attempt to
	 * create a second instance will result in an Error being thrown.
	 *
	 *	pname		Value to use for program name in TET API.
	 *	libname		Name of library holding native code.
	 *
	 * Returns a new TestSession object.
	 */
	static synchronized TestSession createTestSession(String pname,
		String libname)
	{
		// Verify there are no other instances of this class.
		if (nInstances++ > 0)
			throw new TetError("Illegal attempt to create more"
				+ " than one instance of class"
				+ " TET.TestSession");

		// Load the shared library containing the implementations of
		// the native methods.
		System.loadLibrary(libname);

		// Create a new instance and return it.
		return new TestSession(pname);
	}

	/*
	 * Get a value from the environment.
	 *
	 * 	name	Name of environment variable of interest.
	 *
	 * Returns a new String object to the value corresponding to name in
	 * the current environment, or null otherwise.
	 */
	private native String getenv(String name);

	// -------------------- TETware API methods ---------------------------

	/**
	 * Retrieves the absolute test number for the currently executing test
	 * purpose. This is 0 when in the startup and cleanup routines.
	 *
	 * @return	the current test number.
	 */
	public native int tet_thistest();

	/**
	 * Retrieves the test case name.
	 *
	 * @return	the test case name.
	 */
	public String tet_pname()
	{
		return this.pname;
	}

	/**
	 * Retrieves the list of error strings which describe each value of
	 * <code>tet_errno</code>.
	 *
	 * @return	the list of error strings.
	 */
	public String[] tet_errlist()
	{
		return this.errList;
	}

	/*
	 * Retrieves the C API tet_errlist as an array of String objects.
	 */
	private native String[] getErrList();

	/**
	 * Sets the current context to the value of the current process ID, and
	 * resets the block and sequence numbers to 1.
	 */
	public synchronized native void tet_setcontext();

	/**
	 * Increments the current block ID.
	 */
	public void tet_setblock()
	{
		newBlock(getThreadData());
	}

	/**
	 * Retrieves the current TET block number.
	 *
	 * @return	the current TET block number.
	 * @deprecated	Not part of the published API.
	 */
	public int tet_getblock()
	{
		return (int)getThreadData().block;
	}

	/**
	 * Prints a line to the execution results file.
 	 *
	 * @param	line	line to print to the results file.
	 */
	public synchronized native void tet_infoline(String line);

	/**
	 * Prints a group of lines to the execution results file. In
	 * Distributed TETware these lines are printed using a single operation
	 * which guarantees that lines from other test case parts do not appear
	 * in between lines printed by one call to this function.
 	 *
	 * @param	lines	group of lines to print to the results file.
	 * @exception	TetException	if there was an error printing the
	 * 				lines.
	 */
	public synchronized native void tet_minfoline(String[] lines)
							throws TetException;

	/**
	 * Informs the API of the result of the current test purpose.
	 *
	 * @param	result	the result code.
	 */
	public synchronized native void tet_result(int result);

	/**
	 * Mark the test purpose specified by testno as cancelled.
	 *
	 * @param	testno	absolute test number of the test purpose to be
	 *			cancelled.
	 * @param	reason	text describing the reason why the test purpose
	 *			is to be marked as cancelled. May be NULL, in
	 * 			which case the specified test purpose is
	 *			reactivated if it has previously been marked as
	 *			cancelled.
	 */
	public synchronized native void tet_delete(int testno, String reason);

	/**
 	 * Retrieves the reason for test purpose cancellation.
	 *
	 * @param	testno	absolute test number of the test purpose.
	 * @return	a string describing the reason for test purpose
	 * 		cancellation, or <code>null</code> if this test purpose
	 *		is not marked as cancelled.
	 */
	public synchronized native String tet_reason(int testno);

	/**
	 * Gets the value of a configuration variable.
	 *
	 * @param	name	name of the configuration variable.
	 * @return	the value of the specified configuration variable. If
	 *		the variable specified is defined but has no setting,
	 * 		a <code>String</code> object of zero length is
	 *		returned. If the variable is undefined,
	 *		<code>null</code> is returned.
	 */
	public synchronized native String tet_getvar(String name);

	/**
	 * Creates a new subprogram. The subprogram launched must be built with
	 * the child process controller.
	 *
	 * @param	file	path of the subprogram to execute.
	 * @param	argv	argument array to pass to new process.
	 * @param	envp	environment data to pass to new process. If
	 *			this is <code>null</code> or has length 0, the
	 *			current environment is used.
	 * @return	the process ID of the newly created process.
	 * @exception	TetException	if there is an error launching the
	 *				child process.
	 */
	public long tet_spawn(String file, String[] argv, String[] envp)
							throws TetException
	{
		if (envp == null)
			envp = new String[0];

		return spawn(file, argv, envp);
	}

	/**
	 * Creates a new subprogram using a Java class. The class must be a
	 * subclass of ChildTestCase.
	 *
	 * @param	classname	name of the Java class to execute in a
	 *				new process.
	 * @param	args		argument array to pass to
	 *				<code>main()</code> method of the class
	 *				N.B. <code>args[0]</code> is the first
	 *				actual argument, not the name of the
	 *				interpreter or the name of the class.
	 * @param	envp		environment data to pass to new
	 *				process. If this is <code>null</code>
	 *				or has length 0, the current
	 *				environment is used.
	 * @return	the process ID of the newly created process.
	 * @exception	TetException	if there is an error launching the
	 *				child process.
	 */
	public long tet_jspawn(String classname, String[] args, String[] envp)
							throws TetException
	{
		String msg;
		int len;
		String[] newargv;

		// Verify we had no problems determining the value of TET_ROOT
		// in the environment.
		if (this.tet_root == null)
		{
			msg = "Unable to determine value of TET_ROOT in"
				 + " environment";
			tet_error(0, msg);
			throw new TetException(TET_ER_ERR, msg, 0L, null);
		}

		// Construct new argument list using spawn tool and magic
		// value. The spawn tool will rearrange the arguments to deal
		// with the extra arguments added by the C API.
		if (args == null)
			len = 0;
		else
			len = args.length;

		newargv = new String[3 + len];
		newargv[0] = this.tet_root + File.separator + "bin"
			+ File.separator + "jet-spawn";
		newargv[1] = "TET_JAVA_SPAWN_MAGIC";
		newargv[2] = classname;

		if (len > 0)
			System.arraycopy(args, 0, newargv, 3, len);

		// Use regular tet_spawn() to do the work.
		return tet_spawn(newargv[0], newargv, envp);
	}

	/*
	 * Wrapper around the C API function tet_spawn().
	 */
	private synchronized native long
		spawn(String file, String[] argv, String[] envp)
							throws TetException;

	/**
	 * Waits for a process to terminate.
	 *
	 * @param	pid	process ID of the process to wait for.
	 * @return	the exit status of the process.
	 * @exception	TetException	if the wait is unsuccessful.
	 */
	public synchronized native int tet_wait(long pid) throws TetException;

	/**
	 * Sends a signal to a process.
	 *
	 * @param	pid	process ID of the victim process.
	 * @param	sig	number of the signal to send. Note that this is
	 * 			ignored on a Windows NT system.
	 * @exception	TetException	the kill was unsuccessful.
	 */
	public synchronized native void tet_kill(long pid, int sig)
							throws TetException;

	/**
	 * Synchronises with one or more of the other systems in a distributed
	 * test case.
	 *
	 * @param	syncptno	the sync point number to which the
	 *				calling system wishes to synchronise.
	 *				If syncptno is zero, a successful call
	 *				to <code>tet_remsync()</code> returns
	 *				as soon as all participating systems
	 *				have synchronised to the next sync
	 *				point.
	 * @param	sysnames	a list of IDs of the other systems with
	 *				which to synchronise. The system ID of
	 *				the calling system is ignored if it
	 *				appears in the list.
	 * @param	waittime	the number of seconds that may elapse
	 *				between synchronisation requests from
	 *				other participating systems before the
	 *				calling system times out.
	 * @param	vote		how the calling system wishes to vote
	 *				in the synchronisation event. This
	 *				should be TET_SV_YES or TET_SV_NO.
	 * @param	msg		used to exchange sync message data with
	 *				other participating systems.
	 * @exception	TetException	if this is TETware-Lite, or if an error
	 * 				occurs during the sync.
	 * @see		SyncMessage
	 */
	public synchronized native void tet_remsync(long syncptno,
		int[] sysnames, int waittime, int vote, SyncMessage msg)
							throws TetException;

	/**
	 * Gets a list of the other systems participating in the test case.
	 *
	 * @return	list of IDs of the other systems participating in the
	 *		test case. Returns <code>null</code> in TETware-Lite or
	 *		for a test case in which no other systems are
	 *		participating.
	 */
	public synchronized native int[] tet_remgetlist();

	/**
	 * Gets the system ID of the system on which the calling process is
	 * executing.
	 *
	 * @return	the system ID of the current system.
	 */
	public synchronized native int tet_remgetsys();

	/**
	 * Retrieves information about a system participating in the test.
	 *
	 * @param	sysid	ID of the system of interest.
	 * @return	the entry for the requested system.
	 * @exception	TetException	if this is TETware-Lite, or if an error
	 * 				occurs retrieving the entry for the
	 *				specified system.
	 * @see		SystemEntry
	 */
	public SystemEntry tet_getsysbyid(int sysid) throws TetException
	{
		return new SystemEntry(sysid, getsysname(sysid));
	}

	/*
	 * Wrapper around the C API function tet_getsysbyid(), although only
	 * returns the name and not the system ID passed in.
	 */
	private synchronized native String getsysname(int sysid)
							throws TetException;

	/**
	 * Obtains the system time on another system.
	 *
 	 * @param	sysid	ID of the system of interest.
	 * @return	the system time on the system of interest.
	 * @exception	TetException	if this is TETware-Lite, or if an error
	 * 				occurs retrieving the system time of
	 *				the specified system.
	 */
	public Date tet_remtime(int sysid) throws TetException
	{
		return new Date(remtime(sysid) * 1000);
	}

	/*
	 * Wrapper around the C API function tet_remtime().
	 */
	private synchronized native long remtime(int sysid)
							throws TetException;

	/**
	 * Exit function for child process. This should be used instead of
	 * <code>java.lang.System.exit()</code> by child processes created by
	 * <code>tet_spawn()</code>. In Distribute TETware this method logs off
	 * all TETware servers, then exits with the specified status.
	 *
	 * @param	status	exit status.
 	 */
	public synchronized native void tet_exit(int status);

	/**
	 * Log off TETware. This may be called by a child process that is
	 * started by a call to <code>tet_spawn()</code> which does not need to
	 * make any further TETware API calls and is not able to call
	 * <code>tet_exit()</code> at process termination. This should only be
	 * called once from the child process. In Distributed TETware, the
	 * results are undefined if a process or any of its descendents makes
	 * any TETware API calls after tet_logoff() is called.
	 */
	public synchronized native void tet_logoff();

	// --------------- Error reporting within package ---------------------

	/**
	 * Print API messages to the standard channel.
	 *
	 * @param	errnum	error number. This may be +ve to report a Unix
	 *			<code>errno</code> value, or -ve to report a
	 *			DTET server error reply code or API error code.
	 *			N.B. This means that to report
	 *			<code>tet_errno</code> values, they must be
	 *			negated:
	 *			<code>tet_error(-tet_errno, msg)</code>.
	 * @param	msg	error message.
	 */
	void tet_error(int errnum, String msg)
	{
		tet_merror(errnum, new String[] { msg });
	}

	/**
	 * Print API messages to the standard channel.
	 *
	 * @param	errnum	error number (see <code>tet_error()</code>).
	 * @param	msgs	list of error messages. In Distributed
	 *			TETware the messages are sent to XRESD for
	 *			printing using an operation which ensures that
	 *			messages from other systems don't get mixed up
	 *			with these messages.
	 */
	synchronized native void tet_merror(int errnum, String[] msgs);

	/**
	 * Report an error to the execution results file. This is a convenience
	 * method to simplify the printing of Exceptions/Errors.
	 *
	 * @param	t		Object thrown by a test method, or
	 *				<code>null</code> if the error
	 *				condition did not result from an object
	 *				being thrown.
	 * @param	rescode		Result code to use, if in the
	 *				appropriate context.
	 * @param	msg		Text of an error message.
	 */
	void reportError(Throwable t, int rescode, String msg)
	{
		PrintWriter writer;

		if (t == null)
		{
			// Simply write error message to results file.
			tet_error(0, msg);
		}
		else if (t instanceof Error)
		{
			// Re-throw any objects which are Errors.
			throw (Error)t.fillInStackTrace();
		}
		else
		{
			// Print a stack trace of the exception to the journal.
			writer = new PrintWriter(new JournalWriter(this, msg));
			t.printStackTrace(writer);
			writer.flush();
		}

		// If we're in a test purpose, set the result code.
		if (tet_thistest() != 0)
			tet_result(rescode);
	}

	// -------------------- Threading control -----------------------------

	/**
	 * Initialize threads prior to calling a user-supplied test method.
	 * This routine initializes the TestSession's thread group to a new
	 * thread group and resets the thread-specific data.
	 *
	 * @param	name		Name of new thread group. The prefix
	 *				<code>TET:<i>tet_pname</i>:</code> is
	 *				prepended to this when creating the new
	 *				group.
	 * @param	resetBlock	If <code>true</code>, the current block
	 *				number and the unique next block number
	 *				are reset. Otherwise, both are set from
	 *				the current block number. The sequence
	 *				number is always reset.
	 */
	void initThreads(String name, boolean resetBlock)
	{
		// Ensure any existing thread group has been cleaned up.
		cleanThreads();

		// Create a new thread group for any new threads which are
		// created.
		this.tgroup = new ThreadGroup("TET:" + tet_pname() + ":"
			+ name);

		// Reset thread data for all threads, and set block and
		// sequence numbers as required.
		if (resetBlock)
			this.nextBlock = 0;
		else
			this.nextBlock = getThreadData().block - 1;

		// Clear thread data table and initialize data for current
		// thread.
		threadDataTable.clear();
		getThreadData();
	}

	/**
	 * Clean up after a test method. This routine waits for the completion
	 * of all threads in the thread group started by the most recent
	 * initThreads() call. This will be all TetThreads created using a
	 * constructor taking a TestSession argument. If one or more of the
	 * threads does not finish within a certain time interval then this
	 * method exits using System.exit().
	 */
	void cleanThreads()
	{
		int nThreads;
		Thread[] list;
		int i;
		long waitTime;

		// If the thread group field for this object is null, then
		// the threads have already been cleaned up and there is
		// nothing to be done.
		if (this.tgroup == null)
			return;

		// Clean up any threads which were created in this
		// TestSession's current thread group.
		list = new Thread[this.tgroup.activeCount()];
		nThreads = this.tgroup.enumerate(list, false);

		for (i = 0; i < nThreads; i++)
		{
			if (list[i] == Thread.currentThread())
				continue;

			// Wait for the required interval for the
			// thread to finish.
			if (list[i] instanceof TetThread)
				waitTime = ((TetThread)list[i]).getWaitTime();
			else
				waitTime = 0L;

			try
			{
				list[i].join(waitTime);
			}
			catch (InterruptedException e) {}

			// If it's now finished, go to next thread.
			if (!list[i].isAlive())
				continue;

			// Interrupt thread in case it's waiting.
			list[i].interrupt();

			// Wait a little longer for it to finish.
			try
			{
				list[i].join(THREADTIMEOUT);
			}
			catch (InterruptedException e) {}

			// If the thread is still alive, this is considered a
			// fatal error. Note that Thread.stop() is not a safe
			// way to terminate threads and is not recommended.
			if (list[i].isAlive())
 				throw new TetError(
					"Wait failed for one or more threads");
		}

		this.tgroup = null;
	}

	/**
	 * Retrieve the current thread group.
	 *
	 * @return	the current thread group. This is <code>null</code> if
	 *		the object is not currently executing a test purpose
	 *		method or startup/cleanup method.
	 */
	ThreadGroup threadgroup()
	{
		return this.tgroup;
	}

	/*
	 * Get the thread-specific data for the current thread.
	 */
	ThreadData getThreadData()
	{
		Thread curThread;
		ThreadData data;

		curThread = Thread.currentThread();

		// Attempt to retrieve the thread-specific data corresponding
		// to the current thread.
		data = (ThreadData)threadDataTable.get(curThread);
		if (data == null)
		{
			// No ThreadData object exists for this thread, so
			// create it here.
			data = new ThreadData();
			newBlock(data);
			threadDataTable.put(curThread, data);
		}

		return data;
	}

	/*
	 * Start a new block.
	 *
	 *	data	Per-thread data for the thread in which we are starting
	 *		the new block.
	 */
	private synchronized void newBlock(ThreadData data)
	{
		data.block = ++this.nextBlock;
		data.sequence = 1;
	}
}
