/*
 *	SCCS: @(#)SystemTC.java	1.1 (99/09/03)
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

import java.lang.*;
import TET.*;

/**
 * Test case class to test <code>java.lang.System</code>.
 *
 * @version     @(#)SystemTC.java	1.1 99/09/03 TETware release 3.8
 * @author      Matthew Hails, UniSoft Ltd.
 */
public class SystemTC extends SimpleTestCase
{
	/*
	 * Name of child test case class.
	 */
	private final static String CHILD_CLASS	= "SysChildTC";

	/*
	 * Value used for exit code by test 1.
	 */
	private final static int T1_VALUE	= 35;

	/**
	 * Entry point for this class. Calls <code>SimpleTestCase.main()</code>
	 * to pass control to TET.
	 *
	 * @param	args	command line arguments.
	 */
	public static void main(String[] args)
	{
		main(args, new SystemTC());
	}

	/**
	 * Test purpose method for <code>System.exit(int)</code>.
	 * Verifies that <code>System.exit(int)</code> terminates the currently
	 * running Java Virtual Machine with status given by the integer
	 * argument.
	 *
 	 * @param	ts	the <code>TestSession</code> object for this
	 *			test run.
	 */
	public void i1t1(TestSession ts)
	{
		long pid;
		int status;
		int expStatus;

		// Fire off a new child process using tet_jspawn().
		try
		{
			pid = ts.tet_jspawn(CHILD_CLASS,
				new String[] { Integer.toString(T1_VALUE) },
				null);
		}
		catch (TetException e)
		{
			ts.tet_infoline("tet_jspawn() failed: " + e);
			ts.tet_result(ts.TET_UNRESOLVED);
			return;
		}

		// Use tet_wait() to wait for the process to complete.
		try
		{
			status = ts.tet_wait(pid);
		}
		catch (TetException e)
		{
			ts.tet_infoline("tet_wait() failed: " + e);
			ts.tet_result(ts.TET_UNRESOLVED);
			return;
		}

		// Verify that the exit status is as expected.
		expStatus = exitValueToStatus(T1_VALUE);
		if (status == expStatus)
		{
			ts.tet_result(ts.TET_PASS);
		}
		else
		{
			ts.tet_infoline("Child exited with status " + status
				+ ", expecting " + expStatus);
			ts.tet_result(ts.TET_FAIL);
		}
	}

	/*
	 * Compute the exit status as would be returned from tet_wait() for a
	 * process which exits with a given value. This is not a universal
	 * portable solution - only works for Win32 and for those UNIX systems
	 * which encode the exit status in the traditional way.
	 *
	 *	value	integer code as passed to exit().
	 *
	 * Returns the exit status corresponding to the given value.
	 */
	private static int exitValueToStatus(int value)
	{
		String os;
		int status;

		os = System.getProperty("os.name", "");
		if (os.toLowerCase().indexOf("windows") >= 0)
			status = value;
		else
			status = ((value & 0377) << 8);

		return status;
	}
}

/**
 * Child part of SystemTC test case. Tests
 * <code>java.lang.System.exit(int)</code>.
 */
class SysChildTC extends ChildTestCase
{
	/**
	 * Entry point for this class. Calls <code>ChildTestCase.main()</code>
	 * to pass control to TET.
	 *
	 * @param	args	command line arguments.
	 */
	public static void main(String[] args)
	{
		main(args, new SysChildTC());
	}

	/**
	 * Run child test case. Calls <code>System.exit(int)</code> with status
	 * passed as first argument. Overrides
	 * <code>ChildTestCase.tet_main()</code>.
	 *
 	 * @param	ts	the <code>TestSession</code> object for this
	 *			test run.
 	 * @param	args	command line arguments as passed from the
	 *			parent process.
	 * @return	0 for success, non-zero on failure.
	 */
	public int tet_main(TestSession ts, String[] args)
	{
		int status;

		// Verify the parent test case passed us one argument, which is
		// the exit status we should use.
		if (args.length != 1)
		{
			ts.tet_infoline("Child received " + args.length
				+ " arguments, expected 1");
			ts.tet_result(ts.TET_UNRESOLVED);
			return 1;
		}

		status = Integer.parseInt(args[0]);

		// Log off TETware.
		ts.tet_logoff();

		// Call System.exit().
		System.exit(status);

		// If we get this far, System.exit() didn't work, but we can't
		// use more TETware functions as we've already called
		// tet_logoff().
		System.err.println("Error in SysChildTC.tet_main():"
			+ " System.exit(int) didn't terminate process");
		return 1;
	}
}
