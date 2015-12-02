package com.runtimeverification.rvpredict.trace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;

import com.runtimeverification.rvpredict.log.Event;

class ThreadState {

    private final Deque<Event> stacktrace;
    private final Collection<LockState> lockStates;

    ThreadState() {
        stacktrace = new ArrayDeque<>();
        lockStates = new ArrayList<>();
    }

    ThreadState(Deque<Event> stacktrace, Collection<LockState> lockStates) {
        this.stacktrace = stacktrace;
        this.lockStates = lockStates;
    }

    Deque<Event> getStacktrace() {
        return stacktrace;
    }

    Collection<LockState> getLockStates() {
        return lockStates;
    }

}