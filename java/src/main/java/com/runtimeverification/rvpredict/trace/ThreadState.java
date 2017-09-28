package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;

class ThreadState {

    private final Deque<ReadonlyEventInterface> stacktrace;
    private final Collection<LockState> lockStates;

    ThreadState() {
        stacktrace = new ArrayDeque<>();
        lockStates = new ArrayList<>();
    }

    ThreadState(Deque<ReadonlyEventInterface> stacktrace, Collection<LockState> lockStates) {
        this.stacktrace = stacktrace;
        this.lockStates = lockStates;
    }

    Deque<ReadonlyEventInterface> getStacktrace() {
        return stacktrace;
    }

    Collection<LockState> getLockStates() {
        return lockStates;
    }

}