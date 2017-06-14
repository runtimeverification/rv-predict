/*
 *	SCCS: @(#)SyncMessage.java	1.1 (99/09/03)
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
 * The <code>SyncMessage</code> class provides the details of one message used
 * during a TET sychronization event.
 *
 * @version	@(#)SyncMessage.java	1.1 99/09/03 TETware release 3.8
 * @author	From JETpack 1.02 source
 * @author	Matthew Hails, UniSoft Ltd.
 */
public class SyncMessage
{
	static final String sccsid = "@(#)SyncMessage.java	1.1 (99/09/03) TETware release 3.8";

	/**
	 * Bit value for flags, indicating that the system is sending message
	 * data.
	 *
	 * <p>
	 * Provided as public for backwards compatibility only; serves no
	 * useful purpose outside package.
	 */
	public static final int TET_SMSNDMSG	= 001;

	/**
	 * Bit value for flags, indicating that the system is receiving message
	 * data.
	 *
	 * <p>
	 * Provided as public for backwards compatibility only; serves no
	 * useful purpose outside package.
	 */
	public static final int TET_SMRCVMSG	= 002;

	/**
	 * Bit value for flags, indicating that more than one system attempted
	 * to send message data.
	 *
	 * <p>
	 * Provided as public for backwards compatibility only; serves no
	 * useful purpose outside package.
	 */
	public static final int TET_SMDUP	= 004;

	/**
	 * Bit value for flags, indicating that the message data was truncated.
	 *
	 * <p>
	 * Provided as public for backwards compatibility only; serves no
	 * useful purpose outside package.
	 */
	public static final int TET_SMTRUNC	= 010;

	/**
	 * Maximum size of a sync message. Size must be expressable in 12 bits.
	 */
	public static final int TET_SMMSGMAX	= 1024;

	/*
	 * Message data.
	 */
	private byte[] data;

	/*
	 * Flags.
	 */
	private int flags;

	/*
	 * System ID of system that sent data.
	 */
	private int sysID;

	/**
	 * Construct an empty <code>SyncMessage</code>.
	 */
	public SyncMessage()
	{
		data = null;
		flags = 0;
		sysID = -1;
	}

	/**
	 * Construct a <code>SyncMessage</code> which will be used for the
	 * transmission of a message.
	 *
	 * @param	msg	the message to send.
	 */
	public SyncMessage(byte[] msg)
	{
		set_message(msg);
	}

	/**
	 * Construct a <code>SyncMessage</code> which will be used for the
	 * reception of a message.
	 *
	 * @param	msglen	the size of the receive buffer to set up.
	 */
	public SyncMessage(int msglen)
	{
		set_receive_length(msglen);
	}

	/**
	 * Set up a <code>SyncMessage</code> for transmission of a message.
	 *
	 * @param	msg	the message to send.
	 */
	public void set_message(byte[] msg)
	{
		data = msg;
		flags = TET_SMSNDMSG;
		sysID = -1;
	}

	/**
	 * Retrieve the message length.
	 *
	 * @return	the message length.
	 */
	public int length()
	{
		return (data == null) ? 0 : data.length;
	}

	/**
	 * Set up a <code>SyncMessage</code> for reception of a message.
	 *
	 * @param	msglen	the size of the receive buffer to set up.
	 */
	public void set_receive_length(int msglen)
	{
		if (msglen > 0)
			data = new byte[msglen];
		else
			data = null;

		flags = TET_SMRCVMSG;
		sysID = -1;
	}

	/**
	 * Retrieve the message data.
	 *
	 * @return	the message data (as a byte array), <code>null</code>
	 * 		if there is no message data.
	 */
	public byte[] message()
	{
		return data;
	}

	/**
	 * Was this synchronization message truncated?
	 *
	 * @return	<code>true</code> if the message was truncated because
	 *		the message was longer than the receive buffer or
	 *		because the message was longer than the permitted
	 *		message length.
	 */
	public boolean truncated()
	{
		return (flags & TET_SMTRUNC) != 0;
	}

	/**
	 * Were there two senders during synchronization?
	 *
	 * @return	<code>true</code> if there were two senders during
	 *		synchronization.
	 */
	public boolean duplicated()
	{
		return (flags & TET_SMDUP) != 0;
	}

	/**
	 * Was this message sent or does it contains message received from
	 * another system?
	 *
	 * @return	<code>true</code> if this message was sent during
	 *		synchronization.
	 */
	public boolean sender()
	{
		return (flags & TET_SMSNDMSG) != 0;
	}

	/**
	 * Get the value of the system ID of the sending system.
	 *
	 * @return	the value of the system ID, or -1 if this message data
	 * 		was not used to receive data.
	 */
	public int getSysID()
	{
		return sysID;
	}

	/**
	 * Get the flags value.
	 */
	int getFlags()
	{
		return flags;
	}

	/**
	 * Set the fields of the message.
	 *
	 * @param	data	the message data.
	 * @param	flags	the message flags.
	 * @param	sysID	the system ID of the sending system.
	 */
	void setFields(byte[] data, int flags, int sysID)
	{
		this.data = data;
		this.flags = flags;
		this.sysID = sysID;
	}
}
