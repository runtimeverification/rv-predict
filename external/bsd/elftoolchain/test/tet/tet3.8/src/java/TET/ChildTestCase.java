/*
 *	SCCS: @(#)ChildTestCase.java	1.2 (03/04/03)
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
 * <code>ChildTestCase</code> is a base class for child test cases;
 * that is: those executed by calls to <code>tet_spawn()</code>
 * or <code>tet_exec()</code>.
 *
 * <p>
 * To create a new child test case, derive a new class from
 * <code>ChildTestCase</code> and implement the <code>tet_main()</code> method.
 *
 * <p>
 * Attempts to load this class in the same virtual machine as
 * <code>SimpleTestCase</code> will fail. It is only permissible to have one
 * type of test case in one virtual machine.
 *
 * @version	@(#)ChildTestCase.java	1.2 03/04/03 TETware release 3.8
 * @author	Matthew Hails, UniSoft Ltd.
 */
public abstract class ChildTestCase extends TestCase
{
	static final String sccsid = "@(#)ChildTestCase.java	1.2 (03/04/03) TETware release 3.8";
	
	/**
	 * Begin execution of a child test case using the default test session.
	 * The name of the test case is taken from the class of the
	 * <code>tc</code> argument.
	 *
	 * @param	args	the command line arguments.
	 * @param	tc	the testcase.
	 */
	public static void main(String[] args, ChildTestCase tc)
	{
		main(tc.getClassName(), args, tc);
	}

	/**
	 * Begin execution of a test case using the default test session.
	 *
	 * @param	pname	the name of the test case.
	 * @param	args	the command line arguments.
	 * @param	tc	the testcase.
	 */
	public static void main(String pname, String[] args, ChildTestCase tc)
	{
		tc.childMain(TestSession.createTestSession(pname, "japichild"),
			makeArgList(pname, args));
	}

	/*
	 * Pass control to the child process controller. Calls the C function
	 * tet_tcmchild_main(). Control re-enters Java code in runTetMain().
	 * This method does not return.
	 *
	 *	ts	TestSession object for this test run.
	 *	args	Command line arguments passed from parent.
	 */
	private native void childMain(TestSession ts, String[] args);

	/*
	 * Run child test case main function.
	 *
	 *	ts	TestSession object for this test run.
	 *	args	Command line arguments passed from parent.
	 *
	 * Returns result of child test case, or non-zero if an exception was
	 * caught during its execution.
	 */
	final int runTetMain(TestSession ts, String[] args)
	{
		int rv;

		ts.initThreads("childTC", false);

		try
		{
			rv = tet_main(ts, args);
		}
		catch (Exception e)
		{
			ts.reportError(e, ts.TET_UNRESOLVED,
				"Caught exception in child test case");
			rv = 1;
		}

		ts.cleanThreads();

		return rv;
	}

	/**
	 * Run child test case. Implementations of this method should
	 * either return, or exit using <code>ts.tet_exit()</code>.
	 *
 	 * @param	ts	the <code>TestSession</code> object for this
	 *			test run.
 	 * @param	args	command line arguments as passed from the
	 *			parent process.
	 * @return	0 for success, non-zero on failure.
	 */
	public abstract int tet_main(TestSession ts, String[] args);
}
