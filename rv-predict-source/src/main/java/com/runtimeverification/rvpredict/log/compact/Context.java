package com.runtimeverification.rvpredict.log.compact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Context {
    private final Map<Long, ThreadState> threadIdToState;
    private final Map<Long, Long> memoizedSignalMasks;
    private final long minDeltaAndEventType;

    private ThreadState currentThread;

    public Context(long minDeltaAndEventType) {
        threadIdToState = new HashMap<>();
        memoizedSignalMasks = new HashMap<>();
        this.minDeltaAndEventType = minDeltaAndEventType;
    }

    void jump(long address) throws InvalidTraceDataException {
        currentThread.setLastPC(address);
    }

    void updatePcWithDelta(int jumpDelta) throws InvalidTraceDataException {
        currentThread.setLastPC(currentThread.getLastPC() + jumpDelta);
        currentThread.newOperation();
    }

    void beginThread(long threadId, long generation) throws InvalidTraceDataException {
        currentThread = threadIdToState.computeIfAbsent(
                threadId, tid -> new ThreadState(minDeltaAndEventType, threadId));
        currentThread.begin(generation);
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

    void disestablishSignal(long signalNumber) {
        currentThread.removeSignalHandler(signalNumber);
    }

    void establishSignal(long handlerAddress, long signalNumber, long signalMaskNumber) {
        currentThread.setSignalHandler(signalNumber, handlerAddress);
        currentThread.setSignalMask(signalNumber, getMemoizedSignalMask(signalMaskNumber));
    }

    long getSignalNumber() {
        return currentThread.getSignalNumber();
    }

    void exitSignal() {
        currentThread.exitSignal();
    }

    void setSignalDepth(int signalDepth) {
        currentThread.setSignalDepth(signalDepth);
    }

    void memoizeSignalMask(long signalMask, long originBitCount, long signalMaskNumber) {
        memoizedSignalMasks.put(signalMaskNumber, signalMask << originBitCount);
    }

    void maskSignals(long signalMask) {
        currentThread.maskSignals(signalMask);
    }

    void endThread() {
        System.out.println("Ending thread " + currentThread.getThreadId());
        currentThread.end();
    }

    void joinThread(long otherThreadId) {
        System.out.println("Joining thread: " + otherThreadId);
        threadIdToState.get(otherThreadId).wasJoined();
    }

    void forkThread(long threadId) {
        System.out.println("Forking thread " + threadId);
    }

    void switchThread(long threadId) throws InvalidTraceDataException {
        currentThread = threadIdToState.computeIfAbsent(
                threadId, tid -> new ThreadState(minDeltaAndEventType, threadId));
        // TODO: Why, oh why?
        currentThread.setSignalDepth(0);
    }

    long newId() {
        return currentThread.newId();
    }

    long getThreadId() {
        return currentThread.getThreadId();
    }

    private static class ThreadState {
        private enum State {
            NOT_STARTED,
            STARTED,
            ENDED,
        }
        private final long threadId;
        private final List<Long> lastPC;
        private final Stack<Long> signalMaskStack;
        private final List<Long> numberOfOperations;
        private final List<Long> generation;
        private final HashMap<Long, Long> signalMasks;
        private final Map<Long, Long> signalNumberToHandlerAddress;
        private int signalDepth;
        private State state;

        private ThreadState(long initialPC, long threadId) {
            this.threadId = threadId;
            lastPC = new ArrayList<>();
            lastPC.add(initialPC);
            this.generation = new ArrayList<>();
            // TODO: How should this be initialized?
            this.signalMasks = new HashMap<>();
            this.signalNumberToHandlerAddress = new HashMap<>();
            this.signalMaskStack = new Stack<>();
            this.numberOfOperations = new ArrayList<>();
            state = State.NOT_STARTED;
        }

        private void begin(long generation) throws InvalidTraceDataException {
            if (state != State.NOT_STARTED && state != State.ENDED) {
                throw new InvalidTraceDataException(
                        "Attempting to (re)start thread that did not end, state = " + state + ".");
            }
            state = State.STARTED;
            this.generation.add(generation);
            // this.generation.add(0L);
            setSignalDepth(0);
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
            if (this.generation.size() == signalDepth) {
                this.generation.add(generation);
            } else {
                this.generation.set(signalDepth, generation);
            }
            numberOfOperations.set(signalDepth, 0L);
        }

        void enterSignal(long signalNumber, long generation) throws InvalidTraceDataException {
            signalDepth++;
            setGeneration(generation);
            // TODO: This is defined in a slightly different way in the docs.
            signalMaskStack.push(signalMaskStack.peek() | signalMasks.get(signalNumber));
        }

        void setSignalMask(long signalNumber, long signalMask) {
            signalMasks.put(signalNumber, signalMask);
        }

        void exitSignal() {
            signalMaskStack.pop();
        }

        void setSignalDepth(int signalDepth) {
            while (signalMaskStack.size() <= signalDepth) {
                signalMaskStack.push(0L);
            }
            while (numberOfOperations.size() <= signalDepth) {
                numberOfOperations.add(0L);
            }
            while (generation.size() <= signalDepth) {
                generation.add(0L);
            }
            this.signalDepth = signalDepth;
        }

        void maskSignals(long signalMask) {
            // TODO: This is defined in a different way in the documentation.
            // Why is there a signal depth which can be set to a given value
            // instead of just pushing and popping?
            signalMaskStack.push(signalMaskStack.pop() | signalMask);
        }

        void end() {
            state = State.ENDED;
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

        void newOperation() {
            numberOfOperations.set(signalDepth, numberOfOperations.get(signalDepth) + 1);
        }

        long newId() {
            return (generation.get(signalDepth) << 48)
                    | (numberOfOperations.get(signalDepth) << 16)
                    | threadId;
        }

        long getGeneration() {
            return generation.get(signalDepth);
        }
    }
}
