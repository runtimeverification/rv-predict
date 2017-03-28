package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.compact.datatypes.Address;
import com.runtimeverification.rvpredict.log.compact.datatypes.Generation;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMask;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalMaskNumber;
import com.runtimeverification.rvpredict.log.compact.datatypes.SignalNumber;
import com.runtimeverification.rvpredict.log.compact.datatypes.ThreadId;

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
            long deltop_first, ThreadId threadId, Generation generation)
            throws InvalidTraceDataException {
        if (threadIdToState.containsKey(threadId.getAsLong())) {
            throw new InvalidTraceDataException("Thread started twice: " + threadId.getAsLong() + ".");
        }
        currentThread = new ThreadState(deltop_first, threadId, generation);
        threadIdToState.put(currentThread.getThreadId(), currentThread);
    }

    void enterSignal(SignalNumber signalNumber, Generation generation) throws InvalidTraceDataException {
        currentThread.enterSignal(signalNumber, generation);
    }

    void changeOfGeneration(Generation generation) {
        currentThread.setGeneration(generation);
    }

    long getMemoizedSignalMask(long signalMaskNumber) {
        return memoizedSignalMasks.get(signalMaskNumber);
    }

    void removeSignalHandler(SignalNumber signalNumber) {
        currentThread.removeSignalHandler(signalNumber);
    }

    public void setSignalHandler(SignalNumber signalNumber, Address signalHandlerAddress) {
        currentThread.setSignalHandler(signalNumber, signalHandlerAddress);
    }

    public void setSignalMask(SignalNumber signalNumber, long signalMask) {
        currentThread.setSignalMask(signalNumber, signalMask);
    }

    public long getSignalNumber() {
        return currentThread.getSignalNumber();
    }

    public void exitSignal() {
        currentThread.exitSignal();
    }

    public void setSignalDepth(long signalDepth) {
        currentThread.setSignalDepth(signalDepth);
    }

    public void memoizeSignalMask(SignalMask signalMask, long originBitCount, SignalMaskNumber signalMaskNumber) {
        memoizedSignalMasks.put(signalMaskNumber.getAsLong(), signalMask.getAsLong() << originBitCount);
    }

    public void maskSignals(long signalMask) {
        currentThread.maskSignals(signalMask);
    }

    public void endThread() {
        currentThread.end();
    }

    public void joinThread(ThreadId otherThreadId) {
        threadIdToState.get(otherThreadId.getAsLong()).wasJoined();
    }

    public void forkThread(ThreadId threadId) {
    }

    public void switchThread(ThreadId threadId) {
        currentThread = threadIdToState.get(threadId.getAsLong());
        // TODO: Why, oh why?
        currentThread.setSignalDepth(0);
    }

    public long newId() {
    }

    public long getThreadId() {
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

        private ThreadState(long initialPC, ThreadId threadId, Generation generation) {
            this.threadId = threadId.getAsLong();
            lastPC = new ArrayList<>();
            lastPC.add(initialPC);
            this.generation = new ArrayList<>();
            this.generation.add(generation.getAsLong());
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

        void setGeneration(Generation generation) {
            this.generation.set(signalDepth, generation.getAsLong());
        }

        void enterSignal(SignalNumber signalNumber, Generation generation) throws InvalidTraceDataException {
            // TODO: The docs say that one should increment the signal depth.
            signalDepth++;
            currentThread.setGeneration(generation);
            // TODO: This is defined in a slightly different way in the docs.
            signalMaskStack.push(signalMaskStack.peek() | signalMasks.get(signalNumber.getAsLong()));
        }

        public void setSignalMask(SignalNumber signalNumber, long signalMask) {
            signalMasks.put(signalNumber.getAsLong(), signalMask);
        }

        public void exitSignal() {
            // TODO: Shouldn't I do something with the value?
            signalMaskStack.pop();
        }

        public void setSignalDepth(long signalDepth) {
            // TODO: Should I do something with the stack here?
            signalDepth = signalDepth;
        }

        public void maskSignals(long signalMask) {
            // TODO: This is defined in a different way in the documentation.
            // Why is there a signal depth which can be set to a given value
            // instead of just pushing and popping?
            signalMaskStack.push(signalMaskStack.pop() | signalMask);
        }

        public void end() {

        }

        public void wasJoined() {

        }

        public long getSignalNumber() {
            return signalMaskStack.peek();
        }

        void removeSignalHandler(SignalNumber signalNumber) {
            signalNumberToHandlerAddress.remove(signalNumber.getAsLong());
        }

        public void setSignalHandler(SignalNumber signalNumber, Address signalHandlerAddress) {
            signalNumberToHandlerAddress.put(signalNumber.getAsLong(), signalHandlerAddress.getAsLong());
        }
    }
}
