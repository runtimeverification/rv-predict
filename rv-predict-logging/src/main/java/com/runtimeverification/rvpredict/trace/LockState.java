package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.Event;

public class LockState {

    private Event lock;

    private int level = 0;

    public Event lock() {
        return lock;
    }

    public int level() {
        return level;
    }

    public void acquire(Event lock) {
        if (level++ == 0) {
            this.lock = lock;
        }
    }

    public void release() {
        if (--level < 0) {
            throw new IllegalStateException("Lock entrance level cannot be less than 0!");
        }
    }

    public LockState copy() {
        LockState copy = new LockState();
        copy.lock = lock;
        copy.level = level;
        return copy;
    }
}