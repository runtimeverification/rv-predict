/*
 *	SCCS: @(#)StackTC.java	1.1 (99/09/03)
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
import java.util.*;
import TET.*;

/**
 * Test case class to test <code>java.util.Stack</code>.
 *
 * @version     @(#)StackTC.java	1.1 99/09/03 TETware release 3.8
 * @author      Matthew Hails, UniSoft Ltd.
 */
public class StackTC extends SimpleTestCase
{
	/*
	 * Number of objects to use when creating a stack during the tests.
	 * Fairly arbitrary number - just needs to be large enough to exercise
	 * its capabilities.
	 */
	private static final int NTESTOBJS = 7;

	/*
	 * Internal counter for use by makeObject().
	 */
	private int counter;

	/**
	 * Entry point for this class. Calls <code>SimpleTestCase.main()</code>
	 * to pass control to TET.
	 *
	 * @param	args	command line arguments.
	 */
	public static void main(String[] args)
	{
		main(args, new StackTC());
	}

	/**
	 * Create a new <code>StackTC</code>.
	 */
	public StackTC()
	{
		this.counter = 0;
	}

	/**
	 * Test purpose method for <code>Stack.pop()</code>.
	 * Verifies that <code>Stack.pop()</code> removes the object at the top
	 * of this stack and returns that object as the value of this function.
	 *
 	 * @param	ts	the <code>TestSession</code> object for this
	 *			test run.
	 */
	public void i1t1(TestSession ts)
	{
		Stack stack;
		int i;
		Object obj;
		Object top;
		Object got;

		// Create a new stack.
		stack = new Stack();

		// Create a few objects and push them onto the stack, saving a
		// reference to the last one pushed.
		top = null;

		for (i = 0; i < NTESTOBJS; i++)
		{
			obj = makeObject(ts);
			stack.push(obj);
			top = obj;
		}

		// Call pop() on the stack and verify that the object returned
		// is the same object last pushed.
		try
		{
			got = stack.pop();
			if (got == top)
			{
				ts.tet_result(ts.TET_PASS);
			}
			else
			{
				ts.tet_infoline("pop() returned ["
					+ getObjDesc(got) + "], expected ["
					+ getObjDesc(top) + "]");
				ts.tet_result(ts.TET_FAIL);
			}
		}
		catch (EmptyStackException e)
		{
			ts.tet_infoline("pop() threw EmptyStackException");
			ts.tet_result(ts.TET_FAIL);
		}
		catch (Exception e)
		{
			ts.tet_infoline("Caught unexpected exception: " + e);
			ts.tet_result(ts.TET_UNRESOLVED);
		}
	}

	/**
	 * Test purpose method for <code>Stack.push(Object)</code>.
	 * Verifies that <code>Stack.push(Object)</code> pushes an item onto
	 * the top of this stack.
	 *
 	 * @param	ts	the <code>TestSession</code> object for this
	 *			test run.
	 */
	public void i2t1(TestSession ts)
	{
		Stack stack;
		int i;
		Object obj;
		Object top;

		// Create a new stack.
		stack = new Stack();

		// Create a few objects and push them onto the stack.
		for (i = 0; i < NTESTOBJS; i++)
			stack.push(makeObject(ts));

		// Create another object and put it onto the top of the stack
		// using push().
		obj = makeObject(ts);
		stack.push(obj);

		// Call peek() and verify it returns a reference to the object
		// just pushed.
		try
		{
			top = stack.peek();
			if (top == obj)
			{
				ts.tet_result(ts.TET_PASS);
			}
			else
			{
				ts.tet_infoline("Top of stack is ["
					+ getObjDesc(top) + "], expected ["
					+ getObjDesc(obj) + "]");
				ts.tet_result(ts.TET_FAIL);
			}
		}
		catch (EmptyStackException e)
		{
			ts.tet_infoline("peek() threw EmptyStackException");
			ts.tet_result(ts.TET_FAIL);
		}
		catch (Exception e)
		{
			ts.tet_infoline("Caught unexpected exception: " + e);
			ts.tet_result(ts.TET_UNRESOLVED);
		}
	}

	/**
	 * Test purpose method for <code>Stack.search(Object)</code>.
	 * Verifies that <code>Stack.search(Object)</code> returns where an
	 * object is on this stack.
	 *
 	 * @param	ts	the <code>TestSession</code> object for this
	 *			test run.
	 */
	public void i3t1(TestSession ts)
	{
		int testfail = 0;
		Stack stack;
		int i;
		Object[] objs;
		int pos;
		Object obj;

		// Create a new Stack, and a new array of Objects to store
		// references to those Objects pushed onto the stack.
		stack = new Stack();
		objs = new Object[NTESTOBJS];

		// Create a number of new Objects, pushing them onto the stack
		// and storing references to them in the array.
		for (i = 0; i < objs.length; i++)
		{
			objs[i] = makeObject(ts);
			stack.push(objs[i]);
		}

		// For every object in the array, call search() on the stack
		// and verify it returns the correct distance of that object
		// from the top of the stack.
		for (i = 0; i < objs.length; i++)
		{
			pos = stack.search(objs[i]);
			if (pos != objs.length - i)
			{
				ts.tet_infoline("search() returned " + pos
					+ " for object [" + getObjDesc(objs[i])
					+ "], expected " + (objs.length - i));
				ts.tet_result(ts.TET_FAIL);
				testfail++;
			}
		}

		// Create a new Object, but don't push onto the stack.
		obj = makeObject(ts);

		// Call search() and verify it returns -1.
		pos = stack.search(objs);
		if (pos != -1)
		{
			ts.tet_infoline("search() returned " + pos
				+ " for object not on stack, expected -1");
			ts.tet_result(ts.TET_FAIL);
			testfail++;
		}

		if (testfail == 0)
			ts.tet_result(ts.TET_PASS);
	}

	/*
	 * Create a new object.
	 *
 	 * 	ts	the TestSession object for this	test run.
	 *
	 * Returns a new object. The toString() method on this object will
	 * return a string which is unique within this test run.
	 */
	private Object makeObject(TestSession ts)
	{
		return new String(ts.tet_pname() + "-" + ts.tet_thistest()
			+ "-" + (++this.counter));
	}

	/*
	 * Get a description of an object. Includes the class of the object
	 * as well as its string description.
	 *
	 *	obj	the object for which the description is required.
	 *
	 * Returns a String giving a description of the target object.
	 */
	private String getObjDesc(Object obj)
	{
		return "class (" + obj.getClass().getName() + "), object ("
			+ obj.toString() + ")";
	}
}
