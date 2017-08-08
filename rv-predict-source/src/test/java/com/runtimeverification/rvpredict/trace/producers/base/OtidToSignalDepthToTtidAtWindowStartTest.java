package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.trace.ThreadInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.OptionalInt;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isAbsentInt;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isPresentWithIntValue;
import static com.runtimeverification.rvpredict.testutils.ThreadInfosComponentUtils.clearMockThreadInfosComponent;
import static com.runtimeverification.rvpredict.testutils.ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos;
import static com.runtimeverification.rvpredict.testutils.TtidSetDifferenceUtils.clearMockTtidSetDifference;
import static com.runtimeverification.rvpredict.testutils.TtidSetDifferenceUtils.fillMockTtidSetDifference;

@RunWith(MockitoJUnitRunner.class)
public class OtidToSignalDepthToTtidAtWindowStartTest {
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final long SIGNAL_NUMBER_1 = 2L;
    private static final long THREAD_1 = 101L;
    private static final int TTID_1 = 201;
    private static final int TTID_2 = 202;
    private static final long SIGNAL_HANDLER_1 = 301L;

    private static final ThreadInfo THREAD_1_INFO = ThreadInfo.createThreadInfo(TTID_1, THREAD_1, OptionalInt.empty());
    private static final ThreadInfo SIGNAL_2_INFO =
            ThreadInfo.createSignalInfo(TTID_2, THREAD_1, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ONE_SIGNAL);

    private TestProducerModule module = new TestProducerModule();

    @Mock private TtidSetDifference mockThreadsRunningAtWindowStart;
    @Mock private ThreadInfosComponent mockThreadInfosComponent;

    @Test
    public void doesNotFindThreadWhichIsNotPresentInInput() {
        ComputingProducerWrapper<OtidToSignalDepthToTtidAtWindowStart> producer =
                initProducer(module, mockThreadsRunningAtWindowStart, mockThreadInfosComponent);
        module.reset();
        Assert.assertThat(producer.getComputed().getTtid(THREAD_1, NO_SIGNAL), isAbsentInt());
        Assert.assertThat(producer.getComputed().getTtid(THREAD_1, ONE_SIGNAL), isAbsentInt());
    }

    @Test
    public void doesNotFindThreadWhichIsNotRunningAtWindowStart() {
        ComputingProducerWrapper<OtidToSignalDepthToTtidAtWindowStart> producer =
                initProducer(module, mockThreadsRunningAtWindowStart, mockThreadInfosComponent);
        fillMockThreadInfosComponentFromThreadInfos(mockThreadInfosComponent, THREAD_1_INFO);

        module.reset();
        Assert.assertThat(producer.getComputed().getTtid(THREAD_1, NO_SIGNAL), isAbsentInt());
        Assert.assertThat(producer.getComputed().getTtid(THREAD_1, ONE_SIGNAL), isAbsentInt());
    }

    @Test
    public void findsThread() {
        ComputingProducerWrapper<OtidToSignalDepthToTtidAtWindowStart> producer =
                initProducer(module, mockThreadsRunningAtWindowStart, mockThreadInfosComponent);
        fillMockThreadInfosComponentFromThreadInfos(mockThreadInfosComponent, THREAD_1_INFO);
        fillMockTtidSetDifference(mockThreadsRunningAtWindowStart, TTID_1);

        module.reset();
        Assert.assertThat(producer.getComputed().getTtid(THREAD_1, NO_SIGNAL), isPresentWithIntValue(TTID_1));
        Assert.assertThat(producer.getComputed().getTtid(THREAD_1, ONE_SIGNAL), isAbsentInt());
    }

    @Test
    public void findsSignal() {
        ComputingProducerWrapper<OtidToSignalDepthToTtidAtWindowStart> producer =
                initProducer(module, mockThreadsRunningAtWindowStart, mockThreadInfosComponent);
        fillMockThreadInfosComponentFromThreadInfos(mockThreadInfosComponent, SIGNAL_2_INFO);
        fillMockTtidSetDifference(mockThreadsRunningAtWindowStart, TTID_2);

        module.reset();
        Assert.assertThat(producer.getComputed().getTtid(THREAD_1, NO_SIGNAL), isAbsentInt());
        Assert.assertThat(producer.getComputed().getTtid(THREAD_1, ONE_SIGNAL), isPresentWithIntValue(TTID_2));

        clearMockThreadInfosComponent(mockThreadInfosComponent);
        clearMockTtidSetDifference(mockThreadsRunningAtWindowStart);

        module.reset();
        Assert.assertThat(producer.getComputed().getTtid(THREAD_1, NO_SIGNAL), isAbsentInt());
        Assert.assertThat(producer.getComputed().getTtid(THREAD_1, ONE_SIGNAL), isAbsentInt());
    }

    @Test
    public void resets() {
        ComputingProducerWrapper<OtidToSignalDepthToTtidAtWindowStart> producer =
                initProducer(module, mockThreadsRunningAtWindowStart, mockThreadInfosComponent);
        fillMockThreadInfosComponentFromThreadInfos(mockThreadInfosComponent, SIGNAL_2_INFO);
        fillMockTtidSetDifference(mockThreadsRunningAtWindowStart, TTID_2);

        module.reset();
        Assert.assertThat(producer.getComputed().getTtid(THREAD_1, NO_SIGNAL), isAbsentInt());
        Assert.assertThat(producer.getComputed().getTtid(THREAD_1, ONE_SIGNAL), isPresentWithIntValue(TTID_2));
    }

    private static ComputingProducerWrapper<OtidToSignalDepthToTtidAtWindowStart> initProducer(
            TestProducerModule module,
            TtidSetDifference mockThreadsRunningAtWindowStart,
            ThreadInfosComponent mockThreadInfosComponent) {
        ComputingProducerWrapper<TtidSetDifference> threadsRunningAtWindowStart = new ComputingProducerWrapper<>(
                mockThreadsRunningAtWindowStart, module);
        ComputingProducerWrapper<ThreadInfosComponent> threadInfosComponent = new ComputingProducerWrapper<>(
                mockThreadInfosComponent, module);
        return new ComputingProducerWrapper<>(new OtidToSignalDepthToTtidAtWindowStart(
                threadsRunningAtWindowStart, threadInfosComponent), module);
    }
}
