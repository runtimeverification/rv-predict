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
    public boolean equals(Object object) {
        if (!(object instanceof Event)) {
            return false;
        }
        Event otherEvent = (Event) object;
        return getGID() == otherEvent.getGID();
    }


    @Override
    public String toString() {
        return GID + ": thread " + TID + " " + ID + " " + type;
    }
}
