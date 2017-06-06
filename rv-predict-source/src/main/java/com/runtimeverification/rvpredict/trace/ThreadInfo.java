package com.runtimeverification.rvpredict.trace;

class ThreadInfo {
    private final ThreadType threadType;
    private final int id;
    private final long originalThreadId;
    private final long signalNumber;
    private final long signalHandler;
    private final int signalDepth;

    ThreadInfo(
            ThreadType threadType,
            int id,
            long originalThreadId,
            long signalNumber, long signalHandler, int signalDepth) {
        this.threadType = threadType;
        this.id = id;
        this.originalThreadId = originalThreadId;
        this.signalNumber = signalNumber;
        this.signalHandler = signalHandler;
        this.signalDepth = signalDepth;
    }

    public int getId() {
        return id;
    }

    ThreadType getThreadType() {
        return threadType;
    }

    public long getSignalNumber() {
        return signalNumber;
    }

    long getSignalHandler() {
        return signalHandler;
    }

    public long getOriginalThreadId() {
        return originalThreadId;
    }

    public int getSignalDepth() {
        return signalDepth;
    }
}
