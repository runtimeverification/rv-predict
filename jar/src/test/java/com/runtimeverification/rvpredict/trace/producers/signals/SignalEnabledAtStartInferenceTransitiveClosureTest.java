package com.runtimeverification.rvpredict.trace.producers.signals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerModule;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.testutils.InterruptedEventsUtils;
import com.runtimeverification.rvpredict.testutils.SignalMaskForEventsUtils;
import com.runtimeverification.rvpredict.testutils.SignalMasksAtWindowStartUtils;
import com.runtimeverification.rvpredict.testutils.StartAndJoinEventsForWindowUtils;
import com.runtimeverification.rvpredict.testutils.ThreadInfosComponentUtils;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.ThreadInfo;
import com.runtimeverification.rvpredict.trace.producers.base.SortedTtidsWithParentFirst;
import com.runtimeverification.rvpredict.trace.producers.base.ThreadInfosComponent;
import com.runtimeverification.rvpredict.trace.producers.base.TtidToStartAndJoinEventsForWindow;
import com.runtimeverification.rvpredict.util.Constants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.OptionalInt;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsExactly;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasMapSize;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmptyMap;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SignalEnabledAtStartInferenceTransitiveClosureTest {
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final long SIGNAL_NUMBER_1 = 2L;
    private static final long SIGNAL_NUMBER_2 = 3L;
    private static final long SIGNAL_NUMBER_3 = 4L;
    private static final int TTID_1 = 101;
    private static final int TTID_2 = 102;
    private static final int TTID_3 = 103;
    private static final int TTID_4 = 104;
    private static final int TTID_5 = 105;
    private static final int TTID_6 = 106;
    private static final int TTID_7 = 107;
    private static final long THREAD_1 = 201L;
    private static final long THREAD_2 = 202L;
    private static final long THREAD_3 = 203L;
    private static final long THREAD_4 = 204L;
    private static final long EVENT_ID_1 = 301L;
    private static final long EVENT_ID_2 = 302L;
    private static final long EVENT_ID_3 = 303L;
    private static final long SIGNAL_HANDLER_1 = 401L;
    private static final long PC_BASE = 501L;
    private static final long BASE_ID = 601L;

    private static final ThreadInfo THREAD_INFO_1 = ThreadInfo.createThreadInfo(TTID_1, THREAD_1, OptionalInt.empty());
    private static final ThreadInfo THREAD_INFO_2 = ThreadInfo.createThreadInfo(TTID_2, THREAD_2, OptionalInt.empty());
    private static final ThreadInfo THREAD_INFO_3 = ThreadInfo.createThreadInfo(TTID_3, THREAD_3, OptionalInt.empty());
    private static final ThreadInfo SIGNAL_INFO_4 =
            ThreadInfo.createSignalInfo(TTID_4, THREAD_1, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ONE_SIGNAL);
    private static final ThreadInfo SIGNAL_INFO_5 =
            ThreadInfo.createSignalInfo(TTID_5, THREAD_2, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ONE_SIGNAL);
    private static final ThreadInfo SIGNAL_INFO_6 =
            ThreadInfo.createSignalInfo(TTID_6, THREAD_3, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ONE_SIGNAL);
    private static final ThreadInfo THREAD_INFO_7 =
            ThreadInfo.createThreadInfo(TTID_7, THREAD_4, OptionalInt.of(TTID_1));

    @Mock private SignalEnabledAtStartInferenceFromReads mockSignalEnabledAtStartInferenceFromReads;
    @Mock private SignalEnabledAtStartInferenceFromInterruptions mockSignalEnabledAtStartInferenceFromInterruptions;
    @Mock private SortedTtidsWithParentFirst mockSortedTtidsWithParentFirst;
    @Mock private ThreadInfosComponent mockThreadInfosComponent;
    @Mock private InterruptedEvents mockInterruptedEvents;
    @Mock private SignalMaskForEvents mockSignalMaskForEvents;
    @Mock private SignalMaskAtWindowStart<? extends ProducerState> mockSignalMaskAtWindowStart;
    @Mock private TtidToStartAndJoinEventsForWindow mockTtidToStartAndJoinEventsForWindow;

    @Mock private Context mockContext;

    private final TestProducerModule module = new TestProducerModule();
    private int nextIdDelta = 0;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
    }

    @Test
    public void nothingInferredForEmptyInput() {
        ComputingProducerWrapper<SignalEnabledAtStartInferenceTransitiveClosure> producer =
                initProducer(
                        module,
                        mockSignalEnabledAtStartInferenceFromReads,
                        mockSignalEnabledAtStartInferenceFromInterruptions,
                        mockSortedTtidsWithParentFirst,
                        mockThreadInfosComponent,
                        mockInterruptedEvents,
                        mockSignalMaskForEvents,
                        mockSignalMaskAtWindowStart,
                        mockTtidToStartAndJoinEventsForWindow);

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(Collections.emptyMap());
        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(Collections.emptyMap());
        when(mockSignalEnabledAtStartInferenceFromInterruptions.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(Collections.emptyMap());
        when(mockSortedTtidsWithParentFirst.getTtids()).thenReturn(Collections.emptyList());
        ThreadInfosComponentUtils.clearMockThreadInfosComponent(mockThreadInfosComponent);
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        SignalMasksAtWindowStartUtils.clearMockSignalMasksAtWindowStart(mockSignalMaskAtWindowStart);
        StartAndJoinEventsForWindowUtils.clearMockStartAndJoinEventsForWindow(mockTtidToStartAndJoinEventsForWindow);
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart(), isEmptyMap());
        Assert.assertThat(producer.getComputed().getSignalToTtidWhereDisabledAtStart(), isEmptyMap());
    }

    @Test
    public void copiesEnableAndDisableInferencesToOutput() {
        ComputingProducerWrapper<SignalEnabledAtStartInferenceTransitiveClosure> producer =
                initProducer(
                        module,
                        mockSignalEnabledAtStartInferenceFromReads,
                        mockSignalEnabledAtStartInferenceFromInterruptions,
                        mockSortedTtidsWithParentFirst,
                        mockThreadInfosComponent,
                        mockInterruptedEvents,
                        mockSignalMaskForEvents,
                        mockSignalMaskAtWindowStart,
                        mockTtidToStartAndJoinEventsForWindow);

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_1, ImmutableMap.of(TTID_1, Constants.INVALID_EVENT_ID)));

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_2, ImmutableMap.of(TTID_1, Constants.INVALID_EVENT_ID)));
        when(mockSignalEnabledAtStartInferenceFromInterruptions.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_1, ImmutableMap.of(TTID_2, Constants.INVALID_EVENT_ID)));

        when(mockSortedTtidsWithParentFirst.getTtids()).thenReturn(ImmutableList.of(TTID_1, TTID_2));
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent, THREAD_INFO_1, THREAD_INFO_2);
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK, TTID_2, SignalMask.UNKNOWN_MASK));
        StartAndJoinEventsForWindowUtils.clearMockStartAndJoinEventsForWindow(mockTtidToStartAndJoinEventsForWindow);
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart(), hasMapSize(2));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_1));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_1).keySet(),
                containsExactly(TTID_2));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_2));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_2).keySet(),
                containsExactly(TTID_1));

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereDisabledAtStart(), hasMapSize(1));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereDisabledAtStart().containsKey(SIGNAL_NUMBER_1));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereDisabledAtStart().get(SIGNAL_NUMBER_1).keySet(),
                containsExactly(TTID_1));
    }

    @Test
    public void infersEnablingButNotDisablingForInterruptedThread() {
        ComputingProducerWrapper<SignalEnabledAtStartInferenceTransitiveClosure> producer =
                initProducer(
                        module,
                        mockSignalEnabledAtStartInferenceFromReads,
                        mockSignalEnabledAtStartInferenceFromInterruptions,
                        mockSortedTtidsWithParentFirst,
                        mockThreadInfosComponent,
                        mockInterruptedEvents,
                        mockSignalMaskForEvents,
                        mockSignalMaskAtWindowStart,
                        mockTtidToStartAndJoinEventsForWindow);

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_1, ImmutableMap.of(TTID_4, Constants.INVALID_EVENT_ID)));

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_2, ImmutableMap.of(TTID_5, Constants.INVALID_EVENT_ID)));
        when(mockSignalEnabledAtStartInferenceFromInterruptions.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_3, ImmutableMap.of(TTID_6, Constants.INVALID_EVENT_ID)));

        when(mockSortedTtidsWithParentFirst.getTtids()).thenReturn(ImmutableList.of(
                TTID_1, TTID_2, TTID_3, TTID_4, TTID_5, TTID_6));
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent,
                THREAD_INFO_1, THREAD_INFO_2, THREAD_INFO_3,
                SIGNAL_INFO_4, SIGNAL_INFO_5, SIGNAL_INFO_6);
        InterruptedEventsUtils.fillMockInterruptedEvents(
                mockInterruptedEvents,
                ImmutableMap.of(TTID_4, TTID_1, TTID_5, TTID_2, TTID_6, TTID_3),
                ImmutableMap.of(TTID_4, EVENT_ID_1, TTID_5, EVENT_ID_2, TTID_6, EVENT_ID_3),
                ImmutableMap.of(
                        SIGNAL_NUMBER_1,
                        ImmutableMap.of(TTID_1, EVENT_ID_1, TTID_2, EVENT_ID_2, TTID_3, EVENT_ID_3)));
        SignalMaskForEventsUtils.fillMockSignalMaskForEvents(
                mockSignalMaskForEvents,
                ImmutableMap.of(
                        TTID_1, SignalMask.UNKNOWN_MASK,
                        TTID_2, SignalMask.UNKNOWN_MASK,
                        TTID_3, SignalMask.UNKNOWN_MASK),
                ImmutableMap.of(
                        TTID_1, ImmutableMap.of(EVENT_ID_1, SignalMask.UNKNOWN_MASK),
                        TTID_2, ImmutableMap.of(EVENT_ID_2, SignalMask.UNKNOWN_MASK),
                        TTID_3, ImmutableMap.of(EVENT_ID_3, SignalMask.UNKNOWN_MASK)));
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(
                        TTID_1, SignalMask.UNKNOWN_MASK,
                        TTID_2, SignalMask.UNKNOWN_MASK,
                        TTID_3, SignalMask.UNKNOWN_MASK));
        StartAndJoinEventsForWindowUtils.clearMockStartAndJoinEventsForWindow(mockTtidToStartAndJoinEventsForWindow);
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart(), hasMapSize(2));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_2));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_2).keySet(),
                containsExactly(TTID_2, TTID_5));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_3));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_3).keySet(),
                containsExactly(TTID_3, TTID_6));

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereDisabledAtStart(), hasMapSize(1));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereDisabledAtStart().containsKey(SIGNAL_NUMBER_1));
        // TTID_1 is not in the set because we can't infer disable bits from signal interruptions.
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereDisabledAtStart().get(SIGNAL_NUMBER_1).keySet(),
                containsExactly(TTID_4));
    }

    @Test
    public void doesNotInferEnablingIfAlreadyEnabled() {
        ComputingProducerWrapper<SignalEnabledAtStartInferenceTransitiveClosure> producer =
                initProducer(
                        module,
                        mockSignalEnabledAtStartInferenceFromReads,
                        mockSignalEnabledAtStartInferenceFromInterruptions,
                        mockSortedTtidsWithParentFirst,
                        mockThreadInfosComponent,
                        mockInterruptedEvents,
                        mockSignalMaskForEvents,
                        mockSignalMaskAtWindowStart,
                        mockTtidToStartAndJoinEventsForWindow);

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_1, ImmutableMap.of(TTID_4, Constants.INVALID_EVENT_ID)));

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_2, ImmutableMap.of(TTID_5, Constants.INVALID_EVENT_ID)));
        when(mockSignalEnabledAtStartInferenceFromInterruptions.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_3, ImmutableMap.of(TTID_6, Constants.INVALID_EVENT_ID)));

        when(mockSortedTtidsWithParentFirst.getTtids()).thenReturn(ImmutableList.of(
                TTID_1, TTID_2, TTID_3, TTID_4, TTID_5, TTID_6));
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent,
                THREAD_INFO_1, THREAD_INFO_2, THREAD_INFO_3,
                SIGNAL_INFO_4, SIGNAL_INFO_5, SIGNAL_INFO_6);
        InterruptedEventsUtils.fillMockInterruptedEvents(
                mockInterruptedEvents,
                ImmutableMap.of(TTID_4, TTID_1, TTID_5, TTID_2, TTID_6, TTID_3),
                ImmutableMap.of(TTID_4, EVENT_ID_1, TTID_5, EVENT_ID_2, TTID_6, EVENT_ID_3),
                ImmutableMap.of(
                        SIGNAL_NUMBER_1,
                        ImmutableMap.of(TTID_1, EVENT_ID_1, TTID_2, EVENT_ID_2, TTID_3, EVENT_ID_3)));
        SignalMaskForEventsUtils.fillMockSignalMaskForEvents(
                mockSignalMaskForEvents,
                ImmutableMap.of(
                        TTID_1, SignalMask.UNKNOWN_MASK.disable(SIGNAL_NUMBER_1, Constants.INVALID_EVENT_ID),
                        TTID_2, SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_2, Constants.INVALID_EVENT_ID),
                        TTID_3, SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_3, Constants.INVALID_EVENT_ID)),
                ImmutableMap.of(
                        TTID_1,
                        ImmutableMap.of(
                                EVENT_ID_1,
                                SignalMask.UNKNOWN_MASK.disable(SIGNAL_NUMBER_1, Constants.INVALID_EVENT_ID)),
                        TTID_2,
                        ImmutableMap.of(
                                EVENT_ID_2,
                                SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_2, Constants.INVALID_EVENT_ID)),
                        TTID_3,
                        ImmutableMap.of(
                                EVENT_ID_3,
                                SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_3, Constants.INVALID_EVENT_ID))));
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(
                        TTID_1, SignalMask.UNKNOWN_MASK,
                        TTID_2, SignalMask.UNKNOWN_MASK,
                        TTID_3, SignalMask.UNKNOWN_MASK));
        StartAndJoinEventsForWindowUtils.clearMockStartAndJoinEventsForWindow(mockTtidToStartAndJoinEventsForWindow);
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart(), hasMapSize(2));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_2));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_2).keySet(),
                containsExactly(TTID_5));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_3));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_3).keySet(),
                containsExactly(TTID_6));

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereDisabledAtStart(), hasMapSize(1));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereDisabledAtStart().containsKey(SIGNAL_NUMBER_1));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereDisabledAtStart().get(SIGNAL_NUMBER_1).keySet(),
                containsExactly(TTID_4));
    }

    @Test
    public void infersEnablingIfSignalMaskAtWindowStartIsUnknown() {
        ComputingProducerWrapper<SignalEnabledAtStartInferenceTransitiveClosure> producer =
                initProducer(
                        module,
                        mockSignalEnabledAtStartInferenceFromReads,
                        mockSignalEnabledAtStartInferenceFromInterruptions,
                        mockSortedTtidsWithParentFirst,
                        mockThreadInfosComponent,
                        mockInterruptedEvents,
                        mockSignalMaskForEvents,
                        mockSignalMaskAtWindowStart,
                        mockTtidToStartAndJoinEventsForWindow);

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_1, ImmutableMap.of(TTID_4, Constants.INVALID_EVENT_ID)));

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_2, ImmutableMap.of(TTID_5, Constants.INVALID_EVENT_ID)));
        when(mockSignalEnabledAtStartInferenceFromInterruptions.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_3, ImmutableMap.of(TTID_6, Constants.INVALID_EVENT_ID)));

        when(mockSortedTtidsWithParentFirst.getTtids()).thenReturn(ImmutableList.of(
                TTID_1, TTID_2, TTID_3, TTID_4, TTID_5, TTID_6));
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent,
                THREAD_INFO_1, THREAD_INFO_2, THREAD_INFO_3,
                SIGNAL_INFO_4, SIGNAL_INFO_5, SIGNAL_INFO_6);
        InterruptedEventsUtils.fillMockInterruptedEvents(
                mockInterruptedEvents,
                ImmutableMap.of(TTID_4, TTID_1, TTID_5, TTID_2, TTID_6, TTID_3),
                Collections.emptyMap(),
                Collections.emptyMap());
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(
                        TTID_1, SignalMask.UNKNOWN_MASK,
                        TTID_2, SignalMask.UNKNOWN_MASK,
                        TTID_3, SignalMask.UNKNOWN_MASK));
        StartAndJoinEventsForWindowUtils.clearMockStartAndJoinEventsForWindow(mockTtidToStartAndJoinEventsForWindow);
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart(), hasMapSize(2));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_2));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_2).keySet(),
                containsExactly(TTID_2, TTID_5));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_3));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_3).keySet(),
                containsExactly(TTID_3, TTID_6));

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereDisabledAtStart(), hasMapSize(1));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereDisabledAtStart().containsKey(SIGNAL_NUMBER_1));
        // TTID_1 is not in the set because we can't infer disable bits from signal interruptions.
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereDisabledAtStart().get(SIGNAL_NUMBER_1).keySet(),
                containsExactly(TTID_4));
    }

    @Test
    public void infersEnablingAndDisablingForNormalThreads() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalEnabledAtStartInferenceTransitiveClosure> producer =
                initProducer(
                        module,
                        mockSignalEnabledAtStartInferenceFromReads,
                        mockSignalEnabledAtStartInferenceFromInterruptions,
                        mockSortedTtidsWithParentFirst,
                        mockThreadInfosComponent,
                        mockInterruptedEvents,
                        mockSignalMaskForEvents,
                        mockSignalMaskAtWindowStart,
                        mockTtidToStartAndJoinEventsForWindow);

        tu.switchThread(THREAD_1, NO_SIGNAL);
        ReadonlyEventInterface start4 = TraceUtils.extractSingleEvent(tu.threadStart(THREAD_4));

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_1, ImmutableMap.of(TTID_7, Constants.INVALID_EVENT_ID)));

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_2, ImmutableMap.of(TTID_7, Constants.INVALID_EVENT_ID)));

        when(mockSortedTtidsWithParentFirst.getTtids()).thenReturn(ImmutableList.of(
                TTID_1, TTID_7));
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent,
                THREAD_INFO_1, THREAD_INFO_7);
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        SignalMaskForEventsUtils.fillMockSignalMaskForEvents(
                mockSignalMaskForEvents,
                ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK),
                ImmutableMap.of(TTID_1, ImmutableMap.of(start4.getEventId(), SignalMask.UNKNOWN_MASK)));
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(
                        TTID_1,
                        SignalMask.UNKNOWN_MASK,
                        TTID_7,
                        SignalMask.UNKNOWN_MASK
                                .disable(SIGNAL_NUMBER_2, Constants.INVALID_EVENT_ID)
                                .enable(SIGNAL_NUMBER_1, Constants.INVALID_EVENT_ID)));
        StartAndJoinEventsForWindowUtils.fillMockStartAndJoinEventsForWindow(
                mockTtidToStartAndJoinEventsForWindow,
                ImmutableMap.of(TTID_7, start4),
                Collections.emptyMap());
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart(), hasMapSize(1));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_2));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_2).keySet(),
                containsExactly(TTID_1, TTID_7));

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereDisabledAtStart(), hasMapSize(1));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereDisabledAtStart().containsKey(SIGNAL_NUMBER_1));
        // TTID_1 is not in the set because we can't infer disable bits from signal interruptions.
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereDisabledAtStart().get(SIGNAL_NUMBER_1).keySet(),
                containsExactly(TTID_1, TTID_7));
    }

    @Test
    public void resets() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalEnabledAtStartInferenceTransitiveClosure> producer =
                initProducer(
                        module,
                        mockSignalEnabledAtStartInferenceFromReads,
                        mockSignalEnabledAtStartInferenceFromInterruptions,
                        mockSortedTtidsWithParentFirst,
                        mockThreadInfosComponent,
                        mockInterruptedEvents,
                        mockSignalMaskForEvents,
                        mockSignalMaskAtWindowStart,
                        mockTtidToStartAndJoinEventsForWindow);

        tu.switchThread(THREAD_1, NO_SIGNAL);
        ReadonlyEventInterface start4 = TraceUtils.extractSingleEvent(tu.threadStart(THREAD_4));

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_1, ImmutableMap.of(TTID_7, Constants.INVALID_EVENT_ID)));

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_2, ImmutableMap.of(TTID_7, Constants.INVALID_EVENT_ID)));

        when(mockSortedTtidsWithParentFirst.getTtids()).thenReturn(ImmutableList.of(
                TTID_1, TTID_7));
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent,
                THREAD_INFO_1, THREAD_INFO_7);
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        SignalMaskForEventsUtils.fillMockSignalMaskForEvents(
                mockSignalMaskForEvents,
                ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK),
                ImmutableMap.of(TTID_1, ImmutableMap.of(start4.getEventId(), SignalMask.UNKNOWN_MASK)));
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(
                        TTID_1,
                        SignalMask.UNKNOWN_MASK,
                        TTID_7,
                        SignalMask.UNKNOWN_MASK
                                .disable(SIGNAL_NUMBER_2, Constants.INVALID_EVENT_ID)
                                .enable(SIGNAL_NUMBER_1, Constants.INVALID_EVENT_ID)));
        StartAndJoinEventsForWindowUtils.fillMockStartAndJoinEventsForWindow(
                mockTtidToStartAndJoinEventsForWindow,
                ImmutableMap.of(TTID_7, start4),
                Collections.emptyMap());
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart(), hasMapSize(1));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_2));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_2).keySet(),
                containsExactly(TTID_1, TTID_7));

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereDisabledAtStart(), hasMapSize(1));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereDisabledAtStart().containsKey(SIGNAL_NUMBER_1));
        // TTID_1 is not in the set because we can't infer disable bits from signal interruptions.
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereDisabledAtStart().get(SIGNAL_NUMBER_1).keySet(),
                containsExactly(TTID_1, TTID_7));

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_1, ImmutableMap.of(TTID_1, Constants.INVALID_EVENT_ID)));

        when(mockSignalEnabledAtStartInferenceFromReads.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_2, ImmutableMap.of(TTID_1, Constants.INVALID_EVENT_ID)));
        when(mockSignalEnabledAtStartInferenceFromInterruptions.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_1, ImmutableMap.of(TTID_2, Constants.INVALID_EVENT_ID)));

        when(mockSortedTtidsWithParentFirst.getTtids()).thenReturn(ImmutableList.of(TTID_1, TTID_2));
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent, THREAD_INFO_1, THREAD_INFO_2);
        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK, TTID_2, SignalMask.UNKNOWN_MASK));
        StartAndJoinEventsForWindowUtils.clearMockStartAndJoinEventsForWindow(mockTtidToStartAndJoinEventsForWindow);
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart(), hasMapSize(2));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_1));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_1).keySet(),
                containsExactly(TTID_2));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_2));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_2).keySet(),
                containsExactly(TTID_1));

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereDisabledAtStart(), hasMapSize(1));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereDisabledAtStart().containsKey(SIGNAL_NUMBER_1));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereDisabledAtStart().get(SIGNAL_NUMBER_1).keySet(),
                containsExactly(TTID_1));
    }

    private ComputingProducerWrapper<SignalEnabledAtStartInferenceTransitiveClosure> initProducer(
            ProducerModule module,
            SignalEnabledAtStartInferenceFromReads mockSignalEnabledAtStartInferenceFromReads,
            SignalEnabledAtStartInferenceFromInterruptions mockSignalEnabledAtStartInferenceFromInterruptions,
            SortedTtidsWithParentFirst mockSortedTtidsWithParentFirst,
            ThreadInfosComponent mockThreadInfosComponent,
            InterruptedEvents mockInterruptedEvents,
            SignalMaskForEvents mockSignalMaskForEvents,
            SignalMaskAtWindowStart<? extends ProducerState> mockSignalMaskAtWindowStart,
            TtidToStartAndJoinEventsForWindow mockTtidToStartAndJoinEventsForWindow) {
        return new ComputingProducerWrapper<>(
                new SignalEnabledAtStartInferenceTransitiveClosure(
                        new ComputingProducerWrapper<>(mockSignalEnabledAtStartInferenceFromReads, module),
                        new ComputingProducerWrapper<>(mockSignalEnabledAtStartInferenceFromInterruptions, module),
                        new ComputingProducerWrapper<>(mockSortedTtidsWithParentFirst, module),
                        new ComputingProducerWrapper<>(mockThreadInfosComponent, module),
                        new ComputingProducerWrapper<>(mockInterruptedEvents, module),
                        new ComputingProducerWrapper<>(mockSignalMaskForEvents, module),
                        new ComputingProducerWrapper<>(mockSignalMaskAtWindowStart, module),
                        new ComputingProducerWrapper<>(mockTtidToStartAndJoinEventsForWindow, module)),
                module);
    }
}
