package com.runtimeverification.rvpredict.trace;

class ThreadInfo {
    private final ThreadType threadType;
    private final int id;
    private final long originalThreadId;
    private final long signalNumber;
    private final long signalHandler;
    private final int signalDepth;
    // TODO(virgil): This is not the right place for these bits, they are not thread-specific, they are
    // thread-and-window-specific.
    private final boolean threadStartsInTheCurrentWindow;
    private final boolean signalEndsInTheCurrentWindow;

    ThreadInfo(
            ThreadType threadType,
            int id,
            long originalThreadId,
            long signalNumber, long signalHandler, int signalDepth,
            boolean threadStartsInTheCurrentWindow,
            boolean signalEndsInTheCurrentWindow) {
        this.threadType = threadType;
        this.id = id;
        this.originalThreadId = originalThreadId;
        this.signalNumber = signalNumber;
        this.signalHandler = signalHandler;
        this.signalDepth = signalDepth;
        this.threadStartsInTheCurrentWindow = threadStartsInTheCurrentWindow;
        this.signalEndsInTheCurrentWindow = signalEndsInTheCurrentWindow;
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

    boolean getThreadStartsInTheCurrentWindow() {
        return threadStartsInTheCurrentWindow;
    }

    boolean getSignalEndsInTheCurrentWindow() {
        return signalEndsInTheCurrentWindow;
    }
}
