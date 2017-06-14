/*
 *	SCCS: @(#)TetError.java	1.1 (99/09/03)
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
 * A <code>TetError</code> is thrown when code in the <code>TET</code> package
 * encounters a fatal error condition. If a <code>TetError</code> object is
 * caught, the current test run should be aborted as soon as possible.
 *
 * @version	@(#)TetError.java	1.1 99/09/03 TETware release 3.8
 * @author	Matthew Hails, UniSoft Ltd.
 */
class TetError extends Error
{
	static final String sccsid = "@(#)TetError.java	1.1 (99/09/03) TETware release 3.8";

	/**
	 * Constructs a <code>TetError</code> with the specified detail
	 * message. 
	 *
	 * @param	s	the detail message.
	 */
	TetError(String s)
	{
		super(s);
	}
}
