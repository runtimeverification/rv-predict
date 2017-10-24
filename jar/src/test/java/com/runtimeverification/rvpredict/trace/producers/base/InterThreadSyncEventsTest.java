package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerModule;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsExactly;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmpty;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InterThreadSyncEventsTest {
    private static final int NO_SIGNAL = 0;
    private static final long ADDRESS_1 = 101L;
    private static final long VALUE_1 = 201L;
    private static final long THREAD_1 = 301L;
    private static final long THREAD_2 = 302L;
    private static final long PC_BASE = 401L;
    private static final long BASE_ID = 501L;

    @Mock private RawTraces mockRawTraces;
    @Mock private Context mockContext;

    private final TestProducerModule module = new TestProducerModule();
    private int nextIdDelta = 0;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
    }

    @Test
    public void noSyncEventsWithoutTraces() {
        ComputingProducerWrapper<InterThreadSyncEvents> producer = initProducer(module, mockRawTraces);

        when(mockRawTraces.getTraces()).thenReturn(Collections.emptyList());
        module.reset();

        Assert.assertThat(producer.getComputed().getSyncEvents(), isEmpty());
    }

    @Test
    public void noSyncEventsWithoutSyncEventsInTrace() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<InterThreadSyncEvents> producer = initProducer(module, mockRawTraces);

        List<RawTrace> rawTraces = Collections.singletonList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1)));
        when(mockRawTraces.getTraces()).thenReturn(rawTraces);
        module.reset();

        Assert.assertThat(producer.getComputed().getSyncEvents(), isEmpty());
    }

    @Test
    public void extractsSyncEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<InterThreadSyncEvents> producer = initProducer(module, mockRawTraces);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<RawTrace> rawTraces = Collections.singletonList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        e1 = tu.threadStart(THREAD_2),
                        e2 = tu.threadJoin(THREAD_2)));
        when(mockRawTraces.getTraces()).thenReturn(rawTraces);
        module.reset();

        Assert.assertThat(producer.getComputed().getSyncEvents(), containsExactly(
                TraceUtils.extractSingleEvent(e1),
                TraceUtils.extractSingleEvent(e2)));
    }

    @Test
    public void resets() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<InterThreadSyncEvents> producer = initProducer(module, mockRawTraces);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<RawTrace> rawTraces = Collections.singletonList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        e1 = tu.threadStart(THREAD_2),
                        e2 = tu.threadJoin(THREAD_2)));
        when(mockRawTraces.getTraces()).thenReturn(rawTraces);
        module.reset();

        Assert.assertThat(producer.getComputed().getSyncEvents(), containsExactly(
                TraceUtils.extractSingleEvent(e1),
                TraceUtils.extractSingleEvent(e2)));

        when(mockRawTraces.getTraces()).thenReturn(Collections.emptyList());
        module.reset();

        Assert.assertThat(producer.getComputed().getSyncEvents(), isEmpty());
    }

    private static ComputingProducerWrapper<InterThreadSyncEvents> initProducer(
            ProducerModule module,
            RawTraces rawTraces) {
        return new ComputingProducerWrapper<>(
                new InterThreadSyncEvents(new ComputingProducerWrapper<>(rawTraces, module)),
                module);
    }
}
