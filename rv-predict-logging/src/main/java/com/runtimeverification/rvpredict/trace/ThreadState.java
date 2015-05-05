package com.runtimeverification.rvpredict.trace;

import java.util.List;

class ThreadState {

    private final List<Integer> stacktrace;
    private final List<LockState> lockStates;

    ThreadState(List<Integer> stacktrace, List<LockState> lockStates) {
        this.stacktrace = stacktrace;
        this.lockStates = lockStates;
    }

    List<Integer> getStacktrace() {
        return stacktrace;
    }

    List<LockState> getLockStates() {
        return lockStates;
    }

}