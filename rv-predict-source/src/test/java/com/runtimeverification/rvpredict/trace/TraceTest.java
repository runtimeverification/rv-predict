package com.runtimeverification.rvpredict.trace;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToStateMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TraceTest {
    private static final int NO_SIGNAL = 0;
    private static final long THREAD_ID_1 = 100;
    private static final long THREAD_ID_2 = 101;
    private static final long PC_BASE = 200;
    private static final long ADDRESS_1 = 300;
    private static final long ADDRESS_2 = 301;
    private static final long ADDRESS_3 = 302;
    private static final long VALUE_1 = 400;
    private static final long VALUE_2 = 401;
    private static final long SIGNAL_NUMBER_1 = 500;
    private static final long SIGNAL_NUMBER_2 = 501;
    private static final long SIGNAL_HANDLER_1 = 600;
    private static final long SIGNAL_HANDLER_2 = 601;
    private static final long SIGNAL_HANDLER_3 = 602;
    private static final long SIGNAL_MASK_1 = 700;
    private static final long SIGNAL_MASK_2 = 701;
    private static final long SIGNAL_MASK_3 = 702;
    private static final long SIGNAL_MASK_4 = 703;
    private static final long SIGNAL_MASK_5 = 704;

    @Mock private TraceState mockTraceState;
    @Mock private Context mockContext;

    private Map<Long, Integer> eventIdToTtid;
    private Map<Integer, ThreadInfo> ttidToThreadInfo;
    private Map<Integer, List<ReadonlyEventInterface>> tidToEvents;
    private Map<Integer, List<MemoryAccessBlock>> tidToMemoryAccessBlocks;
    private Map<Integer, ThreadState> tidToThreadState;
    private MemoryAddrToStateMap addrToState;
    private Table<Integer, Long, List<ReadonlyEventInterface>> tidToAddrToEvents;
    private Map<Long, List<LockRegion>> lockIdToLockRegions;
    private Set<ReadonlyEventInterface> clinitEvents;
    private Map<Long, Integer> originalTidToTraceTid;
    private Map<Long, Map<Long, List<ReadonlyEventInterface>>> signalNumberToSignalHandlerToEstablishSignalEvents;

    @Before
    public void setUp() {
        eventIdToTtid = new HashMap<>();
        ttidToThreadInfo = new HashMap<>();
        tidToEvents = new HashMap<>();
        tidToMemoryAccessBlocks = new HashMap<>();
        tidToThreadState = new HashMap<>();
        addrToState = new MemoryAddrToStateMap(100);
        tidToAddrToEvents = HashBasedTable.create();
        lockIdToLockRegions = new HashMap<>();
        clinitEvents = new HashSet<>();
        originalTidToTraceTid = new HashMap<>();
        signalNumberToSignalHandlerToEstablishSignalEvents = new HashMap<>();

        when(mockContext.createUniqueDataAddressId(ADDRESS_1)).thenReturn(1L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_2)).thenReturn(2L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_3)).thenReturn(3L);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
    }

    @Test
    public void testSkipsRecurrentPatterns() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        when(mockTraceState.getUnfinishedThreadId(anyInt(), anyInt())).thenReturn(OptionalInt.empty());
        when(mockTraceState.getNewThreadId(THREAD_ID_1)).thenReturn(1);
        when(mockTraceState.getNewThreadId(THREAD_ID_2)).thenReturn(2);
        Trace trace = createTrace(rawTraces);

        Assert.assertEquals(2, trace.eventsByThreadID().size());
        Assert.assertTrue(trace.eventsByThreadID().containsKey(1));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(2));

        List<ReadonlyEventInterface> events = trace.getEvents(1);
        Assert.assertEquals(2, events.size());

        events = trace.getEvents(2);
        Assert.assertEquals(1, events.size());
    }


    @Test
    public void testPatternsWithDifferentPcAreNotRecurrent() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        when(mockTraceState.getUnfinishedThreadId(anyInt(), anyInt())).thenReturn(OptionalInt.empty());
        when(mockTraceState.getNewThreadId(THREAD_ID_1)).thenReturn(1);
        when(mockTraceState.getNewThreadId(THREAD_ID_2)).thenReturn(2);
        Trace trace = createTrace(rawTraces);

        Assert.assertEquals(2, trace.eventsByThreadID().size());
        Assert.assertTrue(trace.eventsByThreadID().containsKey(1));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(2));

        List<ReadonlyEventInterface> events = trace.getEvents(1);
        Assert.assertEquals(5, events.size());

        events = trace.getEvents(2);
        Assert.assertEquals(1, events.size());
    }

    @Test
    public void testRemovesRecurrentPatternsOfLengthGreaterThan1() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_2, VALUE_1),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_2, VALUE_1),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1)));

        when(mockTraceState.getUnfinishedThreadId(anyInt(), anyInt())).thenReturn(OptionalInt.empty());
        when(mockTraceState.getNewThreadId(THREAD_ID_1)).thenReturn(1);
        when(mockTraceState.getNewThreadId(THREAD_ID_2)).thenReturn(2);
        Trace trace = createTrace(rawTraces);

        Assert.assertEquals(2, trace.eventsByThreadID().size());
        Assert.assertTrue(trace.eventsByThreadID().containsKey(1));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(2));

        List<ReadonlyEventInterface> events = trace.getEvents(1);
        Assert.assertEquals(3, events.size());

        events = trace.getEvents(2);
        Assert.assertEquals(2, events.size());
    }

    @Test
    public void testPatternsWithDifferentAddressAreNotRecurrent() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_2, VALUE_1),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_3, VALUE_1),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                        tu.nonAtomicStore(ADDRESS_3, VALUE_1)));

        when(mockTraceState.getUnfinishedThreadId(anyInt(), anyInt())).thenReturn(OptionalInt.empty());
        when(mockTraceState.getNewThreadId(THREAD_ID_1)).thenReturn(1);
        when(mockTraceState.getNewThreadId(THREAD_ID_2)).thenReturn(2);
        Trace trace = createTrace(rawTraces);

        Assert.assertEquals(2, trace.eventsByThreadID().size());
        Assert.assertTrue(trace.eventsByThreadID().containsKey(1));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(2));

        List<ReadonlyEventInterface> events = trace.getEvents(1);
        Assert.assertEquals(5, events.size());

        events = trace.getEvents(2);
        Assert.assertEquals(3, events.size());
    }

    @Test
    public void testPatternsWithDifferentValuesAreNotRecurrent() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_2, VALUE_1),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_2, VALUE_2),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1)));

        when(mockTraceState.getUnfinishedThreadId(anyInt(), anyInt())).thenReturn(OptionalInt.empty());
        when(mockTraceState.getNewThreadId(THREAD_ID_1)).thenReturn(1);
        when(mockTraceState.getNewThreadId(THREAD_ID_2)).thenReturn(2);
        Trace trace = createTrace(rawTraces);

        Assert.assertEquals(2, trace.eventsByThreadID().size());
        Assert.assertTrue(trace.eventsByThreadID().containsKey(1));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(2));

        List<ReadonlyEventInterface> events = trace.getEvents(1);
        Assert.assertEquals(5, events.size());

        events = trace.getEvents(2);
        Assert.assertEquals(2, events.size());
    }

    @Test
    public void extractsEstablishSignalEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setPc(PC_BASE),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, SIGNAL_MASK_1),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, SIGNAL_MASK_2),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_2, SIGNAL_MASK_3),
                        tu.setSignalHandler(SIGNAL_NUMBER_2, SIGNAL_HANDLER_3, SIGNAL_MASK_4)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, SIGNAL_MASK_5),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1)));

        when(mockTraceState.getUnfinishedThreadId(anyInt(), anyInt())).thenReturn(OptionalInt.empty());
        when(mockTraceState.getNewThreadId(THREAD_ID_1)).thenReturn(1);
        when(mockTraceState.getNewThreadId(THREAD_ID_2)).thenReturn(2);
        Trace trace = createTrace(rawTraces);

        Assert.assertEquals(3, trace.getEstablishSignalEvents(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1).size());
        Assert.assertEquals(1, trace.getEstablishSignalEvents(SIGNAL_NUMBER_1, SIGNAL_HANDLER_2).size());
        Assert.assertEquals(1, trace.getEstablishSignalEvents(SIGNAL_NUMBER_2, SIGNAL_HANDLER_3).size());
    }

    private Trace createTrace(List<RawTrace> rawTraces) {
        return new Trace(
                mockTraceState, rawTraces,
                eventIdToTtid, ttidToThreadInfo, tidToEvents, tidToMemoryAccessBlocks, tidToThreadState,
                addrToState, tidToAddrToEvents, lockIdToLockRegions, clinitEvents, originalTidToTraceTid,
                signalNumberToSignalHandlerToEstablishSignalEvents);
    }
}
