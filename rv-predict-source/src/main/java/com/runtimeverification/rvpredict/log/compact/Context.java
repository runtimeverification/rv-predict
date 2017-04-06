package com.runtimeverification.rvpredict.log.compact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Context {
    static final long INVALID_SIGNAL_NUMBER = -1;

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
        currentThread.enterSignal(generation, signalNumberToInformation.get(signalNumber).getSignalMask());
    }

    void exitSignal() {
        currentThread.exitSignal();
    }

    void establishSignal(long handlerAddress, long signalNumber, long signalMaskNumber) {
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

        SignalInformation(long signalMask, long signalHandler) {
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
        private final List<Long> lastPC;
        private final Stack<Long> signalMaskStack;
        private final List<Long> numberOfOperations;
        private final List<Long> generation;
        // private final HashMap<Long, Long> signalNumberToMask;
        // private final Map<Long, Long> signalNumberToHandlerAddress;
        private int signalDepth;
        private State state;

        private ThreadState(long initialPC, long threadId) {
            this.threadId = threadId;
            lastPC = new ArrayList<>();
            lastPC.add(initialPC);
            this.generation = new ArrayList<>();
            // TODO: How should this be initialized?
            // this.signalNumberToMask = new HashMap<>();
            // this.signalNumberToHandlerAddress = new HashMap<>();
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

        void enterSignal(long generation, long signalMask) throws InvalidTraceDataException {
            setSignalDepth(signalDepth + 1);
            setGeneration(generation);
            // TODO: This is defined in a slightly different way in the docs.
            signalMaskStack.push(signalMaskStack.peek() | signalMask);
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
            if (signalDepth == 0) {
                return INVALID_SIGNAL_NUMBER;
            }
            return signalMaskStack.peek();
        }

        long getSignalMask() {
            return signalMaskStack.get(signalDepth);
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
