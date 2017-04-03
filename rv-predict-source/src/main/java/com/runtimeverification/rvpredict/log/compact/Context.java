package com.runtimeverification.rvpredict.log.compact;

public class Context {
    public Context() {
    }

    void jump(long address) throws InvalidTraceDataException {
    }

    void updatePcWithDelta(int jumpDelta) throws InvalidTraceDataException {
    }

    void beginThread(long threadId, long generation) throws InvalidTraceDataException {
    }

    void enterSignal(long signalNumber, long generation) throws InvalidTraceDataException {
    }

    void changeOfGeneration(long generation) {
    }

    long getMemoizedSignalMask(long signalMaskNumber) {
        return 0;
    }

    void disestablishSignal(long signalNumber) {
    }

    void establishSignal(long handlerAddress, long signalNumber, long signalMaskNumber) {
    }

    long getSignalNumber() {
        return 0;
    }

    void exitSignal() {
    }

    void setSignalDepth(int signalDepth) {
    }

    void memoizeSignalMask(long signalMask, long originBitCount, long signalMaskNumber) {
    }

    void maskSignals(long signalMask) {
    }

    void endThread() {
    }

    void joinThread(long otherThreadId) {
    }

    void forkThread(long threadId) {
    }

    void switchThread(long threadId) throws InvalidTraceDataException {
    }

    long newId() {
        return 0;
    }

    long getThreadId() {
        return 0;
    }

}
