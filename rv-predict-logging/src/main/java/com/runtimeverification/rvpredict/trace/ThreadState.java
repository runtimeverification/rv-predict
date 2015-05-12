package com.runtimeverification.rvpredict.trace;

import java.util.Collection;
import java.util.Deque;

class ThreadState {

    private final Deque<Integer> stacktrace;
    private final Collection<LockState> lockStates;

    ThreadState(Deque<Integer> stacktrace, Collection<LockState> lockStates) {
        this.stacktrace = stacktrace;
        this.lockStates = lockStates;
    }

    Deque<Integer> getStacktrace() {
        return stacktrace;
    }

    Collection<LockState> getLockStates() {
        return lockStates;
    }

}