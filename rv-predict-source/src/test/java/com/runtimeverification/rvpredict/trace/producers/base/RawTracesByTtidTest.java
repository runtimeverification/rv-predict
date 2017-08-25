package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isAbsent;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isPresentWithValue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RawTracesByTtidTest {
    private static final int NO_SIGNAL = 0;
    private static final long THREAD_1 = 101L;
    private static final long PC_BASE = 201L;
    private static final long ADDRESS_1 = 301L;
    private static final long VALUE_1 = 401L;
    private static final int TTID_1 = 501;

    @Mock private Context mockContext;
    @Mock private RawTraces mockRawTraces;

    private final TestProducerModule module = new TestProducerModule();

    @Test
    public void noDataWithoutTraces() {
        ComputingProducerWrapper<RawTracesByTtid> producer = initProducer(module, mockRawTraces);
        module.reset();

        Assert.assertThat(producer.getComputed().getRawTrace(TTID_1), isAbsent());
    }

    @Test
    public void indexesTraces() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        ComputingProducerWrapper<RawTracesByTtid> producer = initProducer(module, mockRawTraces);
        module.reset();

        RawTrace trace = tu.createRawTrace(tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(trace));

        Assert.assertThat(producer.getComputed().getRawTrace(TTID_1), isAbsent());
        Assert.assertThat(producer.getComputed().getRawTrace(trace.getThreadInfo().getId()), isPresentWithValue(trace));
    }

    @Test
    public void resetsObject() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        ComputingProducerWrapper<RawTracesByTtid> producer = initProducer(module, mockRawTraces);
        module.reset();

        RawTrace trace = tu.createRawTrace(tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(trace));

        Assert.assertThat(producer.getComputed().getRawTrace(TTID_1), isAbsent());
        Assert.assertThat(producer.getComputed().getRawTrace(trace.getThreadInfo().getId()), isPresentWithValue(trace));

        module.reset();
        when(mockRawTraces.getTraces()).thenReturn(Collections.emptyList());
        Assert.assertThat(producer.getComputed().getRawTrace(TTID_1), isAbsent());
        Assert.assertThat(producer.getComputed().getRawTrace(trace.getThreadInfo().getId()), isAbsent());
    }

    private static ComputingProducerWrapper<RawTracesByTtid> initProducer(
            TestProducerModule module,
            RawTraces mockRawTraces) {
        ComputingProducerWrapper<RawTraces> rawTraces = new ComputingProducerWrapper<>(mockRawTraces, module);
        return new ComputingProducerWrapper<>(
                new RawTracesByTtid(rawTraces), module);
    }
}
