package com.runtimeverification.rvpredict.log.compact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Context {
    static final long INVALID_SIGNAL_NUMBER = -1;

    private static final long INVALID_PC = -1;
    private static final long INVALID_GENERATION = -1;
    private static final long EMPTY_SIGNAL_MASK = 0;

    private final Map<Long, ThreadState> threadIdToState;
    private final Map<Long, Long> memoizedSignalMasks;
    private final long minDeltaAndEventType;
    private final Map<Long, SignalInformation> signalNumberToInformation;

    private ThreadState currentThread;

    public Context(long minDeltaAndEventType) {
        threadIdToState = new HashMap<>();
        memoizedSignalMasks = new HashMap<>();
        signalNumberToInformation = new HashMap<>();
        this.minDeltaAndEventType = minDeltaAndEventType;
    }

    long newId() {
        return currentThread.newId();
    }

    long getThreadId() {
        return currentThread.getThreadId();
    }

    long getPC() {
        return currentThread.getLastPC();
    }

    long getGeneration() {
        return currentThread.getGeneration();
    }

    long getSignalNumber() {
        return currentThread.getSignalNumber();
    }

    void jump(long address) throws InvalidTraceDataException {
        currentThread.setLastPC(address);
        currentThread.newOperation();
    }

    void updatePcWithDelta(int jumpDelta) throws InvalidTraceDataException {
        currentThread.setLastPC(currentThread.getLastPC() + jumpDelta);
        currentThread.newOperation();
    }

    void changeOfGeneration(long generation) {
        currentThread.setGeneration(generation);
    }

    void beginThread(long threadId, long generation) throws InvalidTraceDataException {
        currentThread = threadIdToState.computeIfAbsent(
                threadId, tid -> new ThreadState(minDeltaAndEventType, threadId));
        currentThread.begin(generation);
        threadIdToState.put(currentThread.getThreadId(), currentThread);
    }

    void endThread() {
        currentThread.end();
    }

    void forkThread(long threadId) {
    }

    void joinThread(long otherThreadId) {
        threadIdToState.get(otherThreadId).wasJoined();
    }

    void switchThread(long threadId) throws InvalidTraceDataException {
        currentThread = threadIdToState.computeIfAbsent(
                threadId, tid -> new ThreadState(minDeltaAndEventType, threadId));
        // TODO: Why, oh why?
        currentThread.setSignalDepth(0);
    }

    void enterSignal(long signalNumber, long generation) throws InvalidTraceDataException {
        currentThread.enterSignal(
                signalNumber, generation, signalNumberToInformation.get(signalNumber).getSignalMask());
    }

    void exitSignal() {
        currentThread.exitSignal();
    }

    void establishSignal(long handlerAddress, long signalNumber, long signalMaskNumber) {
        // TODO(virgil): Since this information may be written in the trace after the signal
        // events, it's probably not part of the context.
        signalNumberToInformation.put(
                signalNumber,
                new SignalInformation(handlerAddress, getMemoizedSignalMask(signalMaskNumber)));
    }

    void disestablishSignal(long signalNumber) {
        signalNumberToInformation.remove(signalNumber);
    }

    void maskSignals(long signalMask) {
        currentThread.maskSignals(signalMask);
    }

    void setSignalDepth(int signalDepth) {
        currentThread.setSignalDepth(signalDepth);
    }

    void memoizeSignalMask(long signalMask, long originBitCount, long signalMaskNumber) {
        memoizedSignalMasks.put(signalMaskNumber, signalMask << originBitCount);
    }

    long getMemoizedSignalMask(long signalMaskNumber) {
        return memoizedSignalMasks.get(signalMaskNumber);
    }

    long getSignalMask() {
        return currentThread.getSignalMask();
    }

    private static class SignalInformation {
        private final long signalMask;
        private final long signalHandler;

        SignalInformation(long signalHandler, long signalMask) {
            this.signalMask = signalMask;
            this.signalHandler = signalHandler;
        }

        long getSignalMask() {
            return signalMask;
        }
    }

    private static class ThreadState {
        private enum State {
            NOT_STARTED,
            STARTED,
            ENDED,
        }

        private final long threadId;
        // private final List<Long> lastPC;
        // private final Stack<Long> signalMaskStack;
        // private final List<Long> numberOfOperations;
        // private final List<Long> generation;
        // private final HashMap<Long, Long> signalNumberToMask;
        // private final Map<Long, Long> signalNumberToHandlerAddress;
        private final List<PerSignalState> signalStack;
        private PerSignalState currentSignalState;

        private int signalDepth;
        private State state;

        private ThreadState(long initialPC, long threadId) {
            this.threadId = threadId;
            this.signalStack = new ArrayList<>();
            setSignalDepth(0);
            currentSignalState.lastPC = initialPC;
            state = State.NOT_STARTED;
        }

        private void begin(long generation) throws InvalidTraceDataException {
            if (state != State.NOT_STARTED && state != State.ENDED) {
                throw new InvalidTraceDataException(
                        "Attempting to (re)start thread that did not end, state = " + state + ".");
            }
            state = State.STARTED;

            setSignalDepth(0);
            setGeneration(generation);
            currentSignalState.generation = generation;
        }

        private void setLastPC(long programCounter) {
            currentSignalState.lastPC = programCounter;
        }

        private long getLastPC() {
            return currentSignalState.lastPC;
        }

        private long getThreadId() {
            return threadId;
        }

        void setGeneration(long generation) {
            if (generation != currentSignalState.generation) {
                currentSignalState.generation = generation;
                currentSignalState.numberOfOperations = 0;
            }
        }

        void enterSignal(long signalNumber, long generation, long signalMask) throws InvalidTraceDataException {
            setSignalDepth(signalDepth + 1);
            setGeneration(generation);
            currentSignalState.signalNumber = signalNumber;
            currentSignalState.signalMask = signalStack.get(signalDepth - 1).signalMask | signalMask;
        }

        void exitSignal() {
            setSignalDepth(signalDepth - 1);
        }

        void setSignalDepth(int signalDepth) {
            // TODO: Reset the signal state, at least sometimes.
            while (signalStack.size() <= signalDepth) {
                signalStack.add(new PerSignalState(
                        INVALID_SIGNAL_NUMBER,
                        INVALID_PC,
                        0,
                        INVALID_GENERATION,
                        // TODO: Is this a good initial value for the signal mask?
                        EMPTY_SIGNAL_MASK));
            }
            while (signalStack.size() > signalDepth + 1) {
                signalStack.remove(signalStack.size() - 1);
            }
            this.signalDepth = signalDepth;
            this.currentSignalState = signalStack.get(signalDepth);
        }

        void maskSignals(long signalMask) {
            // TODO: This is defined in a different way in the documentation.
            // Why is there a signal depth which can be set to a given value
            // instead of just pushing and popping?
            // signalMaskStack.push(signalMaskStack.pop() | signalMask);
            currentSignalState.signalMask |= signalMask;
        }

        void end() {
            state = State.ENDED;
        }

        void wasJoined() {

        }

        long getSignalNumber() {
            return currentSignalState.signalNumber;
        }

        long getSignalMask() {
            return currentSignalState.signalMask;
        }

        void newOperation() {
            currentSignalState.numberOfOperations++;
        }

        long newId() {
            return (currentSignalState.generation << 48)
                    | (currentSignalState.numberOfOperations << 16)
                    | threadId;
        }

        long getGeneration() {
            return currentSignalState.generation;
        }

        private static class PerSignalState {
            private long signalNumber;
            private long lastPC;
            private long numberOfOperations;
            private long generation;
            private long signalMask;

            PerSignalState(long signalNumber, long lastPC, long numberOfOperations, long generation, long signalMask) {
                this.signalNumber = signalNumber;
                this.lastPC = lastPC;
                this.numberOfOperations = numberOfOperations;
                this.generation = generation;
                this.signalMask = signalMask;
            }
        }
    }
}
