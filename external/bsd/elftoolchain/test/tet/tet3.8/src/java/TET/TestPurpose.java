/*
 *	SCCS: @(#)TestPurpose.java	1.1 (99/09/03)
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

/*
 * A <code>TestPurpose</code> holds the details of a single test purpose.
 *
 * @version	@(#)TestPurpose.java	1.1 99/09/03 TETware release 3.8
 * @author	Matthew Hails, UniSoft Ltd.
 */
class TestPurpose
{
	static final String sccsid = "@(#)TestPurpose.java	1.1 (99/09/03) TETware release 3.8";

	/*
	 * Test purpose method.
	 */
	Method method;

	/*
	 * Invocable component number of this test purpose.
	 */
	int icnum;

	/*
	 * Name of the test purpose method, as obtained from method.getName().
	 */
	String name;
}
