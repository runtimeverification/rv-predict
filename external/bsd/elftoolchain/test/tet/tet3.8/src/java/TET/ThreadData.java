/*
 *	SCCS: @(#)ThreadData.java	1.1 (99/09/03)
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

/**
 * Thread specific data used with TET package.
 *
 * @version     @(#)ThreadData.java	1.1 99/09/03 TETware release 3.8
 * @author      Matthew Hails, UniSoft Ltd.
 */
class ThreadData
{
	static final String sccsid = "@(#)ThreadData.java	1.1 (99/09/03) TETware release 3.8";

	/**
	 * The per-thread block number.
 	 */
	long block = 0;

	/**
	 * The per-thread sequence number.
 	 */
	long sequence = 0;
}
