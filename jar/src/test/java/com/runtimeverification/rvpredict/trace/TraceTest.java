package com.runtimeverification.rvpredict.trace;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static com.runtimeverification.rvpredict.testutils.TraceUtils.extractSingleEvent;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TraceTest {
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final long SIGNAL_NUMBER_1 = 10;
    private static final long SIGNAL_NUMBER_2 = 11;
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
    private static final ThreadInfo TTID_3_OTID_3_THREAD = ThreadInfo.createThreadInfo(
            3, THREAD_ID_3, OptionalInt.empty());
    private static final ThreadInfo TTID_4_OTID_4_THREAD = ThreadInfo.createThreadInfo(
            4, THREAD_ID_4, OptionalInt.empty());

    private static final ThreadInfo TTID_3_OTID_1_SIGNAL_1_HANDLER_1 = ThreadInfo.createSignalInfo(
            3, THREAD_ID_1, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, 1);

    @Mock private Context mockContext;
    @Mock private Configuration mockConfiguration;
    @Mock private MetadataInterface mockMetadata;

    private long nextIdDelta;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
        when(mockContext.createUniqueSignalHandlerId(SIGNAL_NUMBER_1)).thenReturn(1L);

        when(mockContext.createUniqueDataAddressId(ADDRESS_1)).thenReturn(1L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_2)).thenReturn(2L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_3)).thenReturn(3L);

        when(mockMetadata.getOriginalThreadCreationLocId(anyLong())).thenReturn(OptionalLong.empty());

        mockConfiguration.windowSize = 100;
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

        Trace trace = createTrace(rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);

        Assert.assertEquals(2, trace.eventsByThreadID().size());
        Assert.assertTrue(trace.eventsByThreadID().containsKey(1));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(2));

        List<ReadonlyEventInterface> events = trace.getEvents(1);
        Assert.assertEquals(2, events.size());

        events = trace.getEvents(2);
        Assert.assertEquals(1, events.size());
    }

    @Test
    public void testThreadStartsCannotBeRecurrentPatterns() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.threadStart(THREAD_ID_2),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.threadStart(THREAD_ID_3),
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.threadStart(THREAD_ID_4),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_3, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_4, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Trace trace = createTrace(
                rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_3_THREAD, TTID_4_OTID_4_THREAD);

        Assert.assertEquals(4, trace.eventsByThreadID().size());
        Assert.assertTrue(trace.eventsByThreadID().containsKey(1));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(2));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(3));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(4));

        List<ReadonlyEventInterface> events = trace.getEvents(1);
        Assert.assertEquals(7, events.size());
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

        Trace trace = createTrace(rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);

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

        Trace trace = createTrace(rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);

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

        Trace trace = createTrace(rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);

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

        Trace trace = createTrace(rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);

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

        Trace trace = createTrace(
                rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_3_THREAD, TTID_4_OTID_4_THREAD);

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

        Trace trace = createTrace(
                rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);

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

        Trace trace = createTrace(
                rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);

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

        Trace trace = createTrace(
                rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);

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

        Trace trace = createTrace(
                rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);

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

        Trace trace = createTrace(
                rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);

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

        Trace trace = createTrace(
                rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1);

        Assert.assertTrue(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(1));
        Assert.assertFalse(trace.getTtidsWhereSignalIsEnabledAtStart(SIGNAL_NUMBER_1).contains(2));
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


        Trace trace = createTrace(rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);

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

        Trace trace = createTrace(
                rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_3_THREAD, TTID_4_OTID_4_THREAD);

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

        Trace trace = createTrace(
                rawTraces,
                TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1,
                TTID_4_OTID_4_THREAD);

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

        Trace trace = createTrace(
                rawTraces,
                TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_1_SIGNAL_1_HANDLER_1,
                TTID_4_OTID_4_THREAD);

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

        Trace trace = createTrace(rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);

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

        Trace trace = createTrace(rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);

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
    public void doesNotCrashWithoutSharedVariables() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setPc(PC_BASE),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1)));

        Trace trace = createTrace(rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD);

        Assert.assertEquals(2, trace.eventsByThreadID().size());
        Assert.assertTrue(trace.eventsByThreadID().containsKey(1));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(2));

        Assert.assertTrue(trace.eventsByThreadID().get(1).isEmpty());
        Assert.assertTrue(trace.eventsByThreadID().get(2).isEmpty());
    }

    @Test
    public void addsOngoingThreadsWithoutEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        tu.setPc(PC_BASE);

        RawTrace first = tu.createRawTrace(
                tu.switchThread(THREAD_ID_3, NO_SIGNAL),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1));
        RawTrace second = tu.createRawTrace(
                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));

        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);

        // Thread 3 is running only in the previous window.
        createTrace(
                traceState,
                Collections.singletonList(tu.createRawTrace(
                    tu.switchThread(THREAD_ID_3, NO_SIGNAL),
                    tu.nonAtomicStore(ADDRESS_1, VALUE_1))),
                TTID_3_OTID_3_THREAD);

        List<RawTrace> rawTraces = Arrays.asList(first, second);

        Trace trace = createTrace(
                traceState, rawTraces, TTID_1_OTID_1_THREAD, TTID_2_OTID_2_THREAD, TTID_3_OTID_3_THREAD);

        Assert.assertEquals(3, trace.eventsByThreadID().size());
        Assert.assertTrue(trace.eventsByThreadID().containsKey(1));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(2));
        Assert.assertTrue(trace.eventsByThreadID().containsKey(3));

        Assert.assertFalse(trace.eventsByThreadID().get(1).isEmpty());
        Assert.assertFalse(trace.eventsByThreadID().get(2).isEmpty());
        Assert.assertTrue(trace.eventsByThreadID().get(3).isEmpty());
    }

    private Trace createTrace(List<RawTrace> rawTraces, ThreadInfo... threadInfos) {
        return createTrace(new TraceState(mockConfiguration, mockMetadata), rawTraces, threadInfos);
    }

    private Trace createTrace(TraceState traceState, List<RawTrace> rawTraces, ThreadInfo... threadInfos) {
        for (ThreadInfo threadInfo : threadInfos) {
            traceState.getThreadInfos().registerThreadInfo(threadInfo);
        }
        traceState.preStartWindow();
        return traceState.initNextTraceWindow(rawTraces);
    }
}
