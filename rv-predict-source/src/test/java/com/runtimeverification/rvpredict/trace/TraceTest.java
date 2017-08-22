package com.runtimeverification.rvpredict.trace;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.maps.MemoryAddrToStateMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TraceTest {
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final int TWO_SIGNALS = 2;
    private static final long SIGNAL_NUMBER_1 = 10;
    private static final long SIGNAL_NUMBER_2 = 11;
    private static final long SIGNAL_1_ENABLED = ~(1 << SIGNAL_NUMBER_1);
    private static final long ALL_SIGNALS_DISABLED = ~0;
    private static final long THREAD_ID_1 = 100;
    private static final long THREAD_ID_2 = 101;
    private static final long THREAD_ID_3 = 102;
    private static final long THREAD_ID_4 = 103;
    private static final long PC_BASE = 200;
    private static final long ADDRESS_1 = 300;
    private static final long ADDRESS_2 = 301;
    private static final long ADDRESS_3 = 302;
    private static final long VALUE_1 = 400;
    private static final long VALUE_2 = 401;
    private static final long SIGNAL_HANDLER_1 = 600;
    private static final long SIGNAL_HANDLER_2 = 601;
    private static final long SIGNAL_HANDLER_3 = 602;
    private static final long SIGNAL_MASK_1 = 700;
    private static final long SIGNAL_MASK_2 = 701;
    private static final long SIGNAL_MASK_3 = 702;
    private static final long SIGNAL_MASK_4 = 703;
    private static final long SIGNAL_MASK_5 = 704;
    private static final long GENERATION_1 = 800;
    private static final long BASE_ID = 900;
    private static final long CANONICAL_FRAME_ADDRESS_1 = 1000;
    private static final long CANONICAL_FRAME_ADDRESS_2 = 1001;
    private static final long CANONICAL_FRAME_ADDRESS_3 = 1002;
    private static final OptionalLong CALL_SITE_ADDRESS_1 = OptionalLong.of(1100);

    private static final ThreadInfo TTID_1_OTID_1_THREAD = ThreadInfo.createThreadInfo(
            1, THREAD_ID_1, OptionalInt.empty());
    private static final ThreadInfo TTID_2_OTID_2_THREAD = ThreadInfo.createThreadInfo(
            2, THREAD_ID_2, OptionalInt.empty());
    private static final ThreadInfo TTID_3_OTID_2_THREAD = ThreadInfo.createThreadInfo(
            3, THREAD_ID_2, OptionalInt.empty());
    private static final ThreadInfo TTID_3_OTID_3_THREAD = ThreadInfo.createThreadInfo(
            3, THREAD_ID_3, OptionalInt.empty());
    private static final ThreadInfo TTID_4_OTID_4_THREAD = ThreadInfo.createThreadInfo(
            4, THREAD_ID_4, OptionalInt.empty());

    private static final ThreadInfo TTID_2_OTID_2_SIGNAL_1_HANDLER_1 = ThreadInfo.createSignalInfo(
            2, THREAD_ID_2, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, 1);
    private static final ThreadInfo TTID_2_OTID_1_SIGNAL_1_HANDLER_1 = ThreadInfo.createSignalInfo(
            2, THREAD_ID_1, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, 1);
    private static final ThreadInfo TTID_3_OTID_1_SIGNAL_1_HANDLER_1 = ThreadInfo.createSignalInfo(
            3, THREAD_ID_1, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, 1);
    private static final ThreadInfo TTID_4_OTID_1_SIGNAL_2_HANDLER_1_DEPTH_2 = ThreadInfo.createSignalInfo(
            4, THREAD_ID_1, SIGNAL_NUMBER_2, SIGNAL_HANDLER_1, 2);

    @Mock
    private TraceState mockTraceState;
    @Mock
    private Context mockContext;
    @Mock
    private Configuration mockConfiguration;
    @Mock
    private ThreadInfos mockThreadInfos;

    private long nextIdDelta;
    private Map<Long, Integer> eventIdToTtid;
    private Map<Integer, List<ReadonlyEventInterface>> tidToEvents;
    private Map<Integer, List<MemoryAccessBlock>> tidToMemoryAccessBlocks;
    private Map<Integer, ThreadState> tidToThreadState;
    private MemoryAddrToStateMap addrToState;
    private Table<Integer, Long, List<ReadonlyEventInterface>> tidToAddrToEvents;
    private Map<Long, List<LockRegion>> lockIdToLockRegions;
    private Set<ReadonlyEventInterface> clinitEvents;
    private Map<Long, Map<Integer, Boolean>> signalIsEnabledForThreadCache;
    private Map<Long, Map<Long, Boolean>> atLeastOneSigsetAllowsSignalCache;
    private Map<Integer, Set<Integer>> ttidsThatCanOverlap;
    private Map<Long, Map<Long, List<ReadonlyEventInterface>>> signalNumberToSignalHandlerToEstablishSignalEvents;

    @Before
    public void setUp() {
        eventIdToTtid = new HashMap<>();
        tidToEvents = new HashMap<>();
        tidToMemoryAccessBlocks = new HashMap<>();
        tidToThreadState = new HashMap<>();
        addrToState = new MemoryAddrToStateMap(100);
        tidToAddrToEvents = HashBasedTable.create();
        lockIdToLockRegions = new HashMap<>();
        clinitEvents = new HashSet<>();
        signalIsEnabledForThreadCache = new HashMap<>();
        atLeastOneSigsetAllowsSignalCache = new HashMap<>();
        ttidsThatCanOverlap = new HashMap<>();

        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
        when(mockContext.createUniqueSignalHandlerId(SIGNAL_NUMBER_1)).thenReturn(1L);
        signalNumberToSignalHandlerToEstablishSignalEvents = new HashMap<>();

        when(mockContext.createUniqueDataAddressId(ADDRESS_1)).thenReturn(1L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_2)).thenReturn(2L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_3)).thenReturn(3L);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        when(mockTraceState.config()).thenReturn(mockConfiguration);
        when(mockTraceState.getThreadStateSnapshot(anyInt())).thenReturn(new ThreadState());
        when(mockTraceState.getThreadInfos()).thenReturn(mockThreadInfos);
        when(mockTraceState.getTtidForThreadOngoingAtWindowStart(anyLong(), anyInt()))
                .thenReturn(OptionalInt.empty());
    }

    @Test
    public void testSkipsRecurrentPatterns() throws InvalidTraceDataException {
        /*
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

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);

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

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);
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

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);
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

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);
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

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);
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
    public void computesThreadStartAndJoinEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.threadStart(THREAD_ID_2),
                        tu.threadStart(THREAD_ID_3),
                        tu.threadJoin(THREAD_ID_4),
                        tu.threadJoin(THREAD_ID_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_3, NO_SIGNAL),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_4, NO_SIGNAL),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1))
        );

        addThreadInfoToMocks(
                TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_3_THREAD, TTID_4_OTID_4_THREAD);
        Trace trace = createTrace(rawTraces);

        Assert.assertFalse(trace.getStartEventForTtid(1).isPresent());
        Assert.assertFalse(trace.getJoinEventForTtid(1).isPresent());
        Assert.assertTrue(trace.getStartEventForTtid(2).isPresent());
        Assert.assertTrue(trace.getJoinEventForTtid(2).isPresent());
        Assert.assertTrue(trace.getStartEventForTtid(3).isPresent());
        Assert.assertFalse(trace.getJoinEventForTtid(3).isPresent());
        Assert.assertFalse(trace.getStartEventForTtid(4).isPresent());
        Assert.assertTrue(trace.getJoinEventForTtid(4).isPresent());
    }

    @Test
    public void infersSignalEnablingFromInterruption() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_ID_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_1, ONE_SIGNAL));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
        Assert.assertFalse(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(2));
    }

    @Test
    public void doesNotInferSignalEnablingFromInterruptionIfManuallyEnabled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.enableSignal(SIGNAL_NUMBER_1),

                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_ID_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_1, ONE_SIGNAL));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertFalse(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
        Assert.assertFalse(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(2));
    }

    @Test
    public void infersSignalEnablingFromInterruptionIfManuallyEnabledAfterInterruption()
            throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, NO_SIGNAL),
                tu.enableSignal(SIGNAL_NUMBER_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_ID_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_1, ONE_SIGNAL));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
        Assert.assertFalse(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(2));
    }

    @Test
    public void infersSignalEnablingFromEmptyThreadInterruption() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.nonAtomicStore(ADDRESS_2, VALUE_1),

                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_ID_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_1, ONE_SIGNAL));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
        Assert.assertFalse(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(2));
    }

    @Test
    public void infersSignalEnablingFromInterruptionAfterLastThreadEvent() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_ID_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_1, ONE_SIGNAL));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
        Assert.assertFalse(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(2));
    }

    @Test
    public void infersSignalEnablingFromInterruptionBeforeTheFirstThreadEvent() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.switchThread(THREAD_ID_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_ID_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_1, ONE_SIGNAL));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
        Assert.assertFalse(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(2));
    }

    @Test
    public void infersSignalEnablingFromEmptySignalInterruption() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, TWO_SIGNALS),
                tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
        );

        tu.setNextThreadNumber(3);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_ID_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_1, TWO_SIGNALS));

        addThreadInfoToMocks(
                TTID_1_OTID_1_THREAD, TTID_2_OTID_1_SIGNAL_1_HANDLER_1, TTID_3_OTID_2_THREAD,
                TTID_4_OTID_1_SIGNAL_2_HANDLER_1_DEPTH_2);
        when(mockTraceState.getUnfinishedTtidsAtWindowStart()).thenReturn(
                ImmutableList.of(TTID_1_OTID_1_THREAD.getId(), TTID_2_OTID_1_SIGNAL_1_HANDLER_1.getId()));
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertThat(
                trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_2),
                containsExactly(TTID_2_OTID_1_SIGNAL_1_HANDLER_1.getId()));
    }

    @Test
    public void recursivelyPropagatesSignalEnablingInference()
            throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.threadStart(THREAD_ID_2),

                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_2, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_ID_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_2, ONE_SIGNAL));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
        Assert.assertTrue(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(2));
    }

    @Test
    public void usesTheFirstInterruptionForEnableInferenceIfSignalInterruptsMultipleTimes()
            throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<List<ReadonlyEventInterface>> threadEvents = new ArrayList<>();
        List<List<ReadonlyEventInterface>> firstSignalEvents = new ArrayList<>();
        List<List<ReadonlyEventInterface>> secondSignalEvents = new ArrayList<>();

        threadEvents.add(tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        firstSignalEvents.addAll(Arrays.asList(
                tu.switchThread(THREAD_ID_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                tu.exitSignal()));
        threadEvents.addAll(Arrays.asList(
                tu.switchThread(THREAD_ID_1, NO_SIGNAL),
                tu.enableSignal(SIGNAL_NUMBER_1)));
        secondSignalEvents.addAll(Arrays.asList(
                tu.switchThread(THREAD_ID_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                tu.exitSignal()));
        threadEvents.addAll(Arrays.asList(
                tu.switchThread(THREAD_ID_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        ));

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(threadEvents, THREAD_ID_1, NO_SIGNAL),
                tu.extractRawTrace(firstSignalEvents, THREAD_ID_1, ONE_SIGNAL),
                tu.extractRawTrace(secondSignalEvents, THREAD_ID_1, ONE_SIGNAL));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_1_SIGNAL_1_HANDLER_1, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
    }

    @Test
    public void infersSignalEnablingFromMaskRead() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.getSignalMask(SIGNAL_1_ENABLED)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)
                ));

        addThreadInfoToMocks(
                TTID_1_OTID_1_THREAD, TTID_2_OTID_2_SIGNAL_1_HANDLER_1, TTID_3_OTID_2_THREAD);

        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
    }

    @Test
    public void infersSignalEnablingFromMaskReadWrite() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.getSetSignalMask(SIGNAL_1_ENABLED, ALL_SIGNALS_DISABLED)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)
                ));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_SIGNAL_1_HANDLER_1, TTID_3_OTID_2_THREAD);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
    }

    @Test
    public void doesNotInferSignalEnablingFromDisabledMaskRead() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.getSignalMask(ALL_SIGNALS_DISABLED)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)
                ));


        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_SIGNAL_1_HANDLER_1, TTID_3_OTID_2_THREAD);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertFalse(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
    }

    @Test
    public void threadsWithoutOrderingCanOverlap() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)
                ));


        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.threadsCanOverlap(1, 2));
    }

    @Test
    public void threadsStartedAfterJoinCannotOverlap() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.threadJoin(THREAD_ID_2),
                        tu.threadStart(THREAD_ID_3),
                        tu.threadJoin(THREAD_ID_4)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_3, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_4, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_3_THREAD, TTID_4_OTID_4_THREAD);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.threadsCanOverlap(1, 2));
        Assert.assertTrue(trace.threadsCanOverlap(1, 3));
        Assert.assertTrue(trace.threadsCanOverlap(1, 4));
        Assert.assertFalse(trace.threadsCanOverlap(2, 3));
        Assert.assertTrue(trace.threadsCanOverlap(2, 4));
        Assert.assertTrue(trace.threadsCanOverlap(3, 4));
    }

    @Test
    public void signalOverlapsOnlyWithThreadsWhereEnabled()
            throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_4, NO_SIGNAL),
                tu.enableSignal(SIGNAL_NUMBER_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_ID_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_1, ONE_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_4, NO_SIGNAL));

        addThreadInfoToMocks(
                TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1,
                TTID_4_OTID_4_THREAD);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.threadsCanOverlap(1, 3));
        Assert.assertFalse(trace.threadsCanOverlap(2, 3));
        Assert.assertTrue(trace.threadsCanOverlap(4, 3));
    }

    @Test
    public void threadOverlappingRelationIsSymmetrical()
            throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID_4, NO_SIGNAL),
                tu.enableSignal(SIGNAL_NUMBER_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_ID_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_1, ONE_SIGNAL),
                tu.extractRawTrace(events, THREAD_ID_4, NO_SIGNAL));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1,
                TTID_4_OTID_4_THREAD);
        when(mockTraceState.updateLockLocToUserLoc(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Trace trace = createTrace(rawTraces);

        Assert.assertTrue(trace.threadsCanOverlap(1, 3));
        Assert.assertFalse(trace.threadsCanOverlap(2, 3));
        Assert.assertTrue(trace.threadsCanOverlap(4, 3));

        Assert.assertTrue(trace.threadsCanOverlap(3, 1));
        Assert.assertFalse(trace.threadsCanOverlap(3, 2));
        Assert.assertTrue(trace.threadsCanOverlap(3, 4));
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

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);
        Trace trace = createTrace(rawTraces);

        Assert.assertEquals(3, trace.getEstablishSignalEvents(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1).size());
        Assert.assertEquals(1, trace.getEstablishSignalEvents(SIGNAL_NUMBER_1, SIGNAL_HANDLER_2).size());
        Assert.assertEquals(1, trace.getEstablishSignalEvents(SIGNAL_NUMBER_2, SIGNAL_HANDLER_3).size());
    }

    @Test
    public void extractsStackTraceWithFunctionEntryEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> event1List;
        List<ReadonlyEventInterface> event2List;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setPc(PC_BASE),
                        tu.enterFunction(CANONICAL_FRAME_ADDRESS_1, CALL_SITE_ADDRESS_1),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enterFunction(CANONICAL_FRAME_ADDRESS_2, CALL_SITE_ADDRESS_1),
                        event1List = tu.nonAtomicLoad(ADDRESS_2, VALUE_1),
                        tu.enterFunction(CANONICAL_FRAME_ADDRESS_3, CALL_SITE_ADDRESS_1),
                        event2List = tu.nonAtomicLoad(ADDRESS_3, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                        tu.nonAtomicStore(ADDRESS_3, VALUE_1)
                ));

        when(mockConfiguration.stacks()).thenReturn(true);
        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);

        Trace trace = createTrace(rawTraces);

        ImmutableList<ReadonlyEventInterface> event1Stack =
                ImmutableList.copyOf(trace.getStacktraceAt(extractSingleEvent(event1List)));
        ImmutableList<ReadonlyEventInterface> event2Stack =
                ImmutableList.copyOf(trace.getStacktraceAt(extractSingleEvent(event2List)));

        Assert.assertEquals(3, event1Stack.size());
        Assert.assertEquals(EventType.READ, event1Stack.get(0).getType());
        Assert.assertEquals(ADDRESS_2, event1Stack.get(0).getDataObjectExternalIdentifier());
        Assert.assertEquals(EventType.INVOKE_METHOD, event1Stack.get(2).getType());
        Assert.assertEquals(CANONICAL_FRAME_ADDRESS_1, event1Stack.get(2).getCanonicalFrameAddress());
        Assert.assertEquals(EventType.INVOKE_METHOD, event1Stack.get(1).getType());
        Assert.assertEquals(CANONICAL_FRAME_ADDRESS_2, event1Stack.get(1).getCanonicalFrameAddress());


        Assert.assertEquals(4, event2Stack.size());
        Assert.assertEquals(EventType.READ, event2Stack.get(0).getType());
        Assert.assertEquals(ADDRESS_3, event2Stack.get(0).getDataObjectExternalIdentifier());
        Assert.assertEquals(EventType.INVOKE_METHOD, event2Stack.get(1).getType());
        Assert.assertEquals(CANONICAL_FRAME_ADDRESS_3, event2Stack.get(1).getCanonicalFrameAddress());
        Assert.assertEquals(EventType.INVOKE_METHOD, event2Stack.get(2).getType());
        Assert.assertEquals(CANONICAL_FRAME_ADDRESS_2, event2Stack.get(2).getCanonicalFrameAddress());
        Assert.assertEquals(EventType.INVOKE_METHOD, event2Stack.get(3).getType());
        Assert.assertEquals(CANONICAL_FRAME_ADDRESS_1, event2Stack.get(3).getCanonicalFrameAddress());
    }

    @Test
    public void signalsAreEnabledIfRunningAtWindowStart() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        tu.createRawTrace(
                true,
                tu.atomicStore(ADDRESS_1, VALUE_1)
        );
        tu.createRawTrace(
                true,
                tu.switchThread(THREAD_ID_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        false,
                        tu.setPc(PC_BASE),
                        tu.switchThread(THREAD_ID_1, NO_SIGNAL),
                        e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        false,
                        tu.switchThread(THREAD_ID_1, ONE_SIGNAL),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()
                ));

        when(mockConfiguration.stacks()).thenReturn(true);

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_1_SIGNAL_1_HANDLER_1);

        Trace trace = createTrace(rawTraces);

        Assert.assertEquals(1, trace.getTraceThreadId(extractSingleEvent(e1)));
        Assert.assertEquals(2, trace.getTraceThreadId(extractSingleEvent(e2)));

        Assert.assertTrue(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
    }

    @Test
    public void doesNotCrashWithoutSharedVariables() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1)));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);

        Trace trace = createTrace(rawTraces);

        Assert.assertEquals(2, trace.eventsByThreadID().size());
        Assert.assertTrue(trace.eventsByThreadID().containsKey(1));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(2));

        Assert.assertTrue(trace.eventsByThreadID().get(1).isEmpty());
        Assert.assertTrue(trace.eventsByThreadID().get(2).isEmpty());
    }

    @Test
    public void addsOngoingThreadsWithoutEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        addThreadInfoToMocks(TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_3_THREAD);

        when(mockTraceState.getThreadsForCurrentWindow()).thenReturn(ImmutableSet.of(
                TTID_1_OTID_1_THREAD.getId(),
                TTID_2_OTID_2_THREAD.getId(),
                TTID_3_OTID_3_THREAD.getId()));

        Trace trace = createTrace(rawTraces);

        Assert.assertEquals(3, trace.eventsByThreadID().size());
        Assert.assertTrue(trace.eventsByThreadID().containsKey(1));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(2));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(3));

        Assert.assertFalse(trace.eventsByThreadID().get(1).isEmpty());
        Assert.assertFalse(trace.eventsByThreadID().get(2).isEmpty());
        Assert.assertTrue(trace.eventsByThreadID().get(3).isEmpty());
        */
    }

    private void addThreadInfoToMocks(ThreadInfo... threadInfos) {
        TraceUtils.addThreadInfoToMocks(mockThreadInfos, mockTraceState, threadInfos);
    }

    private Trace createTrace(List<RawTrace> rawTraces) {
        return new Trace(
                mockTraceState, rawTraces,
                eventIdToTtid, tidToEvents, tidToMemoryAccessBlocks, tidToThreadState,
                addrToState, tidToAddrToEvents, lockIdToLockRegions, clinitEvents,
                ttidsThatCanOverlap,
                signalIsEnabledForThreadCache, atLeastOneSigsetAllowsSignalCache,
                signalNumberToSignalHandlerToEstablishSignalEvents);
    }
}
