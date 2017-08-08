package com.runtimeverification.rvpredict.trace;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.testutils.TestUtils;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasSize;
import static com.runtimeverification.rvpredict.testutils.TraceUtils.extractEventByType;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TraceStateTest {
    private final static long THREAD_ID = 10L;
    private final static long THREAD_ID_2 = 11L;

    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 0;
    private static final long NO_ENABLED_SIGNAL_MASK = ~0L;
    private final static long SIGNAL_NUMBER_1 = 13L;

    private final static long SIGNAL_HANDLER_1 = 101L;

    private final static long PC_BASE = 201L;

    private static final long GENERATION = 301;

    @Mock
    private Configuration mockConfiguration;
    @Mock
    private MetadataInterface mockMetadata;
    @Mock
    private ReadonlyEventInterface mockEvent1;
    @Mock
    private ReadonlyEventInterface mockEvent2;
    @Mock
    private Context mockContext;

    @Before
    public void setUp() {
        mockConfiguration.windowSize = 10;
        when(mockEvent1.getEventId()).thenReturn(1L);
        when(mockEvent2.getEventId()).thenReturn(2L);
    }

    @Test
    public void remembersPreviousWindowEstablishEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1;

        List<RawTrace> rawTraces = ImmutableList.of(
                tu.createRawTrace(
                        e1 = tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, NO_ENABLED_SIGNAL_MASK)
                )
        );

        traceState.initNextTraceWindow(rawTraces);
        traceState.preStartWindow();

        Assert.assertEquals(
                extractEventByType(e1, EventType.ESTABLISH_SIGNAL),
                TestUtils.fromOptional(traceState.getPreviousWindowEstablishEvents(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1)));
    }

    @Test
    public void remembersTheLastPreviousWindowEstablishEvent() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces1 = ImmutableList.of(
                tu.createRawTrace(
                        e1 = tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, NO_ENABLED_SIGNAL_MASK)
                )
        );
        List<RawTrace> rawTraces2 = ImmutableList.of(
                tu.createRawTrace(
                        e2 = tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, NO_ENABLED_SIGNAL_MASK)
                )
        );

        traceState.initNextTraceWindow(rawTraces1);
        traceState.preStartWindow();
        Assert.assertEquals(
                extractEventByType(e1, EventType.ESTABLISH_SIGNAL),
                TestUtils.fromOptional(traceState.getPreviousWindowEstablishEvents(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1)));

        traceState.initNextTraceWindow(rawTraces2);
        traceState.preStartWindow();
        Assert.assertEquals(
                extractEventByType(e2, EventType.ESTABLISH_SIGNAL),
                TestUtils.fromOptional(traceState.getPreviousWindowEstablishEvents(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1)));

        traceState.initNextTraceWindow(rawTraces1);
        traceState.preStartWindow();
        Assert.assertEquals(
                extractEventByType(e2, EventType.ESTABLISH_SIGNAL),
                TestUtils.fromOptional(traceState.getPreviousWindowEstablishEvents(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1)));
    }

    @Test
    public void remembersTheLastPreviousWindowEstablishEventWhenFastProcessing() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1;

        RawTrace rawTrace = tu.createRawTrace(
                tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, NO_ENABLED_SIGNAL_MASK),
                e1 = tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, NO_ENABLED_SIGNAL_MASK)
        );

        traceState.fastProcess(rawTrace);
        traceState.preStartWindow();
        Assert.assertEquals(
                extractEventByType(e1, EventType.ESTABLISH_SIGNAL),
                TestUtils.fromOptional(traceState.getPreviousWindowEstablishEvents(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1)));
    }

    @Test
    public void processesEnterExitSignalEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        RawTrace rawTrace = tu.createRawTrace(
                tu.switchThread(THREAD_ID, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION),
                tu.exitSignal());
        traceState.fastProcess(rawTrace);

        Assert.assertThat(traceState.getThreadsForCurrentWindow(), hasSize(1));
        int ttid = traceState.getThreadsForCurrentWindow().iterator().next();
        Assert.assertTrue(traceState.getThreadStartsInTheCurrentWindow(ttid));
        Assert.assertTrue(traceState.getThreadEndsInTheCurrentWindow(ttid));
    }

    @Test
    public void processesStartJoinThreadEvents() throws InvalidTraceDataException {
        /*
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        RawTrace rawTrace = tu.createRawTrace(
                tu.threadStart(THREAD_ID_2),
                tu.threadJoin(THREAD_ID_2));
        traceState.fastProcess(rawTrace);

        Assert.assertThat(traceState.getThreadsForCurrentWindow(), hasSize(2));
        OptionalInt ttid = traceState.getThreadInfos().getTtidFromOtid(THREAD_ID_2);
        Assert.assertTrue(ttid.isPresent());
        Assert.assertTrue(traceState.getThreadStartsInTheCurrentWindow(ttid.getAsInt()));
        Assert.assertTrue(traceState.getThreadEndsInTheCurrentWindow(ttid.getAsInt()));
        */
    }

    @Test
    public void createsThreadInfo() {
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        ThreadInfo threadInfo = traceState.createAndRegisterThreadInfo(THREAD_ID, OptionalInt.empty());

        Assert.assertEquals(THREAD_ID, threadInfo.getOriginalThreadId());
        Assert.assertEquals(0, threadInfo.getSignalDepth());
        Assert.assertEquals(ThreadType.THREAD, threadInfo.getThreadType());
        Assert.assertFalse(threadInfo.getSignalNumber().isPresent());
        Assert.assertFalse(threadInfo.getSignalHandler().isPresent());

        Assert.assertTrue(traceState.getThreadStartsInTheCurrentWindow(threadInfo.getId()));
        Assert.assertFalse(traceState.getThreadEndsInTheCurrentWindow(threadInfo.getId()));

        Assert.assertNotNull(traceState.getThreadInfos().getThreadInfo(threadInfo.getId()));
    }

    @Test
    public void createsSignalInfo() {
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        ThreadInfo threadInfo = traceState.createAndRegisterSignalInfo(
                THREAD_ID, SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ONE_SIGNAL);

        Assert.assertEquals(THREAD_ID, threadInfo.getOriginalThreadId());
        Assert.assertEquals(ONE_SIGNAL, threadInfo.getSignalDepth());
        Assert.assertEquals(ThreadType.SIGNAL, threadInfo.getThreadType());
        OptionalLong signalNumber = threadInfo.getSignalNumber();
        Assert.assertTrue(signalNumber.isPresent());
        Assert.assertEquals(SIGNAL_NUMBER_1, signalNumber.getAsLong());
        OptionalLong signalHandler = threadInfo.getSignalHandler();
        Assert.assertTrue(signalHandler.isPresent());
        Assert.assertEquals(SIGNAL_HANDLER_1, signalHandler.getAsLong());

        Assert.assertTrue(traceState.getThreadStartsInTheCurrentWindow(threadInfo.getId()));
        Assert.assertFalse(traceState.getThreadEndsInTheCurrentWindow(threadInfo.getId()));

        Assert.assertNotNull(traceState.getThreadInfos().getThreadInfo(threadInfo.getId()));
    }

    @Test
    public void retrievesTtidsActiveAtWindowStart() throws InvalidTraceDataException {
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        ThreadInfo threadInfo = traceState.createAndRegisterThreadInfo(THREAD_ID, OptionalInt.empty());

        Assert.assertFalse(traceState.getTtidForThreadOngoingAtWindowStart(THREAD_ID, NO_SIGNAL).isPresent());

        traceState.preStartWindow();

        OptionalInt maybeTtid = traceState.getTtidForThreadOngoingAtWindowStart(THREAD_ID, NO_SIGNAL);
        Assert.assertTrue(maybeTtid.isPresent());
        Assert.assertEquals(threadInfo.getId(), maybeTtid.getAsInt());

        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        RawTrace rawTrace = tu.createRawTrace(
                tu.threadJoin(THREAD_ID));
        traceState.fastProcess(rawTrace);

        Assert.assertTrue(traceState.getTtidForThreadOngoingAtWindowStart(THREAD_ID, NO_SIGNAL).isPresent());

        traceState.preStartWindow();

        Assert.assertFalse(traceState.getTtidForThreadOngoingAtWindowStart(THREAD_ID, NO_SIGNAL).isPresent());
    }

    @Test
    public void threadStartPlaces() throws InvalidTraceDataException {
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        ThreadInfo threadInfo = traceState.createAndRegisterThreadInfo(THREAD_ID, OptionalInt.empty());

        Assert.assertTrue(traceState.getThreadStartsInTheCurrentWindow(threadInfo.getId()));
        Assert.assertFalse(traceState.getThreadEndsInTheCurrentWindow(threadInfo.getId()));

        traceState.preStartWindow();

        Assert.assertFalse(traceState.getThreadStartsInTheCurrentWindow(threadInfo.getId()));
        Assert.assertFalse(traceState.getThreadEndsInTheCurrentWindow(threadInfo.getId()));

        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        RawTrace rawTrace = tu.createRawTrace(
                tu.threadJoin(THREAD_ID));
        traceState.fastProcess(rawTrace);

        Assert.assertFalse(traceState.getThreadStartsInTheCurrentWindow(threadInfo.getId()));
        Assert.assertTrue(traceState.getThreadEndsInTheCurrentWindow(threadInfo.getId()));

        traceState.preStartWindow();

        Assert.assertFalse(traceState.getThreadStartsInTheCurrentWindow(threadInfo.getId()));
        Assert.assertFalse(traceState.getThreadEndsInTheCurrentWindow(threadInfo.getId()));
    }
}
