package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.producerframework.LeafProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasSize;

@RunWith(MockitoJUnitRunner.class)
public class RawTracesTest {
    private static final int NO_SIGNAL = 0;
    private static final long THREAD_1 = 101L;
    private static final long PC_BASE = 201L;
    private static final long ADDRESS_1 = 301L;
    private static final long VALUE_1 = 401L;

    @Mock private Context mockContext;

    private final TestProducerModule module = new TestProducerModule();

    @Test
    public void returnsTraces() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        LeafProducerWrapper<List<RawTrace>, RawTraces> producer =
                new LeafProducerWrapper<>(new RawTraces(), module);
        module.reset();
        producer.set(Collections.singletonList(tu.createRawTrace(tu.nonAtomicStore(ADDRESS_1, VALUE_1))));
        Assert.assertThat(producer.getComputed().getTraces(), hasSize(1));
    }
}
