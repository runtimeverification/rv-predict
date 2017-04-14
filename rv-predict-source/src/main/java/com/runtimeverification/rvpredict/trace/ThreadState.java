package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.ReadonlyEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;

class ThreadState {

    private final Deque<ReadonlyEvent> stacktrace;
    private final Collection<LockState> lockStates;

    ThreadState() {
        stacktrace = new ArrayDeque<>();
        lockStates = new ArrayList<>();
    }

    ThreadState(Deque<ReadonlyEvent> stacktrace, Collection<LockState> lockStates) {
        this.stacktrace = stacktrace;
        this.lockStates = lockStates;
    }

    Deque<ReadonlyEvent> getStacktrace() {
        return stacktrace;
    }

    Collection<LockState> getLockStates() {
        return lockStates;
    }

}