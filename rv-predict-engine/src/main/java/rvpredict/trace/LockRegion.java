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

import java.util.ArrayDeque;
import java.util.Deque;

public class LockRegion {
    private final SyncEvent lock;
    private final SyncEvent unlock;
    private final SyncEvent prewait;

    private final long lockObj;
    private final long threadId;

    private final Deque<SyncEvent> notifyEvents;

    public LockRegion(SyncEvent lock, SyncEvent unlock, SyncEvent prewait,
            Deque<SyncEvent> notifyEvents) {
        assert lock == null || lock.getType() == EventType.LOCK || lock.getType() == EventType.WAIT
                || lock.getType() == EventType.WAIT_TIMEOUT;
        assert unlock == null || unlock.getType() == EventType.UNLOCK
                || unlock.getType() == EventType.PRE_WAIT;
        this.lock = lock;
        this.unlock = unlock;
        this.prewait = prewait;
        if (lock != null) {
            lockObj = lock.getSyncObject();
            threadId = lock.getTID();
        } else {
            lockObj = unlock.getSyncObject();
            threadId = unlock.getTID();
        }
        this.notifyEvents = new ArrayDeque<>(notifyEvents);
    }

    public SyncEvent getLock() {
        return lock;
    }

    public SyncEvent getUnlock() {
        return unlock;
    }

    public SyncEvent getPreWait() {
        return prewait;
    }

    public long getLockObj() {
        return lockObj;
    }

    public long getThreadId() {
        return threadId;
    }

    public Deque<SyncEvent> getNotifyEvents() {
        return notifyEvents;
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", lock, unlock);
    }
}
