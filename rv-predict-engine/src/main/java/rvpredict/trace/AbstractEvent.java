/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package rvpredict.trace;

import com.google.common.primitives.Longs;

import rvpredict.log.EventItem;

/**
 * Base class for all events in the trace.
 */
public abstract class AbstractEvent implements Event {

    protected final long GID;
    protected final long TID;
    protected final int ID;
    protected final EventType type;

    protected AbstractEvent(long GID, long TID, int ID, EventType type) {
        this.GID = GID;
        this.TID = TID;
        this.ID = ID;
        this.type = type;
    }

    public static Event of(EventItem eventItem) {
        AbstractEvent node = null;
        long GID = eventItem.GID;
        long TID = eventItem.TID;
        int ID = eventItem.ID;
        long ADDRL = eventItem.ADDRL;
        int ADDRR = eventItem.ADDRR;
        long VALUE = eventItem.VALUE;
        EventType TYPE = eventItem.TYPE;

        switch (TYPE) {
            case INIT:
                node = new InitEvent(GID, TID, ID, ADDRL, ADDRR, VALUE);
                break;
            case READ:
                node = new ReadEvent(GID, TID, ID, ADDRL, ADDRR, VALUE);
                break;
            case WRITE:
                node = new WriteEvent(GID, TID, ID, ADDRL, ADDRR, VALUE);
                break;
            case WRITE_LOCK:
            case WRITE_UNLOCK:
            case READ_LOCK:
            case READ_UNLOCK:
            case WAIT_REL:
            case WAIT_ACQ:
            case START:
            case PRE_JOIN:
            case JOIN:
                case JOIN_MAYBE_FAILED:
                node = new SyncEvent(GID, TID, ID, TYPE, ADDRL);
                break;
            case BRANCH:
                node = new BranchEvent(GID, TID, ID);
                break;
            default:
                assert false : "unexpected event type: " + TYPE;
                break;
        }
        return node;
    }

    @Override
    public long getGID() {
        return GID;
    }

    @Override
    public long getTID() {
        return TID;
    }

    @Override
    public int getID() {
        return ID;
    }

    @Override
    public EventType getType() {
        return type;
    }

    @Override
    public final boolean isLockEvent() {
        return EventType.isLock(type) || type == EventType.WAIT_ACQ;
    }

    @Override
    public final boolean isUnlockEvent() {
        return EventType.isUnlock(type) || type == EventType.WAIT_REL;
    }

    @Override
    public final int hashCode() {
        return Longs.hashCode(GID);
    }

    @Override
    public final boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Event)) {
            return false;
        }
        Event otherEvent = (Event) object;
        return getGID() == otherEvent.getGID();
    }

    @Override
    public String toString() {
        return String.format("(%s, E%s, T%s, L%s)", type, GID, TID, ID);
    }
}
