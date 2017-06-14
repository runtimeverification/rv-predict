/*
 *	SCCS: @(#)IntegerTC.java	1.1 (99/09/03)
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
 * Test case class to test <code>java.lang.Integer</code>.
 *
 * @version     @(#)IntegerTC.java	1.1 99/09/03 TETware release 3.8
 * @author      Matthew Hails, UniSoft Ltd.
 */
public class IntegerTC extends SimpleTestCase
{
	private static final int T1_VALUE	= 103;
	private static final int T2_VALUE	= 99887766;
	private static final String T2_STRING	= "99887766";
	private static final String T3_STRING	= "N0t4n1nt3g3r";

	/**
	 * Entry point for this class. Calls <code>SimpleTestCase.main()</code>
	 * to pass control to TET.
	 *
	 * @param	args	command line arguments.
	 */
	public static void main(String[] args)
	{
		main(args, new IntegerTC());
	}

	/**
	 * Test purpose method for <code>Integer.intValue()</code>.
	 * Verifies that <code>Integer.intValue()</code> returns the value of
	 * this <code>Integer</code> as an <code>int</code>.
	 *
 	 * @param	ts	the <code>TestSession</code> object for this
	 *			test run.
	 */
	public void i1t1(TestSession ts)
	{
		Integer testInt;
		int val;

		// Create a new Integer object using a int value.
		testInt = new Integer(T1_VALUE);

		// Call intValue() on the new Integer object and verify it
		// returns the same value that was used in its creation.
		val = testInt.intValue();
		if (val == T1_VALUE)
		{
			ts.tet_result(ts.TET_PASS);
		}
		else
		{
			ts.tet_infoline("intValue() returned " + val
				+ ", expected " + T1_VALUE);
			ts.tet_result(ts.TET_FAIL);
		}
	}

	/**
	 * Test purpose method for <code>Integer.toString()</code>.
	 * Verifies that <code>Integer.toString()</code> returns a string
	 * representation of the value of this object in base 10.
	 *
 	 * @param	ts	the <code>TestSession</code> object for this
	 *			test run.
	 */
	public void i2t1(TestSession ts)
	{
		Integer testInt;
		String strval;

		// Create a new Integer object using a int value.
		testInt = new Integer(T2_VALUE);

		// Call toString() on the new Integer object and verify it
		// returns a string which has the expected value.
		strval = testInt.toString();
		if (strval.equals(T2_STRING))
		{
			ts.tet_result(ts.TET_PASS);
		}
		else
		{
			ts.tet_infoline("toString() returned \"" + strval
				+ "\", expected \"" + T2_STRING + "\"");
			ts.tet_result(ts.TET_FAIL);
		}
	}

	/**
	 * Test purpose method for <code>Integer.parseInt(String)</code>.
	 * Verifies that <code>Integer.parseInt(String)</code> throws a
	 * <code>java.lang.NumberFormatException</code> when the string
	 * argument does not contain a parsable integer.
	 *
 	 * @param	ts	the <code>TestSession</code> object for this
	 *			test run.
	 */
	public void i3t1(TestSession ts)
	{
		int val;

		try
		{
			// Call Integer.parseInt(), passing a string argument
			// which does not contain a parsable integer.
			val = Integer.parseInt(T3_STRING);
		}
		catch (NumberFormatException e)
		{
			// We caught the exception we expected.
			ts.tet_result(ts.TET_PASS);
			return;
		}
		catch (Exception e)
		{
			// We caught some other, unexpected exception, so the
			// test did not complete as expected, and hence has an
			// UNRESOLVED result.
			ts.tet_infoline("Integer.parseInt(\"" + T3_STRING
				+ "\") threw an unexpected exception: " + e);
			ts.tet_infoline("  expected a NumberFormatException"
				+ " to be thrown");
			ts.tet_result(ts.TET_UNRESOLVED);
			return;
		}

		// If we reach here, no exception was thrown, so the test has
		// failed.
		ts.tet_infoline("Integer.parseInt(\"" + T3_STRING
			+ "\") succeeded, expecting a NumberFormatException to"
			+ " be thrown");
		ts.tet_result(ts.TET_FAIL);
	}
}
