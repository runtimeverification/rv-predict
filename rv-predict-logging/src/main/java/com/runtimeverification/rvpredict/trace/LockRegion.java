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

import com.runtimeverification.rvpredict.log.EventItem;

public class LockRegion {
    private final EventItem lock;
    private final EventItem unlock;

    private final long lockId;
    private final long threadId;

    private boolean isReadLocked = false;

    public LockRegion(EventItem lock, EventItem unlock) {
        assert lock == null || lock.isLockEvent();
        assert unlock == null || unlock.isUnlockEvent();
        this.lock = lock;
        this.unlock = unlock;
        if (lock != null) {
            lockId = lock.getSyncObject();
            threadId = lock.getTID();
            if (lock.getType() == EventType.READ_LOCK) {
                assert unlock == null || unlock.getType() == EventType.READ_UNLOCK :
                    "Expected no PRE_WAIT event inside read lock region; but found: "
                        + unlock;
                isReadLocked = true;
            }
        } else {
            lockId = unlock.getSyncObject();
            threadId = unlock.getTID();
            if (unlock.getType() == EventType.READ_UNLOCK) {
                assert lock == null || lock.getType() == EventType.READ_LOCK :
                    "Expected no WAIT* event inside read lock region; but found: "
                        + lock;
                isReadLocked = true;
            }
        }
    }

    public EventItem getLock() {
        return lock;
    }

    public EventItem getUnlock() {
        return unlock;
    }

    public long getLockObj() {
        return lockId;
    }

    public long getThreadId() {
        return threadId;
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", lock, unlock);
    }

    public boolean isWriteLocked() {
        return !isReadLocked;
    }
}
