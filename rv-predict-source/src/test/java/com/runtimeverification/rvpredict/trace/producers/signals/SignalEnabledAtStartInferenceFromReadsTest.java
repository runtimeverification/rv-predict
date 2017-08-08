package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerModule;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.testutils.SignalMaskForEventsUtils;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.producers.base.RawTraces;
import com.runtimeverification.rvpredict.util.Constants;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsExactly;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasMapSize;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmptyMap;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SignalEnabledAtStartInferenceFromReadsTest {
    private static final int NO_SIGNAL = 0;
    private static final long SIGNAL_NUMBER_1 = 2;
    private static final long ALL_SIGNALS_DISABLED_MASK = ~0L;
    private static final long ALL_SIGNALS_ENABLED_MASK = 0L;

    private static final long THREAD_1 = 101L;
    private static final long PC_BASE = 201L;

    @Mock private Context mockContext;
    @Mock private RawTraces mockRawTraces;
    @Mock private SignalMaskForEvents mockSignalMaskForEvents;

    private final TestProducerModule module = new TestProducerModule();

    @Test
    public void something() {
        ComputingProducerWrapper<SignalEnabledAtStartInferenceFromReads> producer =
                initProducer(module, mockRawTraces, mockSignalMaskForEvents);
        when(mockRawTraces.getTraces()).thenReturn(Collections.emptyList());
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        Assert.assertThat(producer.getComputed().getSignalToTtidWhereDisabledAtStart(), isEmptyMap());
        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart(), isEmptyMap());
    }

    @Test
    public void infersFromDisabledRead() {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalEnabledAtStartInferenceFromReads> producer =
                initProducer(module, mockRawTraces, mockSignalMaskForEvents);

        List<ReadonlyEventInterface> e1;
        RawTrace rawTrace = tu.createRawTrace(e1 = tu.getSignalMask(ALL_SIGNALS_DISABLED_MASK));
        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(rawTrace));
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        when(mockSignalMaskForEvents.getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e1).getEventId()))
                .thenReturn(SignalMask.UNKNOWN_MASK);

        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereDisabledAtStart(),
                hasMapSize(Math.toIntExact(Constants.SIGNAL_NUMBER_COUNT)));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereDisabledAtStart().containsKey(SIGNAL_NUMBER_1));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereDisabledAtStart().get(SIGNAL_NUMBER_1),
                containsExactly(rawTrace.getThreadInfo().getId()));
        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart(), isEmptyMap());
    }

    @Test
    public void infersFromEnabledRead() {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalEnabledAtStartInferenceFromReads> producer =
                initProducer(module, mockRawTraces, mockSignalMaskForEvents);

        List<ReadonlyEventInterface> e1;
        RawTrace rawTrace = tu.createRawTrace(e1 = tu.getSignalMask(ALL_SIGNALS_ENABLED_MASK));
        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(rawTrace));
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        when(mockSignalMaskForEvents.getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e1).getEventId()))
                .thenReturn(SignalMask.UNKNOWN_MASK);

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereDisabledAtStart(), isEmptyMap());

        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart(),
                hasMapSize(Math.toIntExact(Constants.SIGNAL_NUMBER_COUNT)));
        Assert.assertTrue(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_1));
        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart().get(SIGNAL_NUMBER_1),
                containsExactly(rawTrace.getThreadInfo().getId()));
    }

    @Test
    public void doesNotInferFromDisabledReadIfAlreadyDisabled() {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalEnabledAtStartInferenceFromReads> producer =
                initProducer(module, mockRawTraces, mockSignalMaskForEvents);

        List<ReadonlyEventInterface> e1;
        RawTrace rawTrace = tu.createRawTrace(e1 = tu.getSignalMask(ALL_SIGNALS_DISABLED_MASK));
        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(rawTrace));
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        when(mockSignalMaskForEvents.getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e1).getEventId()))
                .thenReturn(SignalMask.UNKNOWN_MASK.disable(SIGNAL_NUMBER_1));

        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereDisabledAtStart(),
                hasMapSize(Math.toIntExact(Constants.SIGNAL_NUMBER_COUNT) - 1));
        Assert.assertFalse(producer.getComputed().getSignalToTtidWhereDisabledAtStart().containsKey(SIGNAL_NUMBER_1));
        Assert.assertThat(producer.getComputed().getSignalToTtidWhereEnabledAtStart(), isEmptyMap());
    }

    @Test
    public void doesNotInferFromEnabledReadIfAlreadyEnabled() {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<SignalEnabledAtStartInferenceFromReads> producer =
                initProducer(module, mockRawTraces, mockSignalMaskForEvents);

        List<ReadonlyEventInterface> e1;
        RawTrace rawTrace = tu.createRawTrace(e1 = tu.getSignalMask(ALL_SIGNALS_ENABLED_MASK));
        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(rawTrace));
        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        when(mockSignalMaskForEvents.getSignalMaskBeforeEvent(
                rawTrace.getThreadInfo().getId(), TraceUtils.extractSingleEvent(e1).getEventId()))
                .thenReturn(SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_1));

        Assert.assertThat(producer.getComputed().getSignalToTtidWhereDisabledAtStart(), isEmptyMap());

        Assert.assertThat(
                producer.getComputed().getSignalToTtidWhereEnabledAtStart(),
                hasMapSize(Math.toIntExact(Constants.SIGNAL_NUMBER_COUNT) - 1));
        Assert.assertFalse(producer.getComputed().getSignalToTtidWhereEnabledAtStart().containsKey(SIGNAL_NUMBER_1));
    }

    private static ComputingProducerWrapper<SignalEnabledAtStartInferenceFromReads> initProducer(
            ProducerModule module,
            RawTraces mockRawTraces,
            SignalMaskForEvents mockSignalMaskForEvents) {
        return new ComputingProducerWrapper<>(
                new SignalEnabledAtStartInferenceFromReads(
                        new ComputingProducerWrapper<>(mockRawTraces, module),
                        new ComputingProducerWrapper<>(mockSignalMaskForEvents, module)),
                module);
    }
}
