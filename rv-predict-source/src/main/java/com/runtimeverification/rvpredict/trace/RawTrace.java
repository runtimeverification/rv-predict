package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ILoggingEngine;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.util.Constants;

import java.util.Arrays;
import java.util.Optional;

/**
 * Unprocessed trace of events, implemented as a thin wrapper around the array
 * of events obtained from an {@link ILoggingEngine}.
 *
 * @author YilongL
 */
public class RawTrace {

    private final ThreadInfo threadInfo;

    private final int signalDepth;

    private final int start;

    private final int size;

    private final int mask;

    private final ReadonlyEventInterface[] events;

    public RawTrace(int start, int end, ReadonlyEventInterface[] events, int signalDepth, int threadId) {
        long signalNumber = com.runtimeverification.rvpredict.util.Constants.INVALID_SIGNAL;
        if (signalDepth != 0) {
            for (int i = start; i < end; i++) {
                ReadonlyEventInterface event = events[i];
                if (event.getType() == EventType.ENTER_SIGNAL) {
                    signalNumber = event.getSignalNumber();
                    break;
                }
            }
        }
        long originalThreadId = Constants.INVALID_THREAD_ID;
        if (start < end) {
            originalThreadId = events[start].getOriginalThreadId();
            for (int i = start; i < end; i++) {
                assert events[i].getOriginalThreadId() == originalThreadId;
            }
        }
        this.threadInfo = new ThreadInfo(
                signalDepth == 0 ? ThreadType.THREAD : ThreadType.SIGNAL,
                threadId,
                originalThreadId,
                signalNumber);
        this.signalDepth = signalDepth;
        this.start = start;
        this.mask = events.length - 1;
        this.size = (end - start + events.length) & mask;
        this.events = events;
        if ((events.length & mask) != 0) {
            throw new IllegalArgumentException("The length of events must be a power of two!");
        }
    }

    public int getSignalDepth() {
        return signalDepth;
    }

    public long getMinGID() {
        return events[start].getEventId();
    }

    public int size() {
        return size;
    }

    /**
     * Returns the index of the {@code n}-th event.
     */
    private int getIndex(int n) {
        return (n + start) & mask;
    }

    /**
     * Returns the {@code n}-th event in the trace.
     */
    public ReadonlyEventInterface event(int n) {
        return events[getIndex(n)];
    }

    public ThreadInfo getThreadInfo() {
        return threadInfo;
    }
}
