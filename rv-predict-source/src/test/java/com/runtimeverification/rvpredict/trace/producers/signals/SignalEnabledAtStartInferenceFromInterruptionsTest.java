package com.runtimeverification.rvpredict.trace.producers.signals;

import com.google.common.collect.ImmutableMap;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerModule;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.testutils.InterruptedEventsUtils;
import com.runtimeverification.rvpredict.testutils.SignalMaskForEventsUtils;
import com.runtimeverification.rvpredict.testutils.ThreadInfosComponentUtils;
import com.runtimeverification.rvpredict.testutils.TtidSetDifferenceUtils;
import com.runtimeverification.rvpredict.trace.ThreadInfo;
import com.runtimeverification.rvpredict.trace.producers.base.OtidToSignalDepthToTtidAtWindowStart;
import com.runtimeverification.rvpredict.trace.producers.base.ThreadInfosComponent;
import com.runtimeverification.rvpredict.trace.producers.base.TtidSetDifference;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.OptionalInt;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsExactly;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmpty;
import static com.runtimeverification.rvpredict.testutils.OtidToSignalDepthToTtidAtWindowStartUtils.clearMockOtidToSignalDepthToTtidAtWindowStart;
import static com.runtimeverification.rvpredict.testutils.OtidToSignalDepthToTtidAtWindowStartUtils.fillMockOtidToSignalDepthToTtidAtWindowStart;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SignalEnabledAtStartInferenceFromInterruptionsTest {
    private static final int ONE_SIGNAL = 1;
    private static final long SIGNAL_NUMBER_1 = 2;
    private static final int TTID_1 = 101;
    private static final int TTID_2 = 102;
    private static final long EVENT_ID_1 = 201L;
    private static final long THREAD_1 = 301L;
    private static final long SIGNAL_HANDLER_1 = 401L;

    private static final ThreadInfo THREAD_INFO_1 = ThreadInfo.createThreadInfo(TTID_1, THREAD_1, OptionalInt.empty());
    private static final ThreadInfo SIGNAL_INFO_2 =
            ThreadInfo.createSignalInfo(TTID_2, THREAD_1, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ONE_SIGNAL);

    @Mock private InterruptedEvents mockInterruptedEvents;
    @Mock private SignalMaskForEvents mockSignalMaskForEvents;
    @Mock private TtidSetDifference mockUnfinishedTtidsAtWindowStart;
    @Mock private ThreadInfosComponent mockThreadInfosComponent;
    @Mock private OtidToSignalDepthToTtidAtWindowStart mockOtidToSignalDepthToTtidAtWindowStart;

    private final TestProducerModule module = new TestProducerModule();

    @Test
    public void noInferrenceForEmptyInputs() {
        ComputingProducerWrapper<SignalEnabledAtStartInferenceFromInterruptions> producer =
                createAndRegister(
                        module,
                        mockInterruptedEvents,
                        mockSignalMaskForEvents,
                        mockUnfinishedTtidsAtWindowStart,
                        mockThreadInfosComponent,
                        mockOtidToSignalDepthToTtidAtWindowStart);

        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        TtidSetDifferenceUtils.clearMockTtidSetDifference(mockUnfinishedTtidsAtWindowStart);
        ThreadInfosComponentUtils.clearMockThreadInfosComponent(mockThreadInfosComponent);
        clearMockOtidToSignalDepthToTtidAtWindowStart(mockOtidToSignalDepthToTtidAtWindowStart);

        module.reset();

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart().entrySet(), isEmpty());
    }

    @Test
    public void infersFromInterruptionIfSignalMaskBeforeEventIsUnknown() {
        ComputingProducerWrapper<SignalEnabledAtStartInferenceFromInterruptions> producer =
                createAndRegister(
                        module,
                        mockInterruptedEvents,
                        mockSignalMaskForEvents,
                        mockUnfinishedTtidsAtWindowStart,
                        mockThreadInfosComponent,
                        mockOtidToSignalDepthToTtidAtWindowStart);

        InterruptedEventsUtils.fillMockInterruptedEvents(
                mockInterruptedEvents,
                ImmutableMap.of(TTID_2, TTID_1),
                ImmutableMap.of(TTID_2, EVENT_ID_1),
                ImmutableMap.of(SIGNAL_NUMBER_1, ImmutableMap.of(TTID_1, EVENT_ID_1)));
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        when(mockSignalMaskForEvents.getSignalMaskBeforeEvent(TTID_1, EVENT_ID_1)).thenReturn(SignalMask.UNKNOWN_MASK);
        TtidSetDifferenceUtils.clearMockTtidSetDifference(mockUnfinishedTtidsAtWindowStart);
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent, THREAD_INFO_1, SIGNAL_INFO_2);
        clearMockOtidToSignalDepthToTtidAtWindowStart(mockOtidToSignalDepthToTtidAtWindowStart);

        module.reset();

        Assert.assertEquals(1, producer.getComputed().getSignalToTtidWhereEnabledAtStart().size());
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_1),
                containsExactly(TTID_1));
    }

    @Test
    public void doesNotInferFromInterruptionIfEnabledAtInterruptedEvent() {
        ComputingProducerWrapper<SignalEnabledAtStartInferenceFromInterruptions> producer =
                createAndRegister(
                        module,
                        mockInterruptedEvents,
                        mockSignalMaskForEvents,
                        mockUnfinishedTtidsAtWindowStart,
                        mockThreadInfosComponent,
                        mockOtidToSignalDepthToTtidAtWindowStart);

        InterruptedEventsUtils.fillMockInterruptedEvents(
                mockInterruptedEvents,
                ImmutableMap.of(TTID_2, TTID_1),
                ImmutableMap.of(TTID_2, EVENT_ID_1),
                ImmutableMap.of(SIGNAL_NUMBER_1, ImmutableMap.of(TTID_1, EVENT_ID_1)));
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        when(mockSignalMaskForEvents.getSignalMaskBeforeEvent(TTID_1, EVENT_ID_1))
                .thenReturn(SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_1));
        TtidSetDifferenceUtils.clearMockTtidSetDifference(mockUnfinishedTtidsAtWindowStart);
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent, THREAD_INFO_1, SIGNAL_INFO_2);
        clearMockOtidToSignalDepthToTtidAtWindowStart(mockOtidToSignalDepthToTtidAtWindowStart);

        module.reset();

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart().entrySet(), isEmpty());
    }

    @Test
    public void infersFromAlreadyRunningSignal() {
        ComputingProducerWrapper<SignalEnabledAtStartInferenceFromInterruptions> producer =
                createAndRegister(
                        module,
                        mockInterruptedEvents,
                        mockSignalMaskForEvents,
                        mockUnfinishedTtidsAtWindowStart,
                        mockThreadInfosComponent,
                        mockOtidToSignalDepthToTtidAtWindowStart);

        InterruptedEventsUtils.clearMockInterruptedEvents(mockInterruptedEvents);
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        when(mockSignalMaskForEvents.getSignalMaskBeforeEvent(TTID_1, EVENT_ID_1)).thenReturn(SignalMask.UNKNOWN_MASK);
        TtidSetDifferenceUtils.fillMockTtidSetDifference(mockUnfinishedTtidsAtWindowStart, TTID_1, TTID_2);
        ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent, THREAD_INFO_1, SIGNAL_INFO_2);
        fillMockOtidToSignalDepthToTtidAtWindowStart(
                mockOtidToSignalDepthToTtidAtWindowStart, THREAD_INFO_1, SIGNAL_INFO_2);

        module.reset();

        Assert.assertEquals(1, producer.getComputed().getSignalToTtidWhereEnabledAtStart().size());
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_1),
                containsExactly(TTID_1));
    }

    private static ComputingProducerWrapper<SignalEnabledAtStartInferenceFromInterruptions> createAndRegister(
            ProducerModule module,
            InterruptedEvents mockInterruptedEvents,
            SignalMaskForEvents mockSignalMaskForEvents,
            TtidSetDifference mockUnfinishedTtidsAtWindowStart,
            ThreadInfosComponent mockThreadInfosComponent,
            OtidToSignalDepthToTtidAtWindowStart mockOtidToSignalDepthToTtidAtWindowStart) {
        return new ComputingProducerWrapper<>(
                new SignalEnabledAtStartInferenceFromInterruptions(
                        new ComputingProducerWrapper<>(mockInterruptedEvents, module),
                        new ComputingProducerWrapper<>(mockSignalMaskForEvents, module),
                        new ComputingProducerWrapper<>(mockUnfinishedTtidsAtWindowStart, module),
                        new ComputingProducerWrapper<>(mockThreadInfosComponent, module),
                        new ComputingProducerWrapper<>(mockOtidToSignalDepthToTtidAtWindowStart, module)),
                module);
    }
}
