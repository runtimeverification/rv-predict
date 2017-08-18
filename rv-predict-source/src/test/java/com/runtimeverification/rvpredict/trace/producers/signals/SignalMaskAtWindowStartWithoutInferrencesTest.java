package com.runtimeverification.rvpredict.trace.producers.signals;

import com.google.common.collect.ImmutableMap;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.signals.SignalMask;
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
public class SignalMaskAtWindowStartWithoutInferrencesTest {
    private static final long SIGNAL_NUMBER_1 = 2;
    private static final long SIGNAL_NUMBER_2 = 3;
    private static final int TTID_1 = 101;

    @Mock private SignalMaskAtWindowStartLeaf mockSignalMaskAtWindowStartLeaf;

    private final TestProducerModule module = new TestProducerModule();

    @Test
    public void returnsSignals() {
        ComputingProducerWrapper<SignalMaskAtWindowStartWithoutInferrences> producer =
                initProducer(module, mockSignalMaskAtWindowStartLeaf);

        when(mockSignalMaskAtWindowStartLeaf.getMasks())
                .thenReturn(ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_1)));
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalMasks(), hasMapSize(1));
        Optional<SignalMask> maybeSignalMask = producer.getComputed().getMask(TTID_1);
        Assert.assertTrue(maybeSignalMask.isPresent());
        Assert.assertEquals(SignalMask.SignalMaskBit.ENABLED, maybeSignalMask.get().getMaskBit(SIGNAL_NUMBER_1));
        Assert.assertEquals(SignalMask.SignalMaskBit.UNKNOWN, maybeSignalMask.get().getMaskBit(SIGNAL_NUMBER_2));
    }

    @Test
    public void resets() {
        ComputingProducerWrapper<SignalMaskAtWindowStartWithoutInferrences> producer =
                initProducer(module, mockSignalMaskAtWindowStartLeaf);

        when(mockSignalMaskAtWindowStartLeaf.getMasks())
                .thenReturn(ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_1)));
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalMasks(), hasMapSize(1));
        Optional<SignalMask> maybeSignalMask = producer.getComputed().getMask(TTID_1);
        Assert.assertTrue(maybeSignalMask.isPresent());
        Assert.assertEquals(SignalMask.SignalMaskBit.ENABLED, maybeSignalMask.get().getMaskBit(SIGNAL_NUMBER_1));
        Assert.assertEquals(SignalMask.SignalMaskBit.UNKNOWN, maybeSignalMask.get().getMaskBit(SIGNAL_NUMBER_2));

        when(mockSignalMaskAtWindowStartLeaf.getMasks())
                .thenReturn(Collections.emptyMap());
        module.reset();

        Assert.assertThat(producer.getComputed().getSignalMasks(), isEmptyMap());
        Assert.assertThat(producer.getComputed().getMask(TTID_1), isAbsent());
    }

    private ComputingProducerWrapper<SignalMaskAtWindowStartWithoutInferrences> initProducer(
            TestProducerModule module,
            SignalMaskAtWindowStartLeaf signalMaskAtWindowStartLeaf) {
        return new ComputingProducerWrapper<>(
                new SignalMaskAtWindowStartWithoutInferrences(
                        new ComputingProducerWrapper<>(signalMaskAtWindowStartLeaf, module)),
                module
        );
    }
}
