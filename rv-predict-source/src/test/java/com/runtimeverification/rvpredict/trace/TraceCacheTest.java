package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.deadlock.LockGraph;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.testutils.MoreAsserts;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TraceCacheTest {
    private static final long EVENT_ID = 10;
    private static final long LOCATION_ID = 20;
    private static final long THREAD_ID = 30;
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final long SIGNAL_NUMBER = 40;

    @Mock private Configuration mockConfiguration;
    @Mock private TraceState mockTraceState;
    @Mock private LockGraph mockLockGraph;
    @Mock private Trace mockTrace;

    @Captor private ArgumentCaptor<List<RawTrace>> rawTraceArgumentCaptor;

    @Test
    public void createsOneRawTracePerThread() throws IOException {
        mockConfiguration.windowSize = 10;
        when(mockConfiguration.stacks()).thenReturn(false);
        when(mockConfiguration.isLLVMPrediction()).thenReturn(true);
        when(mockTraceState.initNextTraceWindow(any())).thenReturn(mockTrace);
        when(mockTraceState.getUnfinishedThreadId(anyInt(), anyLong())).thenReturn(OptionalInt.empty());

        ReadonlyEventInterface beginThread1 = beginThread(EVENT_ID, LOCATION_ID, THREAD_ID);
        ReadonlyEventInterface readData1Thread1 =
                readData(EVENT_ID + 1, LOCATION_ID + 1, THREAD_ID, NO_SIGNAL);
        ReadonlyEventInterface readData2Thread1 =
                readData(EVENT_ID + 4, LOCATION_ID + 4, THREAD_ID, NO_SIGNAL);
        ReadonlyEventInterface beginThread2 =
                beginThread(EVENT_ID + 2, LOCATION_ID + 2, THREAD_ID + 1);
        ReadonlyEventInterface readDataThread2 =
                readData(EVENT_ID + 3, LOCATION_ID + 3, THREAD_ID + 1, NO_SIGNAL);

        ListEventReader eventReader =
                new ListEventReader(beginThread1, readData1Thread1, beginThread2, readDataThread2, readData2Thread1);

        TraceCache traceCache =
                TraceCache.createForTesting(
                        mockConfiguration, mockTraceState, mockLockGraph, Collections.singletonList(eventReader));

        Assert.assertEquals(mockTrace, traceCache.getTraceWindow());

        verify(mockTraceState).initNextTraceWindow(rawTraceArgumentCaptor.capture());

        List<RawTrace> rawTraces = rawTraceArgumentCaptor.getValue();
        MoreAsserts.assertNotNull(rawTraces);

        Assert.assertEquals(2, rawTraces.size());
        Assert.assertEquals(3, rawTraces.get(0).size());
        Assert.assertEquals(beginThread1, rawTraces.get(0).event(0));
        Assert.assertEquals(readData1Thread1, rawTraces.get(0).event(1));
        Assert.assertEquals(readData2Thread1, rawTraces.get(0).event(2));
        Assert.assertEquals(2, rawTraces.get(1).size());
        Assert.assertEquals(beginThread2, rawTraces.get(1).event(0));
        Assert.assertEquals(readDataThread2, rawTraces.get(1).event(1));
    }

    @Test
    public void createsOneRawTracePerSignal() throws IOException {
        mockConfiguration.windowSize = 10;
        when(mockConfiguration.stacks()).thenReturn(false);
        when(mockConfiguration.isLLVMPrediction()).thenReturn(true);
        when(mockTraceState.initNextTraceWindow(any())).thenReturn(mockTrace);
        when(mockTraceState.getUnfinishedThreadId(anyInt(), anyLong())).thenReturn(OptionalInt.empty());

        ReadonlyEventInterface beginThread1 = beginThread(EVENT_ID, LOCATION_ID, THREAD_ID);
        ReadonlyEventInterface readData1Thread1 =
                readData(EVENT_ID + 1, LOCATION_ID + 1, THREAD_ID, NO_SIGNAL);
        ReadonlyEventInterface readData2Thread1 =
                readData(EVENT_ID + 4, LOCATION_ID + 4, THREAD_ID, NO_SIGNAL);
        ReadonlyEventInterface beginSignal =
                beginSignal(SIGNAL_NUMBER, EVENT_ID + 2, LOCATION_ID + 2, THREAD_ID, ONE_SIGNAL);
        ReadonlyEventInterface readDataSignal =
                readData(EVENT_ID + 3, LOCATION_ID + 3, THREAD_ID, ONE_SIGNAL);

        ListEventReader eventReader =
                new ListEventReader(beginThread1, readData1Thread1, beginSignal, readDataSignal, readData2Thread1);

        TraceCache traceCache =
                TraceCache.createForTesting(
                        mockConfiguration, mockTraceState, mockLockGraph, Collections.singletonList(eventReader));

        Assert.assertEquals(mockTrace, traceCache.getTraceWindow());

        verify(mockTraceState).initNextTraceWindow(rawTraceArgumentCaptor.capture());

        List<RawTrace> rawTraces = rawTraceArgumentCaptor.getValue();
        MoreAsserts.assertNotNull(rawTraces);

        Assert.assertEquals(2, rawTraces.size());
        Assert.assertEquals(3, rawTraces.get(0).size());
        Assert.assertEquals(0, rawTraces.get(0).getSignalDepth());
        Assert.assertEquals(beginThread1, rawTraces.get(0).event(0));
        Assert.assertEquals(readData1Thread1, rawTraces.get(0).event(1));
        Assert.assertEquals(readData2Thread1, rawTraces.get(0).event(2));
        Assert.assertEquals(2, rawTraces.get(1).size());
        Assert.assertEquals(1, rawTraces.get(1).getSignalDepth());
        Assert.assertEquals(beginSignal, rawTraces.get(1).event(0));
        Assert.assertEquals(readDataSignal, rawTraces.get(1).event(1));
    }

    @Test
    public void splitsConsecutiveSignalRunsIntoRawTraces() throws IOException {
        mockConfiguration.windowSize = 10;
        when(mockConfiguration.stacks()).thenReturn(false);
        when(mockConfiguration.isLLVMPrediction()).thenReturn(true);
        when(mockTraceState.initNextTraceWindow(any())).thenReturn(mockTrace);
        when(mockTraceState.getUnfinishedThreadId(anyInt(), anyLong())).thenReturn(OptionalInt.empty());

        ReadonlyEventInterface beginThread1 = beginThread(EVENT_ID, LOCATION_ID, THREAD_ID);
        ReadonlyEventInterface readData1Thread1 =
                readData(EVENT_ID + 1, LOCATION_ID + 1, THREAD_ID, NO_SIGNAL);
        ReadonlyEventInterface readData2Thread1 =
                readData(EVENT_ID + 8, LOCATION_ID + 8, THREAD_ID, NO_SIGNAL);

        ReadonlyEventInterface beginSignal1 =
                beginSignal(SIGNAL_NUMBER, EVENT_ID + 2, LOCATION_ID + 2, THREAD_ID, ONE_SIGNAL);
        ReadonlyEventInterface readDataSignal1 =
                readData(EVENT_ID + 3, LOCATION_ID + 3, THREAD_ID, ONE_SIGNAL);
        ReadonlyEventInterface endSignal1 =
                endSignal(EVENT_ID + 4, LOCATION_ID + 4, THREAD_ID, ONE_SIGNAL);

        ReadonlyEventInterface beginSignal2 =
                beginSignal(SIGNAL_NUMBER, EVENT_ID + 5, LOCATION_ID + 5, THREAD_ID, ONE_SIGNAL);
        ReadonlyEventInterface readDataSignal2 =
                readData(EVENT_ID + 6, LOCATION_ID + 6, THREAD_ID, ONE_SIGNAL);
        ReadonlyEventInterface endSignal2 =
                endSignal(EVENT_ID + 7, LOCATION_ID + 7, THREAD_ID, ONE_SIGNAL);

        ListEventReader eventReader =
                new ListEventReader(
                        beginThread1, readData1Thread1,
                        beginSignal1, readDataSignal1, endSignal1,
                        beginSignal2, readDataSignal2, endSignal2,
                        readData2Thread1);

        TraceCache traceCache =
                TraceCache.createForTesting(
                        mockConfiguration, mockTraceState, mockLockGraph, Collections.singletonList(eventReader));

        Assert.assertEquals(mockTrace, traceCache.getTraceWindow());

        verify(mockTraceState).initNextTraceWindow(rawTraceArgumentCaptor.capture());

        List<RawTrace> rawTraces = rawTraceArgumentCaptor.getValue();
        MoreAsserts.assertNotNull(rawTraces);

        Assert.assertEquals(3, rawTraces.size());

        Assert.assertEquals(3, rawTraces.get(0).size());
        Assert.assertEquals(0, rawTraces.get(0).getSignalDepth());
        Assert.assertEquals(beginThread1, rawTraces.get(0).event(0));
        Assert.assertEquals(readData1Thread1, rawTraces.get(0).event(1));
        Assert.assertEquals(readData2Thread1, rawTraces.get(0).event(2));

        Assert.assertEquals(3, rawTraces.get(1).size());
        Assert.assertEquals(1, rawTraces.get(1).getSignalDepth());
        Assert.assertEquals(beginSignal1, rawTraces.get(1).event(0));
        Assert.assertEquals(readDataSignal1, rawTraces.get(1).event(1));
        Assert.assertEquals(endSignal1, rawTraces.get(1).event(2));

        Assert.assertEquals(3, rawTraces.get(2).size());
        Assert.assertEquals(1, rawTraces.get(2).getSignalDepth());
        Assert.assertEquals(beginSignal2, rawTraces.get(2).event(0));
        Assert.assertEquals(readDataSignal2, rawTraces.get(2).event(1));
        Assert.assertEquals(endSignal2, rawTraces.get(2).event(2));
    }

    private ReadonlyEventInterface endSignal(long eventId, long locationId, long threadId, int signalDepth) {
        return new CompactEvent(eventId, locationId, threadId, signalDepth, EventType.EXIT_SIGNAL) {};
    }

    private ReadonlyEventInterface beginSignal(
            long signalNumber, long eventId, long locationId, long threadId, int signalDepth) {
        return new CompactEvent(eventId, locationId, threadId, signalDepth, EventType.ENTER_SIGNAL) {
            @Override
            public long getSignalNumber() {
                return signalNumber;
            }
        };
    }

    private ReadonlyEventInterface readData(long eventId, long locationId, long threadId, int signalDepth) {
        return new CompactEvent(eventId, locationId, threadId, signalDepth, EventType.READ) {};
    }

    private static CompactEvent beginThread(long eventId, long locationId, long threadId) {
        return new CompactEvent(eventId, locationId, threadId, NO_SIGNAL, EventType.BEGIN_THREAD) {};
    }

    private static class ListEventReader implements IEventReader {

        private final List<ReadonlyEventInterface> events;

        private int eventIndex;
        private ReadonlyEventInterface lastReadEvent;

        private ListEventReader(ReadonlyEventInterface... events) throws IOException {
            this.events = Arrays.asList(events);
            eventIndex = 0;
            readEvent();
        }

        @Override
        public ReadonlyEventInterface readEvent() throws IOException {
            if (eventIndex >= events.size()) {
                lastReadEvent = null;
                return null;
            }
            lastReadEvent = events.get(eventIndex);
            eventIndex++;
            return lastReadEvent;
        }

        @Override
        public ReadonlyEventInterface lastReadEvent() {
            return lastReadEvent;
        }

        @Override
        public void close() throws IOException {

        }
    }
}
