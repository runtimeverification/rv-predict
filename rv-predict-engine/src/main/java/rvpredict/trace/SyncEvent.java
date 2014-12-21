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
 * Represents synchronization events.
 *
 */
public class SyncEvent extends AbstractEvent {

    private final long syncObject;

    public SyncEvent(long GID, long TID, int ID, EventType type, long syncObject) {
        super(GID, TID, ID, type);
        this.syncObject = syncObject;
    }

    /**
     * Returns the {@code long} representation of the synchronization object involved
     * in the event.
     *
     * @see {@link rvpredict.logging.RecordRT} for the specific object involved
     *      in each event
     */
    public final long getSyncObject() {
        return syncObject;
    }

    @Override
    public final String toString() {
        return String.format("(%s, E%s, T%s, L%s, %s)", type, GID, TID, ID, Long.toHexString(syncObject));
    }

    public boolean isLock() {
        return type == EventType.LOCK || type == EventType.WAIT
                || type == EventType.WAIT_INTERRUPTED || type == EventType.WAIT_MAYBE_TIMEOUT;
    }

    public boolean isUnlock() {
        return type == EventType.UNLOCK || type == EventType.PRE_WAIT;
    }

}
