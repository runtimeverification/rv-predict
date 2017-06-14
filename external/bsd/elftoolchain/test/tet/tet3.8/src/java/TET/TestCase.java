/*
 *	SCCS: @(#)TestCase.java	1.1 (99/09/03)
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
 * <code>TestCase</code> is a base class for all TETware test cases, including
 * child processes and remote processes. Actual test case implementations
 * should extend one of its subclasses and not use <code>TestCase</code>
 * directly.
 *
 * @version	@(#)TestCase.java	1.1 99/09/03 TETware release 3.8
 * @author	Matthew Hails, UniSoft Ltd.
 */
public abstract class TestCase
{
	static final String sccsid = "@(#)TestCase.java	1.1 (99/09/03) TETware release 3.8";

	/**
	 * Make a new argument list from the test case name and command line
	 * arguments. The test case name is prepended to the command line
	 * arguments and a new list returned.
	 *
	 * @param	pname	test case name.
	 * @param	args	command line arguments.
	 * @return	a new String array with <code>pname</code> as the first
	 *		element and <code>args</code> as the remainder.
	 */
	static String[] makeArgList(String pname, String[] args)
	{
		String[] newargs;

		newargs = new String[args.length + 1];
		newargs[0] = pname;
		System.arraycopy(args, 0, newargs, 1, args.length);

		return newargs;
	}

	/**
	 * Get the name of the current class. The returned class name does not
	 * include the package qualifiers.
	 *
	 * @return	the name of the class.
	 */
	String getClassName()
	{
		String name;
		int index;

		// Get the name of the class, remove the package qualifiers,
		// and return the resulting string.
		name = getClass().getName();
		index = name.lastIndexOf('.');

		if (index >= 0 && index < name.length() - 1)
			name = name.substring(index + 1);

		return name;
	}
}
