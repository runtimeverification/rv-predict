package com.runtimeverification.rvpredict.trace.producers.signals;

import com.google.common.collect.ImmutableMap;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerModule;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.testutils.SignalMasksAtWindowStartUtils;
import com.runtimeverification.rvpredict.trace.producers.base.TtidsForCurrentWindow;
import com.runtimeverification.rvpredict.util.Constants;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasMapSize;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isAbsent;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmptyMap;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SignalMaskAtWindowOrThreadStartWithInferencesTest {
    private static final long SIGNAL_NUMBER_1 = 2;
    private static final long SIGNAL_NUMBER_2 = 3;
    private static final long SIGNAL_NUMBER_3 = 4;
    private static final int TTID_1 = 101;

    @Mock private SignalMaskAtWindowStart<? extends ProducerState> mockSignalMaskAtWindowStart;
    @Mock private SignalEnabledAtStartInferenceTransitiveClosure mockSignalEnabledAtStartInferenceTransitiveClosure;
    @Mock private TtidsForCurrentWindow mockTtidsForCurrentWindow;

    private final TestProducerModule module = new TestProducerModule();

    @Test
    public void noOutputSignalMaskWhenNoInput() {
        ComputingProducerWrapper<SignalMaskAtWindowOrThreadStartWithInferences> producer =
                initProducer(
                        module,
                        mockSignalMaskAtWindowStart,
                        mockSignalEnabledAtStartInferenceTransitiveClosure,
                        mockTtidsForCurrentWindow);

        SignalMasksAtWindowStartUtils.clearMockSignalMasksAtWindowStart(mockSignalMaskAtWindowStart);
        when(mockSignalEnabledAtStartInferenceTransitiveClosure.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(Collections.emptyMap());
        when(mockSignalEnabledAtStartInferenceTransitiveClosure.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(Collections.emptyMap());
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(Collections.emptyList());
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalMasks(), isEmptyMap());
        Assert.assertThat(producer.getComputed().getMask(TTID_1), isAbsent());
    }

    @Test
    public void preservesSignalMaskWithoutInferences() {
        ComputingProducerWrapper<SignalMaskAtWindowOrThreadStartWithInferences> producer =
                initProducer(
                        module,
                        mockSignalMaskAtWindowStart,
                        mockSignalEnabledAtStartInferenceTransitiveClosure,
                        mockTtidsForCurrentWindow);

        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(
                        TTID_1,
                        SignalMask.UNKNOWN_MASK
                                .enable(SIGNAL_NUMBER_1, Constants.INVALID_EVENT_ID)
                                .disable(SIGNAL_NUMBER_2, Constants.INVALID_EVENT_ID)));
        when(mockSignalEnabledAtStartInferenceTransitiveClosure.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(Collections.emptyMap());
        when(mockSignalEnabledAtStartInferenceTransitiveClosure.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(Collections.emptyMap());
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(Collections.singletonList(TTID_1));
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalMasks(), hasMapSize(1));
        Optional<SignalMask> maybeSignalMask = producer.getComputed().getMask(TTID_1);
        Assert.assertTrue(maybeSignalMask.isPresent());
        SignalMask signalMask = maybeSignalMask.get();
        Assert.assertEquals(SignalMask.SignalMaskBit.ENABLED, signalMask.getMaskBit(SIGNAL_NUMBER_1));
        Assert.assertEquals(SignalMask.SignalMaskBit.DISABLED, signalMask.getMaskBit(SIGNAL_NUMBER_2));
        Assert.assertEquals(SignalMask.SignalMaskBit.UNKNOWN, signalMask.getMaskBit(SIGNAL_NUMBER_3));
    }

    @Test
    public void fillsMaskWithInferences() {
        ComputingProducerWrapper<SignalMaskAtWindowOrThreadStartWithInferences> producer =
                initProducer(
                        module,
                        mockSignalMaskAtWindowStart,
                        mockSignalEnabledAtStartInferenceTransitiveClosure,
                        mockTtidsForCurrentWindow);

        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK));
        when(mockSignalEnabledAtStartInferenceTransitiveClosure.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_1, ImmutableMap.of(TTID_1, Constants.INVALID_EVENT_ID)));
        when(mockSignalEnabledAtStartInferenceTransitiveClosure.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(ImmutableMap.of(SIGNAL_NUMBER_2, ImmutableMap.of(TTID_1, Constants.INVALID_EVENT_ID)));
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(Collections.singletonList(TTID_1));
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalMasks(), hasMapSize(1));
        Optional<SignalMask> maybeSignalMask = producer.getComputed().getMask(TTID_1);
        Assert.assertTrue(maybeSignalMask.isPresent());
        SignalMask signalMask = maybeSignalMask.get();
        Assert.assertEquals(SignalMask.SignalMaskBit.ENABLED, signalMask.getMaskBit(SIGNAL_NUMBER_1));
        Assert.assertEquals(SignalMask.SignalMaskBit.DISABLED, signalMask.getMaskBit(SIGNAL_NUMBER_2));
        Assert.assertEquals(SignalMask.SignalMaskBit.UNKNOWN, signalMask.getMaskBit(SIGNAL_NUMBER_3));
    }

    @Test
    public void resets() {
        ComputingProducerWrapper<SignalMaskAtWindowOrThreadStartWithInferences> producer =
                initProducer(
                        module,
                        mockSignalMaskAtWindowStart,
                        mockSignalEnabledAtStartInferenceTransitiveClosure,
                        mockTtidsForCurrentWindow);

        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(
                        TTID_1,
                        SignalMask.UNKNOWN_MASK
                                .enable(SIGNAL_NUMBER_1, Constants.INVALID_EVENT_ID)
                                .disable(SIGNAL_NUMBER_2, Constants.INVALID_EVENT_ID)));
        when(mockSignalEnabledAtStartInferenceTransitiveClosure.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(Collections.emptyMap());
        when(mockSignalEnabledAtStartInferenceTransitiveClosure.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(Collections.emptyMap());
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(Collections.singletonList(TTID_1));
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalMasks(), hasMapSize(1));
        Optional<SignalMask> maybeSignalMask = producer.getComputed().getMask(TTID_1);
        Assert.assertTrue(maybeSignalMask.isPresent());
        SignalMask signalMask = maybeSignalMask.get();
        Assert.assertEquals(SignalMask.SignalMaskBit.ENABLED, signalMask.getMaskBit(SIGNAL_NUMBER_1));
        Assert.assertEquals(SignalMask.SignalMaskBit.DISABLED, signalMask.getMaskBit(SIGNAL_NUMBER_2));
        Assert.assertEquals(SignalMask.SignalMaskBit.UNKNOWN, signalMask.getMaskBit(SIGNAL_NUMBER_3));

        SignalMasksAtWindowStartUtils.clearMockSignalMasksAtWindowStart(mockSignalMaskAtWindowStart);
        when(mockSignalEnabledAtStartInferenceTransitiveClosure.getSignalToTtidWhereEnabledAtStart())
                .thenReturn(Collections.emptyMap());
        when(mockSignalEnabledAtStartInferenceTransitiveClosure.getSignalToTtidWhereDisabledAtStart())
                .thenReturn(Collections.emptyMap());
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(Collections.emptyList());
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalMasks(), isEmptyMap());
        Assert.assertThat(producer.getComputed().getMask(TTID_1), isAbsent());
    }

    private ComputingProducerWrapper<SignalMaskAtWindowOrThreadStartWithInferences> initProducer(
            ProducerModule module,
            SignalMaskAtWindowStart<? extends ProducerState> signalMaskAtWindowStart,
            SignalEnabledAtStartInferenceTransitiveClosure signalEnabledAtStartInferenceTransitiveClosure,
            TtidsForCurrentWindow ttidsForCurrentWindow) {
        return new ComputingProducerWrapper<>(
                new SignalMaskAtWindowOrThreadStartWithInferences(
                        new ComputingProducerWrapper<>(signalMaskAtWindowStart, module),
                        new ComputingProducerWrapper<>(signalEnabledAtStartInferenceTransitiveClosure, module),
                        new ComputingProducerWrapper<>(ttidsForCurrentWindow, module)),
                module);
    }
}
