package com.runtimeverification.rvpredict.log.compact;

import java.util.Collections;
import java.util.List;

public class CompactEventFactory {
    private static final List<CompactEvent> NO_EVENTS = Collections.emptyList();

    public List<CompactEvent> dataManipulation(
            Context context,
            CompactEventReader.DataManipulationType dataManipulationType,
            int dataSizeInBytes,
            long address,
            long value,
            CompactEventReader.Atomicity atomicity) throws InvalidTraceDataException {
        return NO_EVENTS;
    }

    public List<CompactEvent> atomicReadModifyWrite(
            Context context,
            int dataSizeInBytes,
            long address, long readValue, long writeValue) throws InvalidTraceDataException {
        return NO_EVENTS;
    }

    public List<CompactEvent> changeOfGeneration(Context context, long generation) {
        return NO_EVENTS;
    }

    public List<CompactEvent> lockManipulation(
            Context context, CompactEventReader.LockManipulationType lockManipulationType, long address)
            throws InvalidTraceDataException {
        return NO_EVENTS;
    }

    static List<CompactEvent> jump(Context context, long address) throws InvalidTraceDataException {
        return NO_EVENTS;
    }

    // Signal events.

    public List<CompactEvent> establishSignal(
            Context context,
            long handler, long signalNumber, long signalMaskNumber) {
        return NO_EVENTS;
    }

    public List<CompactEvent> disestablishSignal(
            Context context, long signalNumber) {
        return NO_EVENTS;
    }

    public List<CompactEvent> enterSignal(
            Context context, long generation, long signalNumber) throws InvalidTraceDataException {
        return NO_EVENTS;
    }

    static List<CompactEvent> exitSignal(Context context) {
        return NO_EVENTS;
    }

    public List<CompactEvent> signalOutstandingDepth(Context context, int signalDepth) {
        return NO_EVENTS;
    }

    public List<CompactEvent> signalMaskMemoization(
            Context context, long signalMask, long originBitCount, long signalMaskNumber) {
        return NO_EVENTS;
    }

    public List<CompactEvent> signalMask(Context context, long signalMaskNumber) {
        return NO_EVENTS;
    }

    // Function events.

    static List<CompactEvent> enterFunction(Context context) {
        return NO_EVENTS;
    }

    static List<CompactEvent> exitFunction(Context context) {
        return NO_EVENTS;
    }

    // Thread events.

    public List<CompactEvent> beginThread(Context context, long threadId, long generation)
            throws InvalidTraceDataException {
        return NO_EVENTS;
    }

    static List<CompactEvent> endThread(Context context) {
        return NO_EVENTS;
    }

    public List<CompactEvent> threadSync(
            Context context, CompactEventReader.ThreadSyncType threadSyncType, long threadId) throws InvalidTraceDataException {
        return NO_EVENTS;
    }
}
