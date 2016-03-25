package com.runtimeverification.rvpredict.trace;

import com.google.common.collect.Table;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Encapsulates all the necessary information when a fork is made
 * @author EricPts
 */
public class ForkPoint {
    private final TraceState traceState;

    /**
     * Stores the GID of the FORK event
     * so that the trace can be resumed at that point
     */
    private long fromIndex;

    /**
     *
     * @param parentState the state of the trace when the fork occured
     * @param parentIndex the GID of the FORK event
     */
    public ForkPoint(TraceState parentState, long parentIndex) {
        traceState = new TraceState(parentState.config(), parentState.metadata(), parentState.getLockGraph().copy());
        LongToObjectMap.EntryIterator it = parentState.getTidToStacktrace().iterator();
        while (it.hasNext()) {
            traceState.getTidToStacktrace().put(it.getNextKey(), new ArrayDeque<>((Deque) it.getNextValue()));
            it.incCursor();
        }

        for (Table.Cell<Long, Long, LockState> cell : parentState.getTidToLockIdToLockState().cellSet()) {
            traceState.getTidToLockIdToLockState().put(cell.getRowKey(), cell.getColumnKey(), cell.getValue().copy());
        }
        fromIndex = parentIndex;
    }

    public TraceState getTraceState() {
        return traceState;
    }

    public long getFromIndex() {
        return fromIndex;
    }
}
