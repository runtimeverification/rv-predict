package com.runtimeverification.rvpredict.violation;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.Collection;

class HeldLock {
    private final ReadonlyEventInterface lock;
    private final Collection<ReadonlyEventInterface> stackTrace;

    HeldLock(ReadonlyEventInterface lock, Collection<ReadonlyEventInterface> stackTrace) {
        this.lock = lock;
        this.stackTrace = stackTrace;
    }

    ReadonlyEventInterface getLock() {
        return lock;
    }

    Collection<ReadonlyEventInterface> getStackTrace() {
        return stackTrace;
    }
}
