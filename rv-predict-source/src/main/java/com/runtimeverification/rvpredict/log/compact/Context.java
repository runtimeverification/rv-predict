package com.runtimeverification.rvpredict.log.compact;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Context {
    private final Map<Long, ThreadState> threadIdToState;
    private ThreadState currentThread;
    private int outstandingInterruptCount;
    private int jumpDeltaForBegin;

    public Context() {
        threadIdToState = new HashMap<>();
    }

    public void jump(long address) {
        currentThread.setLastPC(outstandingInterruptCount, address);
    }

    public void updatePcWithDelta(int jumpDelta) {
        currentThread.setLastPC(
                outstandingInterruptCount,
                currentThread.getLastPC(outstandingInterruptCount) + jumpDelta);
    }

    public void setJumpDeltaForBegin(int jumpDeltaForBegin) {
        this.jumpDeltaForBegin = jumpDeltaForBegin;
    }

    public void beginThread(
            long deltop_first, ThreadId threadId, Generation generation)
            throws InvalidTraceDataException {
        if (threadIdToState.containsKey(threadId.getAsLong())) {
            throw new InvalidTraceDataException("Thread started twice: " + threadId.getAsLong() + ".");
        }
        currentThread =
                new ThreadState(deltop_first + jumpDeltaForBegin, threadId, generation);
        threadIdToState.put(currentThread.getThreadId(), currentThread);
        // TODO(virgil): Huh? Why isn't the outstanding interrupt count in the thread's state?
        outstandingInterruptCount = 0;
    }

    public void enterSignal(Generation generation) throws InvalidTraceDataException {
        if (outstandingInterruptCount <= 0) {
            throw new InvalidTraceDataException("Cannot enter signal without outstanding interrupts.");
        }
        currentThread.setGeneration(outstandingInterruptCount, generation);
    }

    private class ThreadState {
        private final long threadId;
        private final List<Long> lastPC;
        private final Stack<Long> callStack;
        private final List<Long> generation;
        // TODO(virgi): What does nops mean?
        private final List<Long> nops;
        private boolean signalsMasked;

        private ThreadState(long initialPC, ThreadId threadId, Generation generation) {
            this.threadId = threadId.getAsLong();
            lastPC = new ArrayList<>();
            lastPC.add(initialPC);
            callStack = new Stack<>();
            this.generation = new ArrayList<>();
            this.generation.add(generation.getAsLong());
            this.generation.add(0L);
            this.nops = new ArrayList<>();
            this.nops.add(0L);
            this.signalsMasked = false;
        }

        private void setLastPC(int outstandingInterruptCount, long programCounter) {
            lastPC.set(outstandingInterruptCount, programCounter);
        }

        private long getLastPC(int outstandingInterruptCount) {
            return lastPC.get(outstandingInterruptCount);
        }

        private long getThreadId() {
            return threadId;
        }

        public void setGeneration(int outstandingInterruptCount, Generation generation) {
            this.generation.set(outstandingInterruptCount, generation.getAsLong());
        }
    }
}
