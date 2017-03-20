package com.runtimeverification.rvpredict.log.compact;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private final Map<Long, ThreadState> threadIdToState;
    private ThreadState currentThread;
    private int outstandingInterruptCount;


    public Context() {
        threadIdToState = new HashMap<>();
    }

    public void jump(long address) {
        currentThread.setLastPC(outstandingInterruptCount, address);
    }

    public void updatePcWithDelta(int jumpDelta) {
        currentThread.setLastPC(
                outstandingInterruptCount, currentThread.getLastPC() + jumpDelta);
    }

    private class ThreadState {
        private long lastPC;

        private void setLastPC(long outstandingInterruptCount, long programCounter) {
            if (outstandingInterruptCount != 0) {
                throw new IllegalArgumentException("nintrOutstanding must always be 0.");
            }
            lastPC = programCounter;
        }

        public long getLastPC() {
            return lastPC;
        }
    }
}
