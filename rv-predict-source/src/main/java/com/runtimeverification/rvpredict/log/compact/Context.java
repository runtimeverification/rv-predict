package com.runtimeverification.rvpredict.log.compact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Context {
    static final long INVALID_SIGNAL_NUMBER = -1;

    private static final long INVALID_PC = -1;
    private static final long INVALID_GENERATION = -1;

    private final Map<Long, ThreadState> threadIdToState;
    private final Map<Long, Long> memoizedSignalMasks;
    private final long minDeltaAndEventType;

    private ThreadState currentThread;

    public Context(long minDeltaAndEventType) {
        threadIdToState = new HashMap<>();
        memoizedSignalMasks = new HashMap<>();
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
                threadId, tid -> new ThreadState(threadId));
        currentThread.initIfNeeded(minDeltaAndEventType);
        currentThread.begin(generation);
        threadIdToState.put(currentThread.getThreadId(), currentThread);
    }

    void endThread() {
        currentThread.end();
    }

    void startThread(long threadId) {
    }

    void joinThread(long otherThreadId) {
        threadIdToState.get(otherThreadId).wasJoined();
    }

    void switchThread(long threadId) throws InvalidTraceDataException {
        currentThread = threadIdToState.computeIfAbsent(
                threadId, tid -> new ThreadState(threadId));
        currentThread.initIfNeeded(minDeltaAndEventType);
        currentThread.setSignalDepth(0, false);
    }

    void enterSignal(long signalNumber, long generation) throws InvalidTraceDataException {
        currentThread.enterSignal(signalNumber, generation);
    }

    void exitSignal() throws InvalidTraceDataException {
        currentThread.exitSignal();
    }

    void establishSignal(long handlerAddress, long signalNumber, long signalMaskNumber) {
        // Since this information may be written in the trace after the signal
        // events, it's likely that in can be handled reasonably only when analysing the trace,
        // so it's probably not part of the context.
    }

    void disestablishSignal(long signalNumber) {
        // Since this information may be written in the trace after the signal
        // events, it's likely that in can be handled reasonably only when analysing the trace,
        // so it's probably not part of the context.
    }

    void maskSignals(long signalMask) {
        // Since this information may be written in the trace after the signal
        // events, it's likely that in can be handled reasonably only when analysing the trace,
        // so it's probably not part of the context.
    }

    void setSignalDepth(int signalDepth) throws InvalidTraceDataException {
        currentThread.setSignalDepth(signalDepth, false);
    }

    void memoizeSignalMask(long signalMask, long originBitCount, long signalMaskNumber) {
        memoizedSignalMasks.put(signalMaskNumber, signalMask << originBitCount);
    }

    long getMemoizedSignalMask(long signalMaskNumber) {
        return memoizedSignalMasks.get(signalMaskNumber);
    }

    private static class ThreadState {
        private enum State {
            NOT_STARTED,
            STARTED,
            ENDED, UNINITIALIZED,
        }

        private final long threadId;
        private final List<PerSignalState> signalStack;
        private PerSignalState currentSignalState;

        private int signalDepth;
        private State state;

        private ThreadState(long threadId) {
            this.threadId = threadId;
            this.signalStack = new ArrayList<>();
            state = State.UNINITIALIZED;
        }

        private void initIfNeeded(long initialPC) throws InvalidTraceDataException {
            if (state == State.UNINITIALIZED) {
                setSignalDepth(0, true);
                currentSignalState.lastPC = initialPC;
                state = State.NOT_STARTED;
            }
        }

        private void begin(long generation) throws InvalidTraceDataException {
            if (state != State.NOT_STARTED && state != State.ENDED) {
                throw new InvalidTraceDataException(
                        "Attempting to (re)start thread that did not end, state = " + state + ".");
            }
            state = State.STARTED;

            setSignalDepth(0, true);
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

        void enterSignal(long signalNumber, long generation) throws InvalidTraceDataException {
            setSignalDepth(signalDepth + 1, false);
            setGeneration(generation);
            currentSignalState.signalNumber = signalNumber;
        }

        void exitSignal() throws InvalidTraceDataException {
            currentSignalState.state = PerSignalState.State.FINISHED;
            setSignalDepth(signalDepth - 1, false);
        }

        void setSignalDepth(int signalDepth, boolean reset) throws InvalidTraceDataException {
            while (signalStack.size() <= signalDepth) {
                signalStack.add(createUnstartedSignalState());
            }
            if (reset) {
                PerSignalState state = signalStack.get(signalDepth);
                if (state.state != PerSignalState.State.UNSTARTED
                        && state.state != PerSignalState.State.FINISHED) {
                    throw new InvalidTraceDataException("Starting a signal on the same level as an ongoing one.");
                }
                if (state.state != PerSignalState.State.UNSTARTED) {
                    state = createUnstartedSignalState();
                    signalStack.set(signalDepth, state);
                    state.state = PerSignalState.State.RUNNING;
                }
            }
            this.signalDepth = signalDepth;
            this.currentSignalState = signalStack.get(signalDepth);
        }

        private PerSignalState createUnstartedSignalState() {
            return new PerSignalState(
                    INVALID_SIGNAL_NUMBER,
                    INVALID_PC,
                    0,
                    INVALID_GENERATION);
        }

        void end() {
            state = State.ENDED;
        }

        void wasJoined() {

        }

        long getSignalNumber() {
            return currentSignalState.signalNumber;
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
            private State state;

            private PerSignalState(long signalNumber, long lastPC, long numberOfOperations, long generation) {
                this.signalNumber = signalNumber;
                this.lastPC = lastPC;
                this.numberOfOperations = numberOfOperations;
                this.generation = generation;
                this.state = State.UNSTARTED;
            }

            private enum State {RUNNING, FINISHED, UNSTARTED}
        }
    }
}
