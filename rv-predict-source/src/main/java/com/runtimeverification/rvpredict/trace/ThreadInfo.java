package com.runtimeverification.rvpredict.trace;

import java.util.OptionalLong;

public class ThreadInfo {
    private final ThreadType threadType;
    private final int id;
    private final long originalThreadId;
    private final OptionalLong signalNumber;
    private final OptionalLong signalHandler;
    private final int signalDepth;

    public ThreadInfo(
            ThreadType threadType,
            int id,
            long originalThreadId,
            OptionalLong signalNumber, OptionalLong signalHandler, int signalDepth) {
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

    public OptionalLong getSignalNumber() {
        return signalNumber;
    }

    OptionalLong getSignalHandler() {
        return signalHandler;
    }

    public long getOriginalThreadId() {
        return originalThreadId;
    }

    public int getSignalDepth() {
        return signalDepth;
    }
}
