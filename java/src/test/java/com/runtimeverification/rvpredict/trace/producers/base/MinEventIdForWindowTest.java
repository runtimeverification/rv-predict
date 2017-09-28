package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MinEventIdForWindowTest {
    private static final int NO_SIGNAL = 0;
    private static final long THREAD_1 = 101L;
    private static final long PC_BASE = 201L;
    private static final long BASE_ID = 301L;
    private static final long ADDRESS_1 = 401L;
    private static final long VALUE_1 = 501L;

    @Mock private RawTraces mockRawTraces;
    @Mock private Context mockContext;

    private int nextIdDelta = 0;
    private TestProducerModule module;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
        module = new TestProducerModule();
    }

    @Test
    public void noMinIdWithoutTraces() {
        ComputingProducerWrapper<MinEventIdForWindow> producerWrapper = createAndRegister(mockRawTraces);

        when(mockRawTraces.getTraces()).thenReturn(Collections.emptyList());
        module.reset();
        Assert.assertFalse(producerWrapper.getComputed().getId().isPresent());
    }

    @Test
    public void minIdForSingleTrace() throws InvalidTraceDataException {
        ComputingProducerWrapper<MinEventIdForWindow> producerWrapper = createAndRegister(mockRawTraces);

        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        RawTrace rawTrace = tu.createRawTrace(tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(rawTrace));

        module.reset();

        OptionalLong id = producerWrapper.getComputed().getId();
        Assert.assertTrue(id.isPresent());
        Assert.assertEquals(rawTrace.getMinGID(), id.getAsLong());
    }

    @Test
    public void minIdForTwoTraces() throws InvalidTraceDataException {
        ComputingProducerWrapper<MinEventIdForWindow> producerWrapper = createAndRegister(mockRawTraces);

        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        RawTrace rawTrace1 = tu.createRawTrace(tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        RawTrace rawTrace2 = tu.createRawTrace(tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        List<RawTrace> traces = Arrays.asList(rawTrace1, rawTrace2);
        when(mockRawTraces.getTraces()).thenReturn(traces);

        module.reset();

        OptionalLong id = producerWrapper.getComputed().getId();
        Assert.assertTrue(id.isPresent());
        Assert.assertEquals(rawTrace1.getMinGID(), id.getAsLong());
        Assert.assertTrue(rawTrace1.getMinGID() < rawTrace2.getMinGID());
    }

    @Test
    public void minIdForTwoTracesReverseOrder() throws InvalidTraceDataException {
        ComputingProducerWrapper<MinEventIdForWindow> producerWrapper = createAndRegister(mockRawTraces);

        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        RawTrace rawTrace1 = tu.createRawTrace(tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        RawTrace rawTrace2 = tu.createRawTrace(tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        List<RawTrace> traces = Arrays.asList(rawTrace2, rawTrace1);
        when(mockRawTraces.getTraces()).thenReturn(traces);

        module.reset();

        OptionalLong id = producerWrapper.getComputed().getId();
        Assert.assertTrue(id.isPresent());
        Assert.assertEquals(rawTrace1.getMinGID(), id.getAsLong());
        Assert.assertTrue(rawTrace1.getMinGID() < rawTrace2.getMinGID());
    }

    @Test
    public void resetsComputation() throws InvalidTraceDataException {
        ComputingProducerWrapper<MinEventIdForWindow> producerWrapper = createAndRegister(mockRawTraces);

        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        RawTrace rawTrace1 = tu.createRawTrace(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        RawTrace rawTrace2 = tu.createRawTrace(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));

        Assert.assertTrue(rawTrace1.getMinGID() < rawTrace2.getMinGID());

        module.reset();
        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(rawTrace2));

        OptionalLong id = producerWrapper.getComputed().getId();
        Assert.assertTrue(id.isPresent());
        Assert.assertEquals(rawTrace2.getMinGID(), id.getAsLong());

        module.reset();
        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(rawTrace1));

        id = producerWrapper.getComputed().getId();
        Assert.assertTrue(id.isPresent());
        Assert.assertEquals(rawTrace1.getMinGID(), id.getAsLong());

        module.reset();
        when(mockRawTraces.getTraces()).thenReturn(Collections.emptyList());

        id = producerWrapper.getComputed().getId();
        Assert.assertFalse(id.isPresent());

        module.reset();
        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(rawTrace2));

        id = producerWrapper.getComputed().getId();
        Assert.assertTrue(id.isPresent());
        Assert.assertEquals(rawTrace2.getMinGID(), id.getAsLong());
    }

    private ComputingProducerWrapper<MinEventIdForWindow> createAndRegister(RawTraces rawTraces) {
        ComputingProducerWrapper<RawTraces> rawTracesWrapper = new ComputingProducerWrapper<>(rawTraces, module);
        return new ComputingProducerWrapper<>(new MinEventIdForWindow(rawTracesWrapper), module);
    }
}
