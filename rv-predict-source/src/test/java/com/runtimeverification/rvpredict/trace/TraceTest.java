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
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

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

        when(mockContext.createUniqueDataAddressId(ADDRESS_1)).thenReturn(1L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_2)).thenReturn(2L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_3)).thenReturn(3L);
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

    private Trace createTrace(List<RawTrace> rawTraces) {
        return new Trace(
                mockTraceState, rawTraces,
                eventIdToTtid, ttidToThreadInfo, tidToEvents, tidToMemoryAccessBlocks, tidToThreadState,
                addrToState, tidToAddrToEvents, lockIdToLockRegions, clinitEvents, originalTidToTraceTid);
    }
}
