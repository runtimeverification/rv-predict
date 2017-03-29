package com.runtimeverification.rvpredict.log.compact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Context {
    private final Map<Long, ThreadState> threadIdToState;
    private final Map<Long, Long> memoizedSignalMasks;
    private ThreadState currentThread;

    public Context() {
        threadIdToState = new HashMap<>();
        memoizedSignalMasks = new HashMap<>();
    }

    void jump(long address) {
        currentThread.setLastPC(address);
    }

    void updatePcWithDelta(int jumpDelta) {
        currentThread.setLastPC(
                currentThread.getLastPC() + jumpDelta);
    }

    void beginThread(
            long deltop_first, long threadId, long generation)
            throws InvalidTraceDataException {
        if (threadIdToState.containsKey(threadId)) {
            throw new InvalidTraceDataException("Thread started twice: " + threadId + ".");
        }
        currentThread = new ThreadState(deltop_first, threadId, generation);
        threadIdToState.put(currentThread.getThreadId(), currentThread);
    }

    void enterSignal(long signalNumber, long generation) throws InvalidTraceDataException {
        currentThread.enterSignal(signalNumber, generation);
    }

    void changeOfGeneration(long generation) {
        currentThread.setGeneration(generation);
    }

    long getMemoizedSignalMask(long signalMaskNumber) {
        return memoizedSignalMasks.get(signalMaskNumber);
    }

    void removeSignalHandler(long signalNumber) {
        currentThread.removeSignalHandler(signalNumber);
    }

    void setSignalHandler(long signalNumber, long signalHandlerAddress) {
        currentThread.setSignalHandler(signalNumber, signalHandlerAddress);
    }

    void setSignalMask(long signalNumber, long signalMask) {
        currentThread.setSignalMask(signalNumber, signalMask);
    }

    long getSignalNumber() {
        return currentThread.getSignalNumber();
    }

    void exitSignal() {
        currentThread.exitSignal();
    }

    void setSignalDepth(long signalDepth) {
        currentThread.setSignalDepth(signalDepth);
    }

    void memoizeSignalMask(long signalMask, long originBitCount, long signalMaskNumber) {
        memoizedSignalMasks.put(signalMaskNumber, signalMask << originBitCount);
    }

    void maskSignals(long signalMask) {
        currentThread.maskSignals(signalMask);
    }

    void endThread() {
        currentThread.end();
    }

    void joinThread(long otherThreadId) {
        threadIdToState.get(otherThreadId).wasJoined();
    }

    void forkThread(long threadId) {
    }

    void switchThread(long threadId) {
        currentThread = threadIdToState.get(threadId);
        // TODO: Why, oh why?
        currentThread.setSignalDepth(0);
    }

    long newId() {
    }

    long getThreadId() {
        return currentThread.getThreadId();
    }

    private class ThreadState {
        private final long threadId;
        private final List<Long> lastPC;
        private final List<Long> generation;
        private final Stack<Long> signalMaskStack;
        private HashMap<Long, Long> signalMasks;
        private final Map<Long, Long> signalNumberToHandlerAddress;
        private int signalDepth;

        private ThreadState(long initialPC, long threadId, long generation) {
            this.threadId = threadId;
            lastPC = new ArrayList<>();
            lastPC.add(initialPC);
            this.generation = new ArrayList<>();
            this.generation.add(generation);
            this.generation.add(0L);
            this.signalDepth = 0;
            // TODO: How should this be initialized?
            this.signalMasks = new HashMap<>();
            this.signalNumberToHandlerAddress = new HashMap<>();
            this.signalMaskStack = new Stack<>();
        }

        private void setLastPC(long programCounter) {
            lastPC.set(signalDepth, programCounter);
        }

        private long getLastPC() {
            return lastPC.get(signalDepth);
        }

        private long getThreadId() {
            return threadId;
        }

        void setGeneration(long generation) {
            this.generation.set(signalDepth, generation);
        }

        void enterSignal(long signalNumber, long generation) throws InvalidTraceDataException {
            signalDepth++;
            currentThread.setGeneration(generation);
            // TODO: This is defined in a slightly different way in the docs.
            signalMaskStack.push(signalMaskStack.peek() | signalMasks.get(signalNumber));
        }

        void setSignalMask(long signalNumber, long signalMask) {
            signalMasks.put(signalNumber, signalMask);
        }

        void exitSignal() {
            // TODO: Shouldn't I do something with the value?
            signalMaskStack.pop();
        }

        void setSignalDepth(long signalDepth) {
            // TODO: Should I do something with the stack here?
            signalDepth = signalDepth;
        }

        void maskSignals(long signalMask) {
            // TODO: This is defined in a different way in the documentation.
            // Why is there a signal depth which can be set to a given value
            // instead of just pushing and popping?
            signalMaskStack.push(signalMaskStack.pop() | signalMask);
        }

        void end() {
        }

        void wasJoined() {

        }

        long getSignalNumber() {
            return signalMaskStack.peek();
        }

        void removeSignalHandler(long signalNumber) {
            signalNumberToHandlerAddress.remove(signalNumber);
        }

        void setSignalHandler(long signalNumber, long signalHandlerAddress) {
            signalNumberToHandlerAddress.put(signalNumber, signalHandlerAddress);
        }
    }
}
