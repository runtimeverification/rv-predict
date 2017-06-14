/*
 *	SCCS: @(#)JournalWriter.java	1.1 (99/09/03)
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
import java.util.*;
import java.io.*;

/**
 * Print lines to the execution results file.
 *
 * @version	@(#)JournalWriter.java	1.1 99/09/03 TETware release 3.8
 * @author	Matthew Hails, UniSoft Ltd.
 */
final class JournalWriter extends Writer
{
	static final String sccsid = "@(#)JournalWriter.java	1.1 (99/09/03) TETware release 3.8";

	/*
	 * The TestSession object for the current test run.
	 */
	private TestSession ts;

	/*
	 * Vector of Strings - essentially a buffer of lines due to be logged
	 * to the results file.
	 */
	private Vector lines;

	/*
	 * Is the stream `open'?
	 */
	private boolean open;

	/*
	 * Construct a new JournalWriter.
	 *
	 * 	ts		the TestSession object for the current test
	 *			run.
	 * 	firstLine	an initial line to log to the execution results
	 * 			file. May be null.
 	 */
	JournalWriter(TestSession ts, String firstLine)
	{
		int i;

		this.ts = ts;
		this.lines = new Vector();
		this.open = true;

		if (firstLine != null)
			lines.addElement(firstLine);
	}

	/*
	 * Write a string. Implements the corresponding write() method of
	 * java.io.Writer.
	 *
	 * 	cbuf	array of characters.
	 * 	off	offset from which to start writing characters.
	 * 	len	number of characters to write.
	 */
	public synchronized void write(char[] cbuf, int off, int len)
							throws IOException
	{
		int i;
		int start;

		checkState("Write");

		for (start = off, i = off; i < off + len; i++)
		{
			if (cbuf[i] == '\n')
			{
				if (i > start)
				{
					lines.addElement(new String(cbuf,
							start, i - start));
				}

				start = i + 1;
			}
		}

		if (start < off + len)
		{
			lines.addElement(
				new String(cbuf, start, off + len - start));
		}
	}

	/*
 	 * Flush any outstanding lines to the journal. Implements the flush()
	 * method of java.io.Writer.
	 */
	public synchronized void flush() throws IOException
	{
		String[] lineArray;

		checkState("Flush");

		if (lines.size() > 0)
		{
			/* Copy lines into a new String array and pass that
			 * to tet_merror() so the lines are logged together.
			 */
			lineArray = new String[lines.size()];
			lines.copyInto(lineArray);
			ts.tet_merror(0, lineArray);

			/* Empty the list of lines now they have been printed
			 * to the journal.
			 */
			lines.removeAllElements();
		}
	}

	/*
 	 * Close the stream, first flushing any outstanding lines to the
	 * journal. Implements the flush() method of Writer.
	 */
	public synchronized void close() throws IOException
	{
		if (open)
		{
			flush();
			open = false;
		}
	}

	/*
	 * Checks the state of the stream and throws a java.io.IOException if
	 * the stream is closed.
	 *
	 *	operation	name of current I/O operation, e.g. "write".
	 */
	private void checkState(String operation) throws IOException
	{
		if (!open)
		{
			throw new IOException(operation
				+ " attempt on closed JournalWriter()");
		}
	}
}
