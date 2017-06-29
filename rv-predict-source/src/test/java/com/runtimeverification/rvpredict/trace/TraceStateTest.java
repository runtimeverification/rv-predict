package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.OptionalInt;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TraceStateTest {
    private final static long THREAD_ID = 10L;
    private final static long THREAD_ID_2 = 11L;

    private final static long SIGNAL_NUMBER_1 = 13L;

    private final static long SIGNAL_HANDLER_1 = 101L;

    @Mock private Configuration mockConfiguration;
    @Mock private MetadataInterface mockMetadata;
    @Mock private ReadonlyEventInterface mockEvent1;
    @Mock private ReadonlyEventInterface mockEvent2;

    @Before
    public void setUp() {
        mockConfiguration.windowSize = 10;
        when(mockEvent1.getEventId()).thenReturn(1L);
        when(mockEvent2.getEventId()).thenReturn(2L);
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

    @Test
    public void remembersPreviousWindowEstablishEvents() {
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        when(mockEvent1.getType()).thenReturn(EventType.ESTABLISH_SIGNAL);
        when(mockEvent1.getSignalNumber()).thenReturn(10L);
        when(mockEvent1.getSignalHandlerAddress()).thenReturn(11L);
        traceState.onSignalEvent(mockEvent1);
        Assert.assertEquals(
                mockEvent1, traceState.getSignalNumberToHandlerToPreviousWindowEstablishEvent().get(10L).get(11L));
    }

    @Test
    public void remembersTheLastPreviousWindowEstablishEvent() {
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        when(mockEvent1.getType()).thenReturn(EventType.ESTABLISH_SIGNAL);
        when(mockEvent1.getSignalNumber()).thenReturn(SIGNAL_NUMBER_1);
        when(mockEvent1.getSignalHandlerAddress()).thenReturn(SIGNAL_HANDLER_1);

        when(mockEvent2.getType()).thenReturn(EventType.ESTABLISH_SIGNAL);
        when(mockEvent2.getSignalNumber()).thenReturn(SIGNAL_NUMBER_1);
        when(mockEvent2.getSignalHandlerAddress()).thenReturn(SIGNAL_HANDLER_1);

        traceState.onSignalEvent(mockEvent1);
        Assert.assertEquals(
                mockEvent1,
                traceState.getSignalNumberToHandlerToPreviousWindowEstablishEvent()
                        .get(SIGNAL_NUMBER_1).get(SIGNAL_HANDLER_1));

        traceState.onSignalEvent(mockEvent2);
        Assert.assertEquals(
                mockEvent2,
                traceState.getSignalNumberToHandlerToPreviousWindowEstablishEvent()
                        .get(SIGNAL_NUMBER_1).get(SIGNAL_HANDLER_1));

        traceState.onSignalEvent(mockEvent1);
        Assert.assertEquals(
                mockEvent2,
                traceState.getSignalNumberToHandlerToPreviousWindowEstablishEvent()
                        .get(SIGNAL_NUMBER_1).get(SIGNAL_HANDLER_1));
    }

    @Test
    public void remembersTheLastPreviousWindowEstablishEventWhenFastProcessing() {
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        when(mockEvent1.getType()).thenReturn(EventType.ESTABLISH_SIGNAL);
        when(mockEvent1.getSignalNumber()).thenReturn(SIGNAL_NUMBER_1);
        when(mockEvent1.getSignalHandlerAddress()).thenReturn(SIGNAL_HANDLER_1);
        when(mockEvent1.isSignalEvent()).thenReturn(true);

        when(mockEvent2.getType()).thenReturn(EventType.ESTABLISH_SIGNAL);
        when(mockEvent2.getSignalNumber()).thenReturn(SIGNAL_NUMBER_1);
        when(mockEvent2.getSignalHandlerAddress()).thenReturn(SIGNAL_HANDLER_1);
        when(mockEvent2.isSignalEvent()).thenReturn(true);

        ReadonlyEventInterface[] events = new ReadonlyEventInterface[] {
                mockEvent1, mockEvent2, null, null};
        traceState.fastProcess(new RawTrace(
                0, 2, events,
                0, 1, true, false ));
        Assert.assertEquals(
                mockEvent2,
                traceState.getSignalNumberToHandlerToPreviousWindowEstablishEvent()
                        .get(SIGNAL_NUMBER_1).get(SIGNAL_HANDLER_1));
    }
}
