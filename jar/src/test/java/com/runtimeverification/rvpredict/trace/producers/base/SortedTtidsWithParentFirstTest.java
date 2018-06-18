package com.runtimeverification.rvpredict.trace.producers.base;

import com.google.common.collect.Sets;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.trace.ThreadInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.OptionalInt;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsInOrder;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmpty;
import static com.runtimeverification.rvpredict.testutils.ThreadInfosComponentUtils.clearMockThreadInfosComponent;
import static com.runtimeverification.rvpredict.testutils.ThreadInfosComponentUtils.fillMockThreadInfosComponentFromThreadInfos;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SortedTtidsWithParentFirstTest {
    private static final int ONE_SIGNAL = 1;
    private static final int TWO_SIGNALS = 2;
    private static final long SIGNAL_NUMBER_1 = 2L;
    private static final long THREAD_1 = 101L;
    private static final long THREAD_2 = 102L;
    private static final long THREAD_3 = 103L;
    private static final int TTID_1 = 201;
    private static final int TTID_2 = 202;
    private static final int TTID_3 = 203;
    private static final int TTID_4 = 204;
    private static final int TTID_5 = 205;
    private static final long SIGNAL_HANDLER_1 = 301L;

    private static final ThreadInfo THREAD_1_INFO =
            ThreadInfo.createThreadInfo(TTID_1, THREAD_1, OptionalInt.empty());
    private static final ThreadInfo THREAD_2_INFO =
            ThreadInfo.createThreadInfo(TTID_2, THREAD_2, OptionalInt.of(TTID_1));
    private static final ThreadInfo THREAD_3_INFO =
            ThreadInfo.createThreadInfo(TTID_3, THREAD_3, OptionalInt.of(TTID_2));
    private static final ThreadInfo SIGNAL_4_INFO =
            ThreadInfo.createSignalInfo(TTID_4, THREAD_1, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ONE_SIGNAL);
    private static final ThreadInfo SIGNAL_5_INFO =
            ThreadInfo.createSignalInfo(TTID_5, THREAD_1, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, TWO_SIGNALS);

    @Mock private TtidSetLeaf mockTtidsForCurrentWindow;
    @Mock private ThreadInfosComponent mockThreadInfosComponent;

    private final TestProducerModule module = new TestProducerModule();

    @Test
    public void emptySortingForEmptyInput() {
        ComputingProducerWrapper<SortedTtidsWithParentFirst> producer =
                initProducer(module, mockTtidsForCurrentWindow, mockThreadInfosComponent);
        module.reset();

        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(Collections.emptySet());

        Assert.assertThat(producer.getComputed().getTtids(), isEmpty());
    }

    @Test
    public void sortsOneThread() {
        ComputingProducerWrapper<SortedTtidsWithParentFirst> producer =
                initProducer(module, mockTtidsForCurrentWindow, mockThreadInfosComponent);
        module.reset();

        fillMockThreadInfosComponentFromThreadInfos(mockThreadInfosComponent, THREAD_1_INFO);
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(Collections.singleton(THREAD_1_INFO.getId()));

        Assert.assertThat(
                producer.getComputed().getTtids(),
                containsInOrder(THREAD_1_INFO.getId()));
    }

    @Test
    public void sortsMultipleThreads() {
        ComputingProducerWrapper<SortedTtidsWithParentFirst> producer =
                initProducer(module, mockTtidsForCurrentWindow, mockThreadInfosComponent);
        module.reset();

        fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent, THREAD_1_INFO, THREAD_3_INFO, THREAD_2_INFO);
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(Sets.newHashSet(
                THREAD_3_INFO.getId(), THREAD_2_INFO.getId(), THREAD_1_INFO.getId()));

        Assert.assertThat(
                producer.getComputed().getTtids(),
                containsInOrder(THREAD_1_INFO.getId(), THREAD_2_INFO.getId(), THREAD_3_INFO.getId()));
    }

    @Test
    public void ignoresThreadsNotRelevantToCurrentWindow() {
        ComputingProducerWrapper<SortedTtidsWithParentFirst> producer =
                initProducer(module, mockTtidsForCurrentWindow, mockThreadInfosComponent);
        module.reset();

        fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent, THREAD_1_INFO, THREAD_3_INFO, THREAD_2_INFO);
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(Sets.newHashSet(
                THREAD_3_INFO.getId(), THREAD_1_INFO.getId()));

        Assert.assertThat(
                producer.getComputed().getTtids(),
                containsInOrder(THREAD_1_INFO.getId(), THREAD_3_INFO.getId()));
    }

    @Test
    public void addsSignalsAtEndSortedByDepth() {
        ComputingProducerWrapper<SortedTtidsWithParentFirst> producer =
                initProducer(module, mockTtidsForCurrentWindow, mockThreadInfosComponent);
        module.reset();

        fillMockThreadInfosComponentFromThreadInfos(
                mockThreadInfosComponent, THREAD_1_INFO, THREAD_3_INFO, THREAD_2_INFO,
                SIGNAL_5_INFO, SIGNAL_4_INFO);
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(Sets.newHashSet(
                SIGNAL_5_INFO.getId(), SIGNAL_4_INFO.getId(),
                THREAD_3_INFO.getId(), THREAD_2_INFO.getId(), THREAD_1_INFO.getId()));

        Assert.assertThat(
                producer.getComputed().getTtids(),
                containsInOrder(
                        THREAD_1_INFO.getId(), THREAD_2_INFO.getId(), THREAD_3_INFO.getId(),
                        SIGNAL_4_INFO.getId(), SIGNAL_5_INFO.getId()));
    }

    @Test
    public void resets() {
        ComputingProducerWrapper<SortedTtidsWithParentFirst> producer =
                initProducer(module, mockTtidsForCurrentWindow, mockThreadInfosComponent);

        module.reset();
        fillMockThreadInfosComponentFromThreadInfos(mockThreadInfosComponent, THREAD_1_INFO);
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(Sets.newHashSet(THREAD_1_INFO.getId()));

        Assert.assertThat(
                producer.getComputed().getTtids(),
                containsInOrder(THREAD_1_INFO.getId()));

        module.reset();

        clearMockThreadInfosComponent(mockThreadInfosComponent);
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(Collections.emptySet());
        Assert.assertThat(producer.getComputed().getTtids(), isEmpty());
    }

    private static ComputingProducerWrapper<SortedTtidsWithParentFirst> initProducer(
            TestProducerModule module,
            TtidSetLeaf mockTtidsForCurrentWindow,
            ThreadInfosComponent mockThreadInfosComponent) {
        return new ComputingProducerWrapper<>(
                new SortedTtidsWithParentFirst(
                        new ComputingProducerWrapper<>(mockTtidsForCurrentWindow, module),
                        new ComputingProducerWrapper<>(mockThreadInfosComponent, module)),
                module);
    }
}
