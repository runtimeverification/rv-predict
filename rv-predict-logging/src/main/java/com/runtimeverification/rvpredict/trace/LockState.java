package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.Event;

public class LockState {

    private final Event lock;

    private int level = 0;

    public LockState(Event lock) {
        this.lock = lock;
    }

    public Event lock() {
        return lock;
    }

    public int level() {
        return level;
    }

    public void incLevel() {
        level++;
    }

    public void decLevel() {
        level--;
        if (level < 0) {
            throw new IllegalStateException("Lock entrance level cannot be less than 0!");
        }
    }

    public LockState copy() {
        LockState copy = new LockState(lock);
        copy.level = level;
        return copy;
    }
}