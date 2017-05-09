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

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public class LockRegion implements Comparable<LockRegion> {
    private final ReadonlyEventInterface lock;
    private final ReadonlyEventInterface unlock;

    private final long ttid;
    private final long lockId;

    private boolean isReadLocked = false;

    public LockRegion(ReadonlyEventInterface lock, ReadonlyEventInterface unlock, long ttid) {
        this.lock = lock;
        this.unlock = unlock;
        this.ttid = ttid;

        if (lock != null) {
            lockId = lock.getLockId();
            if (lock.getType() == EventType.READ_LOCK) {
                if (unlock != null && unlock.getType() != EventType.READ_UNLOCK) {
                    throw new IllegalArgumentException("Unmatched lock pairs: " + lock + " & " + unlock);
                }
                isReadLocked = true;
            }
        } else {
            lockId = unlock.getLockId();
            if (unlock.getType() == EventType.READ_UNLOCK) {
                isReadLocked = true;
            }
        }
    }

    public ReadonlyEventInterface getLock() {
        return lock;
    }

    public ReadonlyEventInterface getUnlock() {
        return unlock;
    }

    public long getLockId() {
        return lockId;
    }

    public long getTTID() {
        return ttid;
    }

    public boolean isWriteLocked() {
        return !isReadLocked;
    }

    public boolean include(ReadonlyEventInterface e, long ettid) {
        return ttid == ettid && (lock == null || lock.compareTo(e) < 0)
                && (unlock == null || unlock.compareTo(e) > 0);
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", lock, unlock);
    }

    @Override
    public int compareTo(LockRegion otherRegion) {
        long x1 = lock == null ? Integer.MIN_VALUE : lock.getEventId();
        long y1 = unlock == null ? Integer.MAX_VALUE : unlock.getEventId();
        long x2 = otherRegion.lock == null ? Integer.MIN_VALUE : otherRegion.lock.getEventId();
        long y2 = otherRegion.unlock == null ? Integer.MAX_VALUE : otherRegion.unlock.getEventId();
        return x1 != x2 ? Long.compare(x1, x2) : Long.compare(y1, y2);
    }

}
