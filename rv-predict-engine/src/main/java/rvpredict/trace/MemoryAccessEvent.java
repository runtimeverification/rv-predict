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
 * Interface for read and write events.
 *
 */
public abstract class MemoryAccessEvent extends AbstractEvent {

    protected final long value;
    protected final long objectHashCode;
    protected final long index;

    protected long prevBranchId = -1;

    protected MemoryAccessEvent(long GID, long TID, int ID, EventType type, long objectHashCode,
            long index, long value) {
        super(GID, TID, ID, type);
        this.objectHashCode = objectHashCode;
        this.index = index;
        this.value = value;
    }

    /**
     * Returns {@code String} representation of the accessed memory address in the event.
     */
    // TODO(YilongL): normalize address representation of memory access events
    public abstract String getAddr();

    /**
     * Returns the value read or written in the access.
     */
    public final long getValue() {
        return value;
    }

    public long getPrevBranchId() {
        return prevBranchId;
    }

    public void setPrevBranchId(long id) {
        prevBranchId = id;
    }

    @Override
    public final String toString() {
        return GID + ": thread " + TID + " " + ID + " " + objectHashCode + " " + index + " " + value + " " + type;
    }

}
