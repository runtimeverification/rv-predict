package com.runtimeverification.rvpredict.trace;

import java.util.Deque;
import java.util.List;

class ThreadState {

    private final Deque<Integer> stacktrace;
    private final List<LockState> lockStates;

    ThreadState(Deque<Integer> stacktrace, List<LockState> lockStates) {
        this.stacktrace = stacktrace;
        this.lockStates = lockStates;
    }

    Deque<Integer> getStacktrace() {
        return stacktrace;
    }

    List<LockState> getLockStates() {
        return lockStates;
    }

}