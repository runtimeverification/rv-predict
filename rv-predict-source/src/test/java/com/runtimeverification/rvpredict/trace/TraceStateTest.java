package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.trace.Trace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.OptionalInt;

@RunWith(MockitoJUnitRunner.class)
public class TraceStateTest {
    private final static long THREAD_ID = 10;
    private final static long THREAD_ID_2 = 11;

    @Mock private Configuration mockConfiguration;
    @Mock private MetadataInterface mockMetadata;

    @Before
    public void setUp() {
        mockConfiguration.windowSize = 10;
    }

    @Test
    public void generatesThreadIds() {
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        Assert.assertEquals(1, traceState.getNewThreadId());
    }

    @Test
    public void remembersThreadsWhenGeneratingThreadIds() {
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        Assert.assertEquals(1, traceState.getNewThreadId(THREAD_ID));
        OptionalInt threadId = traceState.getUnfinishedThreadId(0, THREAD_ID);
        Assert.assertTrue(threadId.isPresent());
        Assert.assertEquals(1, threadId.getAsInt());
        Assert.assertFalse(traceState.getUnfinishedThreadId(1, THREAD_ID).isPresent());
        Assert.assertFalse(traceState.getUnfinishedThreadId(0, THREAD_ID_2).isPresent());
    }

    @Test
    public void remembersSignalsWhenGeneratingThreadIds() {
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        Assert.assertEquals(1, traceState.enterSignal(1, THREAD_ID));
        OptionalInt threadId = traceState.getUnfinishedThreadId(1, THREAD_ID);
        Assert.assertTrue(threadId.isPresent());
        Assert.assertEquals(1, threadId.getAsInt());
        Assert.assertFalse(traceState.getUnfinishedThreadId(0, THREAD_ID).isPresent());
        Assert.assertFalse(traceState.getUnfinishedThreadId(1, THREAD_ID_2).isPresent());

        traceState.exitSignal(1, THREAD_ID);
        Assert.assertFalse(traceState.getUnfinishedThreadId(1, THREAD_ID).isPresent());
    }
}
