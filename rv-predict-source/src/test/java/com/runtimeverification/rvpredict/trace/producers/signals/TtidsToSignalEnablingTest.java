package com.runtimeverification.rvpredict.trace.producers.signals;

import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.testutils.SignalMasksAtWindowStartUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsExactly;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasMapSize;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmptyMap;

@RunWith(MockitoJUnitRunner.class)
public class TtidsToSignalEnablingTest {
    private static final long SIGNAL_NUMBER_1 = 2L;
    private static final long SIGNAL_NUMBER_2 = 3L;
    private static final int TTID_1 = 101;

    @Mock private SignalMaskAtWindowStart<? extends ProducerState> mockSignalMaskAtWindowStart;

    private final TestProducerModule module = new TestProducerModule();

    @Test
    public void emptyOutputForEmptyInput() {
        ComputingProducerWrapper<TtidsToSignalEnabling> producer = initProducer(module, mockSignalMaskAtWindowStart);

        SignalMasksAtWindowStartUtils.clearMockSignalMasksAtWindowStart(mockSignalMaskAtWindowStart);
        module.reset();

        Map<Long, Set<Integer>> signalToTtidWhereDisabled =
                producer.getComputed().getSignalToTtidWhereDisabledAtStart();
        Assert.assertThat(signalToTtidWhereDisabled, isEmptyMap());
        Map<Long, Set<Integer>> signalToTtidWhereEnabled =
                producer.getComputed().getSignalToTtidWhereEnabledAtStart();
        Assert.assertThat(signalToTtidWhereEnabled, isEmptyMap());
    }

    @Test
    public void copiesSignalsToOutput() {
        ComputingProducerWrapper<TtidsToSignalEnabling> producer = initProducer(module, mockSignalMaskAtWindowStart);

        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                Collections.singletonMap(
                        TTID_1, SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_1).disable(SIGNAL_NUMBER_2)));
        module.reset();

        Map<Long, Set<Integer>> signalToTtidWhereDisabled =
                producer.getComputed().getSignalToTtidWhereDisabledAtStart();
        Assert.assertThat(signalToTtidWhereDisabled, hasMapSize(1));
        Assert.assertThat(signalToTtidWhereDisabled.get(SIGNAL_NUMBER_2), containsExactly(TTID_1));
        Map<Long, Set<Integer>> signalToTtidWhereEnabled =
                producer.getComputed().getSignalToTtidWhereEnabledAtStart();
        Assert.assertThat(signalToTtidWhereEnabled, hasMapSize(1));
        Assert.assertThat(signalToTtidWhereEnabled.get(SIGNAL_NUMBER_1), containsExactly(TTID_1));
    }

    private static ComputingProducerWrapper<TtidsToSignalEnabling> initProducer(
            TestProducerModule module,
            SignalMaskAtWindowStart<? extends ProducerState> signalMaskAtWindowStart) {
        return new ComputingProducerWrapper<>(
                new TtidsToSignalEnabling(
                        new ComputingProducerWrapper<>(signalMaskAtWindowStart, module)),
                module);
    }
}
