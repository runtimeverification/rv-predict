package com.runtimeverification.rvpredict.trace.producers.signals;

import com.google.common.collect.ImmutableMap;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.testutils.InterruptedEventsUtils;
import com.runtimeverification.rvpredict.testutils.SignalMasksAtWindowStartUtils;
import com.runtimeverification.rvpredict.testutils.ThreadInfosComponentUtils;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.ThreadInfo;
import com.runtimeverification.rvpredict.trace.producers.base.OtidToMainTtid;
import com.runtimeverification.rvpredict.trace.producers.base.RawTracesByTtid;
import com.runtimeverification.rvpredict.trace.producers.base.SortedTtidsWithParentFirst;
import com.runtimeverification.rvpredict.trace.producers.base.ThreadInfosComponent;
import com.runtimeverification.rvpredict.util.Constants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.assertException;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasMapSize;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmptyMap;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SignalMaskForEventsTest {
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final long SIGNAL_NUMBER_1 = 2L;
    private static final long SIGNAL_NUMBER_2 = 3L;
    private static final long SIGNAL_NUMBER_3 = 4L;
    private static final long THREAD_1 = 301L;
    private static final long THREAD_2 = 302L;
    private static final long PC_BASE = 401L;
    private static final long BASE_ID = 501L;
    private static final int TTID_1 = 601;
    private static final long EVENT_ID_1 = 701L;
    private static final long ADDRESS_1 = 801L;
    private static final long VALUE_1 = 901L;
    private static final long SIGNAL_HANDLER_1 = 1001L;
    private static final long GENERATION_1 = 1101L;

    private static final SignalMask SIGNAL_MASK_1_ENABLED_2_DISABLED =
            SignalMask.UNKNOWN_MASK
                    .enable(SIGNAL_NUMBER_1, Constants.INVALID_EVENT_ID)
                    .disable(SIGNAL_NUMBER_2, Constants.INVALID_EVENT_ID);
    private static final SignalMask SIGNAL_MASK_2_ENABLED_1_DISABLED =
            SignalMask.UNKNOWN_MASK
                    .enable(SIGNAL_NUMBER_2, Constants.INVALID_EVENT_ID)
                    .disable(SIGNAL_NUMBER_1, Constants.INVALID_EVENT_ID);
    private static final SignalMask SIGNAL_MASK_1_DISABLED =
            SignalMask.UNKNOWN_MASK.disable(SIGNAL_NUMBER_1, Constants.INVALID_EVENT_ID);

    @Mock private RawTracesByTtid mockRawTracesByTtid;
    @Mock private SortedTtidsWithParentFirst mockSortedTtidsWithParentFirst;
    @Mock private SignalMaskAtWindowStart<? extends ProducerState> mockSignalMaskAtWindowStart;
    @Mock private OtidToMainTtid mockOtidToMainTtid;
    @Mock private InterruptedEvents mockInterruptedEvents;
    @Mock private ThreadInfosComponent mockThreadInfosComponent;

    @Mock private Context mockContext;

    private final TestProducerModule module = new TestProducerModule();
    private int nextIdDelta = 0;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
    }

    @Test
    public void emptyOutputForEmptyInput() {
        ComputingProducerWrapper<SignalMaskForEvents> producer = initProducer(
                module,
                mockRawTracesByTtid,
                mockSortedTtidsWithParentFirst,
                mockSignalMaskAtWindowStart,
                mockOtidToMainTtid,
                mockInterruptedEvents,
                mockThreadInfosComponent);

        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockSortedTtidsWithParentFirst.getTtids()).thenReturn(Collections.emptyList());
        SignalMasksAtWindowStartUtils.clearMockSignalMasksAtWindowStart(mockSignalMaskAtWindowStart);
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        ThreadInfosComponentUtils.clearMockThreadInfosComponent(mockThreadInfosComponent);
        module.reset();

        assertException(
                IllegalArgumentException.class,
                () -> producer.getComputed().getSignalMaskBeforeEvent(TTID_1, EVENT_ID_1));
        Assert.assertThat(producer.getComputed().extractTtidToLastEventMap(), isEmptyMap());
    }

    @Test
    public void preservesMaskThroughoutTheThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalMaskForEvents> producer = initProducer(
                module,
                mockRawTracesByTtid,
                mockSortedTtidsWithParentFirst,
                mockSignalMaskAtWindowStart,
                mockOtidToMainTtid,
                mockInterruptedEvents,
                mockThreadInfosComponent);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        RawTrace rawTrace = tu.createRawTrace(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                e2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                e3 = tu.nonAtomicStore(ADDRESS_1, VALUE_1));

        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockRawTracesByTtid.getRawTrace(rawTrace.getThreadInfo().getId())).thenReturn(Optional.of(rawTrace));
        when(mockSortedTtidsWithParentFirst.getTtids())
                .thenReturn(Collections.singletonList(rawTrace.getThreadInfo().getId()));
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                Collections.singletonMap(rawTrace.getThreadInfo().getId(), SIGNAL_MASK_1_ENABLED_2_DISABLED));
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockOtidToMainTtid.getTtid(THREAD_1)).thenReturn(OptionalInt.of(rawTrace.getThreadInfo().getId()));
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromTraces(mockThreadInfosComponent, rawTrace);
        module.reset();

        assertException(
                IllegalArgumentException.class,
                () -> producer.getComputed().getSignalMaskBeforeEvent(TTID_1, EVENT_ID_1));
        Map<Integer, SignalMask> lastEventMap = producer.getComputed().extractTtidToLastEventMap();
        Assert.assertThat(lastEventMap, hasMapSize(1));
        Assert.assertTrue(lastEventMap.containsKey(rawTrace.getThreadInfo().getId()));
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, lastEventMap.get(rawTrace.getThreadInfo().getId()));

        SignalMask signalMask;
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e1).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e2).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e3).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, signalMask);
    }

    @Test
    public void doesNotCrashForThreadWithoutEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalMaskForEvents> producer = initProducer(
                module,
                mockRawTracesByTtid,
                mockSortedTtidsWithParentFirst,
                mockSignalMaskAtWindowStart,
                mockOtidToMainTtid,
                mockInterruptedEvents,
                mockThreadInfosComponent);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        RawTrace rawTrace = tu.createRawTrace(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                e2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                e3 = tu.nonAtomicStore(ADDRESS_1, VALUE_1));

        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockRawTracesByTtid.getRawTrace(rawTrace.getThreadInfo().getId())).thenReturn(Optional.of(rawTrace));
        when(mockSortedTtidsWithParentFirst.getTtids())
                .thenReturn(Arrays.asList(
                        TTID_1, rawTrace.getThreadInfo().getId()));
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(
                        rawTrace.getThreadInfo().getId(), SIGNAL_MASK_1_ENABLED_2_DISABLED,
                        TTID_1, SIGNAL_MASK_2_ENABLED_1_DISABLED));
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockOtidToMainTtid.getTtid(THREAD_1)).thenReturn(OptionalInt.of(rawTrace.getThreadInfo().getId()));
        when(mockOtidToMainTtid.getTtid(THREAD_2)).thenReturn(OptionalInt.of(TTID_1));
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent,
                rawTrace.getThreadInfo(),
                ThreadInfo.createThreadInfo(TTID_1, THREAD_2, OptionalInt.empty()));
        module.reset();

        Map<Integer, SignalMask> lastEventMap = producer.getComputed().extractTtidToLastEventMap();
        Assert.assertThat(lastEventMap, hasMapSize(2));
        Assert.assertTrue(lastEventMap.containsKey(rawTrace.getThreadInfo().getId()));
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, lastEventMap.get(rawTrace.getThreadInfo().getId()));
        Assert.assertTrue(lastEventMap.containsKey(TTID_1));
        Assert.assertEquals(SIGNAL_MASK_2_ENABLED_1_DISABLED, lastEventMap.get(TTID_1));

        SignalMask signalMask;
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e1).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e2).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e3).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, signalMask);
    }

    @Test
    public void startMaskIsUnknownWhenNotExplicitlyFilled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalMaskForEvents> producer = initProducer(
                module,
                mockRawTracesByTtid,
                mockSortedTtidsWithParentFirst,
                mockSignalMaskAtWindowStart,
                mockOtidToMainTtid,
                mockInterruptedEvents,
                mockThreadInfosComponent);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        RawTrace rawTrace = tu.createRawTrace(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                e2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                e3 = tu.nonAtomicStore(ADDRESS_1, VALUE_1));

        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockRawTracesByTtid.getRawTrace(rawTrace.getThreadInfo().getId())).thenReturn(Optional.of(rawTrace));
        when(mockSortedTtidsWithParentFirst.getTtids())
                .thenReturn(Collections.singletonList(rawTrace.getThreadInfo().getId()));
        SignalMasksAtWindowStartUtils.clearMockSignalMasksAtWindowStart(mockSignalMaskAtWindowStart);
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockOtidToMainTtid.getTtid(THREAD_1)).thenReturn(OptionalInt.of(rawTrace.getThreadInfo().getId()));
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromTraces(mockThreadInfosComponent, rawTrace);
        module.reset();

        assertException(
                IllegalArgumentException.class,
                () -> producer.getComputed().getSignalMaskBeforeEvent(TTID_1, EVENT_ID_1));
        Map<Integer, SignalMask> lastEventMap = producer.getComputed().extractTtidToLastEventMap();
        Assert.assertThat(lastEventMap, hasMapSize(1));
        Assert.assertTrue(lastEventMap.containsKey(rawTrace.getThreadInfo().getId()));
        Assert.assertEquals(SignalMask.UNKNOWN_MASK, lastEventMap.get(rawTrace.getThreadInfo().getId()));

        SignalMask signalMask;
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e1).getEventId());
        Assert.assertEquals(SignalMask.UNKNOWN_MASK, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e2).getEventId());
        Assert.assertEquals(SignalMask.UNKNOWN_MASK, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e3).getEventId());
        Assert.assertEquals(SignalMask.UNKNOWN_MASK, signalMask);
    }

    @Test
    public void threadMaskChanges() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalMaskForEvents> producer = initProducer(
                module,
                mockRawTracesByTtid,
                mockSortedTtidsWithParentFirst,
                mockSignalMaskAtWindowStart,
                mockOtidToMainTtid,
                mockInterruptedEvents,
                mockThreadInfosComponent);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        RawTrace rawTrace = tu.createRawTrace(
                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                e1 = tu.enableSignal(SIGNAL_NUMBER_2),
                tu.disableSignal(SIGNAL_NUMBER_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1));

        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockRawTracesByTtid.getRawTrace(rawTrace.getThreadInfo().getId())).thenReturn(Optional.of(rawTrace));
        when(mockSortedTtidsWithParentFirst.getTtids())
                .thenReturn(Collections.singletonList(rawTrace.getThreadInfo().getId()));
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                Collections.singletonMap(rawTrace.getThreadInfo().getId(), SIGNAL_MASK_1_ENABLED_2_DISABLED));
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockOtidToMainTtid.getTtid(THREAD_1)).thenReturn(OptionalInt.of(rawTrace.getThreadInfo().getId()));
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromTraces(
                mockThreadInfosComponent, rawTrace);
        module.reset();

        Map<Integer, SignalMask> lastEventMap = producer.getComputed().extractTtidToLastEventMap();
        Assert.assertThat(lastEventMap, hasMapSize(1));
        Assert.assertTrue(lastEventMap.containsKey(rawTrace.getThreadInfo().getId()));
        Assert.assertEquals(SIGNAL_MASK_2_ENABLED_1_DISABLED, lastEventMap.get(rawTrace.getThreadInfo().getId()));

        SignalMask signalMask;
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e1).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e2).getEventId());
        Assert.assertEquals(SIGNAL_MASK_2_ENABLED_1_DISABLED, signalMask);
    }

    @Test
    public void threadMaskInheritedFromParentWhenNotExplicitlyFilled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalMaskForEvents> producer = initProducer(
                module,
                mockRawTracesByTtid,
                mockSortedTtidsWithParentFirst,
                mockSignalMaskAtWindowStart,
                mockOtidToMainTtid,
                mockInterruptedEvents,
                mockThreadInfosComponent);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        RawTrace rawTrace1 = tu.createRawTrace(
                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                e1 = tu.enableSignal(SIGNAL_NUMBER_2),
                tu.disableSignal(SIGNAL_NUMBER_1),
                e2 = tu.threadStart(THREAD_2),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        RawTrace rawTrace2 = tu.createRawTrace(
                tu.switchThread(THREAD_2, NO_SIGNAL),
                e3 = tu.nonAtomicStore(ADDRESS_1, VALUE_1));

        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockRawTracesByTtid.getRawTrace(rawTrace1.getThreadInfo().getId())).thenReturn(Optional.of(rawTrace1));
        when(mockRawTracesByTtid.getRawTrace(rawTrace2.getThreadInfo().getId())).thenReturn(Optional.of(rawTrace2));
        when(mockSortedTtidsWithParentFirst.getTtids())
                .thenReturn(Arrays.asList(
                        rawTrace1.getThreadInfo().getId(),
                        rawTrace2.getThreadInfo().getId()));
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                Collections.singletonMap(rawTrace1.getThreadInfo().getId(), SIGNAL_MASK_1_ENABLED_2_DISABLED));
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockOtidToMainTtid.getTtid(THREAD_1)).thenReturn(OptionalInt.of(rawTrace1.getThreadInfo().getId()));
        when(mockOtidToMainTtid.getTtid(THREAD_2)).thenReturn(OptionalInt.of(rawTrace2.getThreadInfo().getId()));
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromTraces(
                mockThreadInfosComponent, rawTrace1, rawTrace2);
        module.reset();

        Map<Integer, SignalMask> lastEventMap = producer.getComputed().extractTtidToLastEventMap();
        Assert.assertThat(lastEventMap, hasMapSize(2));
        Assert.assertTrue(lastEventMap.containsKey(rawTrace1.getThreadInfo().getId()));
        Assert.assertEquals(SIGNAL_MASK_2_ENABLED_1_DISABLED, lastEventMap.get(rawTrace1.getThreadInfo().getId()));
        Assert.assertTrue(lastEventMap.containsKey(rawTrace2.getThreadInfo().getId()));
        Assert.assertEquals(SIGNAL_MASK_2_ENABLED_1_DISABLED, lastEventMap.get(rawTrace2.getThreadInfo().getId()));

        SignalMask signalMask;
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace1.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e1).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace1.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e2).getEventId());
        Assert.assertEquals(SIGNAL_MASK_2_ENABLED_1_DISABLED, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace2.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e3).getEventId());
        Assert.assertEquals(SIGNAL_MASK_2_ENABLED_1_DISABLED, signalMask);
    }

    @Test
    public void threadMaskInheritedFromInterruptedThreadWhenNotExplicitlyFilled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalMaskForEvents> producer = initProducer(
                module,
                mockRawTracesByTtid,
                mockSortedTtidsWithParentFirst,
                mockSignalMaskAtWindowStart,
                mockOtidToMainTtid,
                mockInterruptedEvents,
                mockThreadInfosComponent);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                e1 = tu.enableSignal(SIGNAL_NUMBER_2),
                tu.disableSignal(SIGNAL_NUMBER_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_3, SIGNAL_HANDLER_1, GENERATION_1),
                e3 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1));

        RawTrace rawTrace1 = tu.extractRawTrace(events, THREAD_1, NO_SIGNAL);
        RawTrace rawTrace2 = tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL);

        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockRawTracesByTtid.getRawTrace(rawTrace1.getThreadInfo().getId())).thenReturn(Optional.of(rawTrace1));
        when(mockRawTracesByTtid.getRawTrace(rawTrace2.getThreadInfo().getId())).thenReturn(Optional.of(rawTrace2));
        when(mockSortedTtidsWithParentFirst.getTtids())
                .thenReturn(Arrays.asList(
                        rawTrace1.getThreadInfo().getId(),
                        rawTrace2.getThreadInfo().getId()));
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                Collections.singletonMap(rawTrace1.getThreadInfo().getId(), SIGNAL_MASK_1_ENABLED_2_DISABLED));
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockOtidToMainTtid.getTtid(THREAD_1)).thenReturn(OptionalInt.of(rawTrace1.getThreadInfo().getId()));
        when(mockOtidToMainTtid.getTtid(THREAD_2)).thenReturn(OptionalInt.of(rawTrace2.getThreadInfo().getId()));
        InterruptedEventsUtils.fillMockInterruptedEvents(mockInterruptedEvents,
                Collections.singletonMap(rawTrace2.getThreadInfo().getId(), rawTrace1.getThreadInfo().getId()),
                Collections.singletonMap(
                        rawTrace2.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e2).getEventId()),
                Collections.singletonMap(
                        SIGNAL_NUMBER_3,
                        Collections.singletonMap(
                                rawTrace1.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e2).getEventId())));
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromTraces(
                mockThreadInfosComponent, rawTrace1, rawTrace2);
        module.reset();

        Map<Integer, SignalMask> lastEventMap = producer.getComputed().extractTtidToLastEventMap();
        Assert.assertThat(lastEventMap, hasMapSize(2));
        Assert.assertTrue(lastEventMap.containsKey(rawTrace1.getThreadInfo().getId()));
        Assert.assertEquals(SIGNAL_MASK_2_ENABLED_1_DISABLED, lastEventMap.get(rawTrace1.getThreadInfo().getId()));
        Assert.assertTrue(lastEventMap.containsKey(rawTrace2.getThreadInfo().getId()));
        Assert.assertEquals(SIGNAL_MASK_1_DISABLED, lastEventMap.get(rawTrace2.getThreadInfo().getId()));

        SignalMask signalMask;
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace1.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e1).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace1.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e2).getEventId());
        Assert.assertEquals(SIGNAL_MASK_2_ENABLED_1_DISABLED, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace2.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e3).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_DISABLED, signalMask);
    }

    @Test
    public void resets() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalMaskForEvents> producer = initProducer(
                module,
                mockRawTracesByTtid,
                mockSortedTtidsWithParentFirst,
                mockSignalMaskAtWindowStart,
                mockOtidToMainTtid,
                mockInterruptedEvents,
                mockThreadInfosComponent);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        RawTrace rawTrace = tu.createRawTrace(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                e2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                e3 = tu.nonAtomicStore(ADDRESS_1, VALUE_1));

        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockRawTracesByTtid.getRawTrace(rawTrace.getThreadInfo().getId())).thenReturn(Optional.of(rawTrace));
        when(mockSortedTtidsWithParentFirst.getTtids())
                .thenReturn(Collections.singletonList(rawTrace.getThreadInfo().getId()));
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                Collections.singletonMap(rawTrace.getThreadInfo().getId(), SIGNAL_MASK_1_ENABLED_2_DISABLED));
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockOtidToMainTtid.getTtid(THREAD_1)).thenReturn(OptionalInt.of(rawTrace.getThreadInfo().getId()));
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromTraces(mockThreadInfosComponent, rawTrace);
        module.reset();

        assertException(
                IllegalArgumentException.class,
                () -> producer.getComputed().getSignalMaskBeforeEvent(TTID_1, EVENT_ID_1));
        Map<Integer, SignalMask> lastEventMap = producer.getComputed().extractTtidToLastEventMap();
        Assert.assertThat(lastEventMap, hasMapSize(1));
        Assert.assertTrue(lastEventMap.containsKey(rawTrace.getThreadInfo().getId()));
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, lastEventMap.get(rawTrace.getThreadInfo().getId()));

        SignalMask signalMask;
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e1).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e2).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, signalMask);
        signalMask = producer.getComputed().getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e3).getEventId());
        Assert.assertEquals(SIGNAL_MASK_1_ENABLED_2_DISABLED, signalMask);


        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockSortedTtidsWithParentFirst.getTtids()).thenReturn(Collections.emptyList());
        SignalMasksAtWindowStartUtils.clearMockSignalMasksAtWindowStart(mockSignalMaskAtWindowStart);
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        ThreadInfosComponentUtils.clearMockThreadInfosComponent(mockThreadInfosComponent);
        module.reset();

        assertException(
                IllegalArgumentException.class,
                () -> producer.getComputed().getSignalMaskBeforeEvent(
                        rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e1).getEventId()));
        Assert.assertThat(producer.getComputed().extractTtidToLastEventMap(), isEmptyMap());
    }

    private ComputingProducerWrapper<SignalMaskForEvents> initProducer(
            TestProducerModule module,
            RawTracesByTtid rawTracesByTtid,
            SortedTtidsWithParentFirst sortedTtidsWithParentFirst,
            SignalMaskAtWindowStart<? extends ProducerState> signalMaskAtWindowStart,
            OtidToMainTtid otidToMainTtid,
            InterruptedEvents interruptedEvents,
            ThreadInfosComponent threadInfosComponent) {
        return new ComputingProducerWrapper<>(
                new SignalMaskForEvents(
                        new ComputingProducerWrapper<>(rawTracesByTtid, module),
                        new ComputingProducerWrapper<>(sortedTtidsWithParentFirst, module),
                        new ComputingProducerWrapper<>(signalMaskAtWindowStart, module),
                        new ComputingProducerWrapper<>(otidToMainTtid, module),
                        new ComputingProducerWrapper<>(interruptedEvents, module),
                        new ComputingProducerWrapper<>(threadInfosComponent, module)),
                module);
    }
}
