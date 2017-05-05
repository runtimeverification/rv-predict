package com.runtimeverification.rvpredict.trace;

class ThreadInfo {
    private final ThreadType threadType;
    private final int id;
    private final long originalThreadId;
    private final long signalNumber;

    ThreadInfo(ThreadType threadType, int id, long originalThreadId, long signalNumber) {
        this.threadType = threadType;
        this.id = id;
        this.originalThreadId = originalThreadId;
        this.signalNumber = signalNumber;
    }

    public int getId() {
        return id;
    }

    public ThreadType getThreadType() {
        return threadType;
    }

    public long getSignalNumber() {
        return signalNumber;
    }

    public long getOriginalThreadId() {
        return originalThreadId;
    }
}
