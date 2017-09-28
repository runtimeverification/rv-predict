package com.runtimeverification.rvpredict.trace;

import java.util.OptionalInt;
import java.util.OptionalLong;

public class ThreadInfo {
    private final ThreadType threadType;
    private final int id;
    private final long originalThreadId;
    private final OptionalInt parentTtid;
    private final OptionalLong signalNumber;
    private final OptionalLong signalHandler;
    private final int signalDepth;

    public static ThreadInfo createThreadInfo(
            int id,
            long originalThreadId,
            OptionalInt parentTtid) {
        return new ThreadInfo(
                ThreadType.THREAD, id, originalThreadId, parentTtid,
                OptionalLong.empty(), OptionalLong.empty(), 0);
    }

    public static ThreadInfo createSignalInfo(
            int id,
            long originalThreadId,
            long signalNumber,
            long signalHandler,
            int signalDepth) {
        return new ThreadInfo(
                ThreadType.SIGNAL, id, originalThreadId, OptionalInt.empty(),
                OptionalLong.of(signalNumber), OptionalLong.of(signalHandler), signalDepth);
    }

    private ThreadInfo(
            ThreadType threadType,
            int id,
            long originalThreadId,
            OptionalInt parentTtid,
            OptionalLong signalNumber, OptionalLong signalHandler, int signalDepth) {
        this.threadType = threadType;
        this.id = id;
        this.originalThreadId = originalThreadId;
        this.parentTtid = parentTtid;
        this.signalNumber = signalNumber;
        this.signalHandler = signalHandler;
        this.signalDepth = signalDepth;
    }

    public int getId() {
        return id;
    }

    public ThreadType getThreadType() {
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

    public OptionalInt getParentTtid() {
        return parentTtid;
    }
}
