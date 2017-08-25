package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.LeafProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.trace.ThreadInfo;
import com.runtimeverification.rvpredict.trace.ThreadInfos;
import com.runtimeverification.rvpredict.trace.ThreadType;
import org.junit.Assert;
import org.junit.Test;

import java.util.OptionalInt;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isAbsentInt;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isAbsentLong;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isPresentWithIntValue;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isPresentWithLongValue;

public class ThreadInfosComponentTest {
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final long SIGNAL_NUMBER_1 = 2L;
    private static final long THREAD_1 = 101L;
    private static final long THREAD_2 = 102L;
    private static final int TTID_1 = 201;
    private static final int TTID_2 = 202;
    private static final int TTID_3 = 203;
    private static final long SIGNAL_HANDLER_1 = 301L;

    private static final ThreadInfo THREAD_1_INFO =
            ThreadInfo.createThreadInfo(TTID_1, THREAD_1, OptionalInt.empty());
    private static final ThreadInfo THREAD_3_INFO =
            ThreadInfo.createThreadInfo(TTID_3, THREAD_2, OptionalInt.of(TTID_1));
    private static final ThreadInfo SIGNAL_2_INFO =
            ThreadInfo.createSignalInfo(TTID_2, THREAD_1, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ONE_SIGNAL);

    private final TestProducerModule module = new TestProducerModule();

    @Test
    public void returnsInfos() {
        LeafProducerWrapper<ThreadInfos, ThreadInfosComponent> producer =
                new LeafProducerWrapper<>(new ThreadInfosComponent(), module);
        module.reset();
        ThreadInfos threadInfos = new ThreadInfos();
        threadInfos.registerThreadInfo(THREAD_1_INFO);
        threadInfos.registerThreadInfo(SIGNAL_2_INFO);
        threadInfos.registerThreadInfo(THREAD_3_INFO);
        producer.set(threadInfos);

        // Intentional == comparisons.
        Assert.assertTrue(THREAD_1_INFO == producer.getComputed().getThreadInfo(TTID_1));
        Assert.assertTrue(SIGNAL_2_INFO == producer.getComputed().getThreadInfo(TTID_2));

        Assert.assertEquals(ThreadType.THREAD, producer.getComputed().getThreadType(TTID_1));
        Assert.assertEquals(ThreadType.SIGNAL, producer.getComputed().getThreadType(TTID_2));

        Assert.assertEquals(THREAD_1, producer.getComputed().getOriginalThreadIdForTraceThreadId(TTID_1));
        Assert.assertEquals(THREAD_1, producer.getComputed().getOriginalThreadIdForTraceThreadId(TTID_2));

        Assert.assertEquals(NO_SIGNAL, producer.getComputed().getSignalDepth(TTID_1));
        Assert.assertEquals(ONE_SIGNAL, producer.getComputed().getSignalDepth(TTID_2));

        Assert.assertThat(producer.getComputed().getParentThread(TTID_1), isAbsentInt());
        Assert.assertThat(producer.getComputed().getParentThread(TTID_2), isAbsentInt());
        Assert.assertThat(producer.getComputed().getParentThread(TTID_3), isPresentWithIntValue(TTID_1));

        Assert.assertThat(producer.getComputed().getSignalNumber(TTID_1), isAbsentLong());
        Assert.assertThat(producer.getComputed().getSignalNumber(TTID_2), isPresentWithLongValue(SIGNAL_NUMBER_1));
    }
}
