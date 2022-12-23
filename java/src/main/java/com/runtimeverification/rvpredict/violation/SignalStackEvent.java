package com.runtimeverification.rvpredict.violation;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.trace.ThreadType;
import com.runtimeverification.rvpredict.trace.Trace;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collectors;

public class SignalStackEvent {
    private final Optional<ReadonlyEventInterface> event;
    private final int ttid;
    private final Collection<ReadonlyEventInterface> stackTrace;
    private final ThreadType threadType;
    private final long originalThreadId;
    private final OptionalLong signalNumber;
    private final List<HeldLock> heldLocks;

    public static SignalStackEvent fromEventAndTrace(ReadonlyEventInterface event, Trace trace) {
        OptionalInt maybeTtid = trace.getTraceThreadId(event);
        assert maybeTtid.isPresent();
        int ttid = maybeTtid.getAsInt();
        return fromEvent(
                event,
                ttid,
                trace.getStacktraceAt(event),
                trace.getThreadType(ttid),
                trace.getOriginalThreadIdForTraceThreadId(ttid),
                trace.maybeGetSignalNumber(ttid),
                trace.getHeldLocksAt(event).stream()
                        .map(e -> new HeldLock(e, trace.getStacktraceAt(e)))
                        .collect(Collectors.toList()));
    }

    private static SignalStackEvent fromEvent(
            ReadonlyEventInterface event,
            int ttid,
            Collection<ReadonlyEventInterface> stackTrace,
            ThreadType threadType,
            long originalThreadId,
            OptionalLong signalNumber,
            List<HeldLock> heldLocks) {
        return new SignalStackEvent(
                Optional.of(event), ttid, stackTrace, threadType, originalThreadId, signalNumber, heldLocks);
    }

    public static SignalStackEvent fromBeforeFirstEvent(
            int ttid,
            Collection<ReadonlyEventInterface> stackTrace,
            ThreadType threadType,
            long originalThreadId,
            OptionalLong signalNumber) {
        return new SignalStackEvent(
                Optional.empty(),
                ttid,
                stackTrace,
                threadType,
                originalThreadId,
                signalNumber,
                Collections.emptyList());
    }

    private SignalStackEvent(
            Optional<ReadonlyEventInterface> event,
            int ttid,
            Collection<ReadonlyEventInterface> stackTrace,
            ThreadType threadType,
            long originalThreadId,
            OptionalLong signalNumber,
            List<HeldLock> heldLocks) {
        this.event = event;
        this.ttid = ttid;
        this.stackTrace = stackTrace;
        this.threadType = threadType;
        this.originalThreadId = originalThreadId;
        this.signalNumber = signalNumber;
        this.heldLocks = heldLocks;
    }

    public Optional<ReadonlyEventInterface> getEvent() {
        return event;
    }

    public int getTtid() {
        return ttid;
    }

    public ThreadType getThreadType() {
        return threadType;
    }

    public long getOriginalThreadId() {
        return originalThreadId;
    }

    public OptionalLong getSignalNumber() {
        return signalNumber;
    }

    List<HeldLock> getHeldLocks() {
        return heldLocks;
    }

    Collection<ReadonlyEventInterface> getStackTrace() {
        return stackTrace;
    }
}
