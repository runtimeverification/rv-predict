package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.SharedLibrary;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasSize;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SharedLibrariesTest {
    private static final int NO_SIGNAL = 0;
    private static final long BASE_ID = 101L;
    private static final long THREAD_1 = 201L;
    private static final long PC_BASE = 301L;
    private static final int LIBRARY_ID_1 = 401;
    private static final int LIBRARY_START = 501;
    private static final int LIBRARY_SIZE = 601;
    private static final String LIBRARY_NAME_1 = "library-name-1";

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
    public void noLibrariesWithoutLibrariesInTrace() {
        ComputingProducerWrapper<SharedLibraries> sharedLibraries = createProducer(mockRawTraces);

        when(mockRawTraces.getTraces()).thenReturn(Collections.emptyList());
        SharedLibraries computed = sharedLibraries.getComputed();
        Assert.assertTrue(computed.getLibraries().isEmpty());
    }

    @Test
    public void buildsLibrary() {
        ComputingProducerWrapper<SharedLibraries> sharedLibraries = createProducer(mockRawTraces);

        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        RawTrace rawTrace = tu.createRawTrace(tu.sharedLibrary(LIBRARY_ID_1, LIBRARY_NAME_1));

        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(rawTrace));
        SharedLibraries computed = sharedLibraries.getComputed();
        Assert.assertThat(computed.getLibraries(), hasSize(1));
        SharedLibrary library = computed.getLibraries().iterator().next();
        Assert.assertFalse(library.containsAddress(LIBRARY_START - 1));
        Assert.assertFalse(library.containsAddress(LIBRARY_START + 1));
        Assert.assertFalse(library.containsAddress(LIBRARY_START + LIBRARY_SIZE - 1));
        Assert.assertFalse(library.containsAddress(LIBRARY_START + LIBRARY_SIZE + 1));
        Assert.assertEquals(LIBRARY_NAME_1, library.getName());
    }

    @Test
    public void buildsLibraryWithSegments() {
        ComputingProducerWrapper<SharedLibraries> sharedLibraries = createProducer(mockRawTraces);

        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        RawTrace rawTrace = tu.createRawTrace(
                tu.sharedLibrary(LIBRARY_ID_1, LIBRARY_NAME_1),
                tu.sharedLibrarySegment(LIBRARY_ID_1, LIBRARY_START, LIBRARY_SIZE));

        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(rawTrace));
        SharedLibraries computed = sharedLibraries.getComputed();
        Assert.assertThat(computed.getLibraries(), hasSize(1));
        SharedLibrary library = computed.getLibraries().iterator().next();
        Assert.assertFalse(library.containsAddress(LIBRARY_START - 1));
        Assert.assertTrue(library.containsAddress(LIBRARY_START + 1));
        Assert.assertTrue(library.containsAddress(LIBRARY_START + LIBRARY_SIZE - 1));
        Assert.assertFalse(library.containsAddress(LIBRARY_START + LIBRARY_SIZE + 1));
        Assert.assertEquals(LIBRARY_NAME_1, library.getName());
    }

    private ComputingProducerWrapper<SharedLibraries> createProducer(
            RawTraces rawTraces) {
        return new ComputingProducerWrapper<>(
                new SharedLibraries(new ComputingProducerWrapper<>(rawTraces, module)),
                module);
    }
}
