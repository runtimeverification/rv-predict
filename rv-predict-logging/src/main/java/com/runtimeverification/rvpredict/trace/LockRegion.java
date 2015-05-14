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
package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.EventType;

public class LockRegion {
    private final Event lock;
    private final Event unlock;

    private final long tid;
    private final long lockId;

    private boolean isReadLocked = false;

    public LockRegion(Event lock, Event unlock) {
        this.lock = lock;
        this.unlock = unlock;

        if (lock != null) {
            tid = lock.getTID();
            lockId = lock.getLockId();
            if (lock.getType() == EventType.READ_LOCK) {
                if (unlock != null && unlock.getType() != EventType.READ_UNLOCK) {
                    throw new IllegalArgumentException("Unmatched lock pairs: " + lock + " & " + unlock);
                }
                isReadLocked = true;
            }
        } else {
            tid = unlock.getTID();
            lockId = unlock.getLockId();
            if (unlock.getType() == EventType.READ_UNLOCK) {
                isReadLocked = true;
            }
        }
    }

    public Event getLock() {
        return lock;
    }

    public Event getUnlock() {
        return unlock;
    }

    public long getLockId() {
        return lockId;
    }

    public long getTID() {
        return tid;
    }

    public boolean isWriteLocked() {
        return !isReadLocked;
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", lock, unlock);
    }

}
