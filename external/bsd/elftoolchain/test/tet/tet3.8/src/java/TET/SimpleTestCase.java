/*
 *	SCCS: @(#)SimpleTestCase.java	1.3 (05/12/09)
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
import java.lang.reflect.*;
import java.util.*;

/**
 * <code>SimpleTestCase</code> is a base class for TETware test cases.
 *
 * <p>
 * To create a new test case, derive a new class from
 * <code>SimpleTestCase</code>. Test purposes are defined as public instance
 * methods of the form:
 *
 * <blockquote>
 * <pre>
 * public void i#t#(TestSession ts)
 * </pre>
 * </blockquote>
 *
 * Where each '#' represents a sequence of one or more digits. The first
 * sequence of digits (the "i"-string) denotes the number of the invocable
 * component to which the test purpose belongs. The second sequence (the
 * "t"-string) is used to determine the execution order of the test purposes,
 * AKA the test purpose number. Note that the sequence of digits is
 * <bold>not</bold> used as the test purpose number, but as a comparison to
 * other methods in the same IC.
 *
 * <p>
 * A test purpose method may throw exceptions, but these will result in an
 * unresolved result code being marked for that test.
 *
 * <p>
 * The <code>startup()</code> and <code>cleanup()</code> methods can be
 * overridden to provide code which is executed before and after all test
 * purpose methods.
 *
 * <p>
 * Attempts to load this class in the same virtual machine as
 * <code>ChildTestCase</code> will fail. It is only permissible to have one
 * type of test case in one virtual machine.
 *
 * @version	@(#)SimpleTestCase.java	1.3 05/12/09 TETware release 3.8
 * @author	From JETpack 1.02 source
 * @author	Matthew Hails, UniSoft Ltd.
 */
public abstract class SimpleTestCase extends TestCase
{
	static final String sccsid = "@(#)SimpleTestCase.java	1.3 (05/12/09) TETware release 3.8";

	/* --------------------- Implementation notes -------------------------
	 *
	 * The current naming convention for test purpose methods could be made
	 * more flexible by making getICofTP() and compareTPs() public methods.
	 * By overriding these two methods a test case writer could then
	 * enforce an alternative system for determining which methods are
	 * test purpose methods and which IC they belong to.
	 *
	 * --------------------------------------------------------------------
	 */

	/*
	 * Test case operations. Values for `op' parameter to doTCOp().
	 */	 
	private static final int TET_GETMAXIC	= 1;
	private static final int TET_GETMINIC	= 2;
	private static final int TET_ISDEFIC	= 3;
	private static final int TET_GETTPCOUNT	= 4;
	private static final int TET_GETTESTNUM	= 5;
	private static final int TET_INVOKETP	= 6;
	private static final int TET_STARTUP	= 7;
	private static final int TET_CLEANUP	= 8;

	/*
	 * List of user-supplied test purposes.
	 */
	private TestPurpose[] testList;

	/**
	 * Constructs a new <code>SimpleTestCase</code>.
	 */
	public SimpleTestCase()
	{
		Method[] methods;
		Vector tpvec;
		int i;
		Class[] paramTypes;
		int modifiers;
		String name;
		int icnum;
		TestPurpose tp;
		int j;

		// Get the names of the test purpose methods.
		methods = getClass().getMethods();
		tpvec = new Vector();

		for (i = 0; i < methods.length; i++)
		{
	 		// Check the signature of the method. It must have a
			// return type of void and take a single parameter of
			// class TET.TestSession.
			if (methods[i].getReturnType() != Void.TYPE)
				continue;

			paramTypes = methods[i].getParameterTypes();
			if (paramTypes == null || paramTypes.length != 1
				|| !paramTypes[0].getName().equals(
					"TET.TestSession"))
				continue;

			// Check the method's modifiers. It must be a public
			// instance method.
			modifiers = methods[i].getModifiers();
			if (!Modifier.isPublic(modifiers)
				|| Modifier.isStatic(modifiers))
				continue;

			// Check the name of the method. Must conform to the
			// naming scheme for methods as given by getICofTP().
			name = methods[i].getName();
			icnum = getICofTP(name);
			if (icnum <= 0)
				continue;

			// Create new TestPurpose object and add to vector.
			tp = new TestPurpose();
			tp.method = methods[i];
			tp.icnum = icnum;
			tp.name = name;
			tpvec.addElement(tp);
		}

		// Create testList array and copy vector into testList.
		testList = new TestPurpose[tpvec.size()];
		tpvec.copyInto(testList);

		// Sort the list of test purposes.
		// Uses a selection sort; not particulary fast, but keeps the
		// code simple to understand.
		for (i = 0; i < testList.length - 1; i++)
		{
			for (j = i + 1; j < testList.length; j++)
			{
				if (compareTPs(testList[i].name,
					testList[j].name) > 0)
				{
					tp = testList[i];
					testList[i] = testList[j];
					testList[j] = tp;
				}
			}
		}
	}

	/**
	 * Get the number of the invocable component that a test purpose
	 * belongs to, based on the method name.
	 *
	 * @param	tpname	name of the test purpose method.
	 * @return	the IC number of the test purpose, or 0 if the method
	 * 		name indicates it is not a test purpose method.
	 */
	private int getICofTP(String tpname)
	{
		int len;
		int cp;
		int icnum;
		int d;

		len = tpname.length();
		if (len == 0)
			return 0;

		cp = 0;

		// First character may be 'i', for invocable component.
		// This is optional - the IC defaults to 1.
		if (tpname.charAt(cp) == 'i')
		{
			cp++;
			icnum = 0;

			while (cp < len
				&& (d = Character.digit(tpname.charAt(cp), 10))
				> -1)
			{
				icnum = 10 * icnum + d;
				cp++;
			}

			// IC numbers must be >= 1
			if (icnum == 0)
				return 0;
		}
		else
		{
			icnum = 1;
		}

		// Next character must be 't', for test purpose.
		if (cp > len - 2 || tpname.charAt(cp) != 't')
			return 0;

		for (cp++; cp < len; cp++)
		{
			if (Character.digit(tpname.charAt(cp), 10) < 0)
				return 0;
		}

		return icnum;
	}

	/**
	 * Compares two test purposes given the names of the methods, based on
	 * IC/TP number.
	 *
	 * @param	tp1name	name of first test purpose.
	 * @param	tp2name	name of second test purpose.
	 * @return	the value 0 if the two test purposes are the same,
	 *		greater than 0 if <code>tp1name</code> has a higher
	 *		IC/TP number combination than <code>tp2name</code>, and
	 *		less than 0 if <code>tp1name</code> has a lower IC/TP
	 *		number combination than <code>tp2name</code>.
	 */
	private int compareTPs(String tp1name, String tp2name)
	{
		int icdiff;
		int index1;
		int index2;
		int value1;
		int value2;

		// Compare IC numbers. If they are different, then the result
		// of the comparison has been decided.
		icdiff = getICofTP(tp1name) - getICofTP(tp2name);
		if (icdiff != 0)
			return icdiff;

		// Compare the rest of the strings after the 't'. Note that if
		// indexOf() returns -1, the whole string is used in the
		// comparison.
		index1 = tp1name.indexOf('t');
		index2 = tp2name.indexOf('t');

		value1 = Integer.parseInt(tp1name.substring(index1 + 1));
		value2 = Integer.parseInt(tp2name.substring(index2 + 1));

		if (value1 > value2)
			return 1;
		else
			return 0;
	}

	/**
	 * Begin execution of a test case using the default test session.
	 * The name of the test case is taken from the class of the
	 * <code>tc</code> argument.
	 *
 	 * @param	args	the command line arguments.
	 * @param	tc	the testcase.
	 */
	public static void main(String[] args, SimpleTestCase tc)
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
	public static void main(String pname, String[] args, SimpleTestCase tc)
	{
		tc.tcmMain(TestSession.createTestSession(pname, "japi"),
			makeArgList(pname, args));
	}

	/**
	 * Begin execution of a test case.
	 *
	 * @param	pname	the name of the test case.
	 * @param	args	the command line arguments.
	 * @param	tc	the test case.
	 * @param	ts	the test session to use.
	 * @deprecated	Not part of the published API.
	 */
	public static void main(String pname, String[] args, SimpleTestCase tc,
		TestSession ts)
	{
		tc.tcmMain(ts, makeArgList(pname, args));
	}

	/*
	 * Pass control to the TCM. Calls the C function tet_tcm_main().
	 * Control re-enters Java code in doTCOp(). This method does not
	 * return.
	 *
	 *	ts	TestSession object for this test run.
	 *	args	Command line arguments passed from parent.
	 */
	private native void tcmMain(TestSession ts, String[] args);

	/*
	 * Perform Test Case Operation. This provides the functionality
	 * necessary for the dynamic test case interface which is implemented
	 * in the native code library. This functionality is rolled into one
	 * method rather than one method for each operation to simplify the
	 * JNI calls in the native code.
	 *
	 *	ts	the TestSession object for the current test run.
	 *	op	Test case operation. One of:
	 *
	 *		TET_GETMAXIC	get highest IC number.
	 *		TET_GETMINIC	get lowest IC number.
	 *		TET_ISDEFIC	is this IC number defined?
	 *		TET_GETTPCOUNT	get number of TPs.
	 *		TET_GETTESTNUM	get absolute test number.
	 *		TET_INVOKETP	invoke TP method.
	 *		TET_STARTUP	invoke startup method.
	 *		TET_CLEANUP	invoke cleanup method.
	 *
	 *	icnum	Number of the IC of interest.
	 *	tpnum	Number of the TP of interest.
	 *
	 * Returns the result of the underlying operation; 0 in case of error
	 * or where the underlying operation has no return value.
	 */
	final int doTCOp(TestSession ts, int op, int icnum, int tpnum)
	{
		int i;
		int count;
		Method tpm;
		String reason;

		switch (op)
		{
		case TET_GETMAXIC:	/* Get highest IC number */
			return (testList.length > 0)
				? testList[testList.length - 1].icnum : 0;

		case TET_GETMINIC:	/* Get lowest IC number */
			return (testList.length > 0) ? testList[0].icnum : 0;

		case TET_ISDEFIC:	/* Is this IC number defined? */
			for (i = 0; i < testList.length; i++)
			{
				if (testList[i].icnum == icnum)
					return 1;
			}
			return 0;

		case TET_GETTPCOUNT:	/* Get number of TPs */
			for (i = 0, count = 0; i < testList.length; i++)
			{
				if (testList[i].icnum == icnum)
					count++;
			}
			return count;

		case TET_GETTESTNUM:	/* Get absolute test number */
			for (i = 0, count = 0; i < testList.length; i++)
			{
				if (testList[i].icnum == icnum
					&& ++count == tpnum)
					return i + 1;
			}
			return 0;

		case TET_INVOKETP:	/* Invoke TP method */
			tpm = null;

			for (i = 0, count = 0; i < testList.length; i++)
			{
				if (testList[i].icnum == icnum
					&& ++count == tpnum)
				{
					tpm = testList[i].method;
					break;
				}
			}

			if (tpm == null)
			{
				ts.reportError(null, ts.TET_NORESULT,
					"tet_invoketp() called for"
					+ " non-existent test purpose:"
					+ " icnum = " + icnum + ", tpnum = "
					+ tpnum);
			}
			else
			{
				ts.initThreads("IC" + icnum + "TP" + tpnum,
					true);

				try
				{
					tpm.invoke(this, new Object[] { ts });
				}
				catch (InvocationTargetException e)
				{
					ts.reportError(e.getTargetException(),
						ts.TET_UNRESOLVED,
						"Caught exception in test"
						+ " purpose method "
						+ tpm.getName() + "()");
				}
				catch (Exception e)
				{
					ts.reportError(e, ts.TET_UNRESOLVED,
						"Caught exception invoking"
						+ " test purpose method "
						+ tpm.getName()
						+ "() using reflection");
				}

				ts.cleanThreads();
			}

			return 0;

		case TET_STARTUP:	/* Invoke startup method */
			ts.initThreads("startup", true);

			try
			{
				startup(ts);
			}
			catch (Exception e)
			{
				reason = "Caught exception in startup";

				ts.reportError(e, ts.TET_NORESULT, reason);

				/* Cancel all test cases */
				for (i = 1; i <= testList.length; i++)
					ts.tet_delete(i, reason);
			}

			ts.cleanThreads();
			return 0;

		case TET_CLEANUP:	/* Invoke cleanup method */
			ts.initThreads("cleanup", true);

			try
			{
				cleanup(ts);
			}
			catch (Exception e)
			{
				ts.reportError(e, ts.TET_NORESULT,
					"Caught exception in cleanup");
			}

			ts.cleanThreads();
			return 0;

		default:	/* Should never happen - code inconsistency */
			ts.reportError(null, ts.TET_NORESULT,
				"Internal error - unknown op code " + op
				+ " in doTCOp()");
			break;
		}

		return 0;
	}

	/**
	 * Method to be executed at test case startup. It can be overridden by
	 * the instance of this class implementing the test.
	 *
	 * @param	ts	the <code>TestSession</code> object for the
	 *			current test run.
	 */
	public void startup(TestSession ts) {}

	/**
	 * Method to be executed at test case shutdown. It can be overridden by
	 * the instance of this class implementing the test.
	 *
	 * @param	ts	the <code>TestSession</code> object for the
	 *			current test run.
	 */
	public void cleanup(TestSession ts) {}
}
