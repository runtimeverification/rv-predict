/*
 *	SCCS: @(#)SystemEntry.java	1.1 (99/09/03)
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
 * An information record about a system participating in a test.
 *
 * @version	@(#)SystemEntry.java	1.1 99/09/03 TETware release 3.8
 * @author	From JETpack 1.02 source
 * @author	Matthew Hails, UniSoft Ltd.
 */
public class SystemEntry
{
	static final String sccsid = "@(#)SystemEntry.java	1.1 (99/09/03) TETware release 3.8";

	/**
	 * The id of the system.
	 */
	public int ts_sysid;

	/**
	 * The name of the system.
	 */
	public String ts_name;

	/**
	 * Construct a new <code>SystemEntry</code>
	 *
	 * @param	id	ID of the system.
	 * @param	name	name of the system.
	 */
	public SystemEntry(int id, String name)
	{
		this.ts_sysid = id;
		this.ts_name = name;
	}
}
