package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsExactly;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StateAtWindowBorderTest {
    private static final int NO_SIGNAL = 0;
    private static final long NO_SIGNAL_ENABLED_MASK = ~0L;
    private static final long SIGNAL_NUMBER_1 = 3;
    private static final long SIGNAL_NUMBER_2 = 4;
    private static final long SIGNAL_HANDLER_1 = 101;
    private static final long SIGNAL_HANDLER_2 = 102;
    private static final int THREAD_ID_1 = 201;
    private static final int THREAD_ID_2 = 202;
    private static final long EVENT_ID_1 = 301;
    private static final long EVENT_ID_2 = 302;
    private static final long PC_BASE = 400;
    private static final long BASE_ID = 500;
    private static final long ADDRESS_1 = 601;
    private static final long VALUE_1 = 701;

    private long nextIdDelta = 0;

    @Mock private Context mockContext;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);

    }

    @Test
    public void initialState() {
        StateAtWindowBorder state = new StateAtWindowBorder();
        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().isEmpty());
        Assert.assertFalse(state.threadWasStarted(THREAD_ID_1));
        Assert.assertFalse(state.threadEnded(THREAD_ID_1));
        Assert.assertTrue(state.getThreadsForCurrentWindow().isEmpty());
    }

    @Test
    public void startThread() {
        StateAtWindowBorder state = new StateAtWindowBorder();
        state.registerThread(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertThat(state.getUnfinishedTtids(), containsExactly(THREAD_ID_1));
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertFalse(state.threadEnded(THREAD_ID_1));
        Assert.assertThat(state.getThreadsForCurrentWindow(), containsExactly(THREAD_ID_1));
    }

    @Test
    public void joinThread() {
        StateAtWindowBorder state = new StateAtWindowBorder();
        state.joinThread(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().isEmpty());
        Assert.assertFalse(state.threadWasStarted(THREAD_ID_1));
        Assert.assertTrue(state.threadEnded(THREAD_ID_1));
        Assert.assertTrue(state.getThreadsForCurrentWindow().isEmpty());
    }

    @Test
    public void startJoinThread() {
        StateAtWindowBorder state = new StateAtWindowBorder();
        state.registerThread(THREAD_ID_1);
        state.joinThread(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().isEmpty());
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertTrue(state.threadEnded(THREAD_ID_1));
        Assert.assertThat(state.getThreadsForCurrentWindow(), containsExactly(THREAD_ID_1));
    }

    @Test
    public void joinStartThread() {
        StateAtWindowBorder state = new StateAtWindowBorder();
        state.joinThread(THREAD_ID_1);
        state.registerThread(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().isEmpty());
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertTrue(state.threadEnded(THREAD_ID_1));
        Assert.assertThat(state.getThreadsForCurrentWindow(), containsExactly(THREAD_ID_1));
    }

    @Test
    public void threadEvent() {
        StateAtWindowBorder state = new StateAtWindowBorder();
        state.threadEvent(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertThat(state.getUnfinishedTtids(), containsExactly(THREAD_ID_1));
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertFalse(state.threadEnded(THREAD_ID_1));
        Assert.assertThat(state.getThreadsForCurrentWindow(), containsExactly(THREAD_ID_1));
    }

    @Test
    public void threadEventThenJoin() {
        StateAtWindowBorder state = new StateAtWindowBorder();
        state.threadEvent(THREAD_ID_1);
        state.joinThread(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().isEmpty());
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertTrue(state.threadEnded(THREAD_ID_1));
        Assert.assertThat(state.getThreadsForCurrentWindow(), containsExactly(THREAD_ID_1));
    }

    @Test
    public void joinThenThreadEvent() {
        StateAtWindowBorder state = new StateAtWindowBorder();
        state.joinThread(THREAD_ID_1);
        state.threadEvent(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().isEmpty());
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertTrue(state.threadEnded(THREAD_ID_1));
        Assert.assertThat(state.getThreadsForCurrentWindow(), containsExactly(THREAD_ID_1));
    }

    @Test
    public void processEvent() throws InvalidTraceDataException {
        StateAtWindowBorder state = new StateAtWindowBorder();
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        ReadonlyEventInterface e1 = TraceUtils.extractSingleEvent(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        ReadonlyEventInterface e2 = TraceUtils.extractSingleEvent(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        Assert.assertTrue(e1.getEventId() < e2.getEventId());

        state.processEvent(e2, THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().isEmpty());
        Assert.assertFalse(state.threadWasStarted(THREAD_ID_1));
        Assert.assertFalse(state.threadEnded(THREAD_ID_1));
        Assert.assertTrue(state.getThreadsForCurrentWindow().isEmpty());
        Assert.assertEquals(e2.getEventId(), state.getMinEventIdForWindow());

        state.processEvent(e1, THREAD_ID_1);
        Assert.assertEquals(e1.getEventId(), state.getMinEventIdForWindow());

        state.processEvent(e2, THREAD_ID_1);
        Assert.assertEquals(e1.getEventId(), state.getMinEventIdForWindow());
    }

    @Test
    public void registerThread() {
        StateAtWindowBorder state = new StateAtWindowBorder();
        state.registerThread(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().contains(THREAD_ID_1));
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertFalse(state.threadEnded(THREAD_ID_1));
        Assert.assertTrue(state.getThreadsForCurrentWindow().contains(THREAD_ID_1));
    }

    @Test
    public void registerSignal() {
        StateAtWindowBorder state = new StateAtWindowBorder();
        state.registerSignal(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().contains(THREAD_ID_1));
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertFalse(state.threadEnded(THREAD_ID_1));
        Assert.assertTrue(state.getThreadsForCurrentWindow().contains(THREAD_ID_1));
    }

    @Test
    public void establishSignal() throws InvalidTraceDataException {
        StateAtWindowBorder state = new StateAtWindowBorder();
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        ReadonlyEventInterface e1;

        state.processEvent(e1 = TraceUtils.extractEventByType(
                tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, NO_SIGNAL_ENABLED_MASK),
                EventType.ESTABLISH_SIGNAL),
                THREAD_ID_1);

        Optional<ReadonlyEventInterface> lastEstablish = state.getLastEstablishEvent(SIGNAL_NUMBER_1);
        Assert.assertTrue(lastEstablish.isPresent());
        Assert.assertEquals(SIGNAL_HANDLER_1, lastEstablish.get().getSignalHandlerAddress());
        lastEstablish = state.getLastEstablishEvent(SIGNAL_NUMBER_2);
        Assert.assertFalse(lastEstablish.isPresent());

        state.processEvent(TraceUtils.extractEventByType(
                tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_2, NO_SIGNAL_ENABLED_MASK),
                EventType.ESTABLISH_SIGNAL), THREAD_ID_1);

        lastEstablish = state.getLastEstablishEvent(SIGNAL_NUMBER_1);
        Assert.assertTrue(lastEstablish.isPresent());
        Assert.assertEquals(SIGNAL_HANDLER_2, lastEstablish.get().getSignalHandlerAddress());
        lastEstablish = state.getLastEstablishEvent(SIGNAL_NUMBER_2);
        Assert.assertFalse(lastEstablish.isPresent());

        state.processEvent(e1, THREAD_ID_1);

        lastEstablish = state.getLastEstablishEvent(SIGNAL_NUMBER_1);
        Assert.assertTrue(lastEstablish.isPresent());
        Assert.assertEquals(SIGNAL_HANDLER_2, lastEstablish.get().getSignalHandlerAddress());
        lastEstablish = state.getLastEstablishEvent(SIGNAL_NUMBER_2);
        Assert.assertFalse(lastEstablish.isPresent());

        state.processEvent(TraceUtils.extractEventByType(
                tu.setSignalHandler(SIGNAL_NUMBER_2, SIGNAL_HANDLER_1, NO_SIGNAL_ENABLED_MASK),
                EventType.ESTABLISH_SIGNAL), THREAD_ID_1);

        lastEstablish = state.getLastEstablishEvent(SIGNAL_NUMBER_1);
        Assert.assertTrue(lastEstablish.isPresent());
        Assert.assertEquals(SIGNAL_HANDLER_2, lastEstablish.get().getSignalHandlerAddress());
        lastEstablish = state.getLastEstablishEvent(SIGNAL_NUMBER_2);
        Assert.assertTrue(lastEstablish.isPresent());
        Assert.assertEquals(SIGNAL_HANDLER_1, lastEstablish.get().getSignalHandlerAddress());
    }

    @Test
    public void initializeFromPreviousWindow() throws InvalidTraceDataException {
        /*
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        ReadonlyEventInterface e1 = TraceUtils.extractSingleEvent(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        ReadonlyEventInterface e2 = TraceUtils.extractSingleEvent(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        Assert.assertTrue(e1.getEventId() < e2.getEventId());

        StateAtWindowBorder state = new StateAtWindowBorder();
        state.registerThread(THREAD_ID_1);
        state.registerThread(THREAD_ID_2);
        state.joinThread(THREAD_ID_1);
        state.processEvent(TraceUtils.extractEventByType(
                tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_2, NO_SIGNAL_ENABLED_MASK),
                EventType.ESTABLISH_SIGNAL),
                THREAD_ID_1);
        state.processEvent(e1, THREAD_ID_1);

        Optional<ReadonlyEventInterface> lastEstablish = state.getLastEstablishEvent(SIGNAL_NUMBER_1);
        Assert.assertTrue(lastEstablish.isPresent());
        Assert.assertEquals(SIGNAL_HANDLER_2, lastEstablish.get().getSignalHandlerAddress());
        Assert.assertThat(state.getUnfinishedTtids(), containsExactly(THREAD_ID_2));
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_2));
        Assert.assertTrue(state.threadEnded(THREAD_ID_1));
        Assert.assertFalse(state.threadEnded(THREAD_ID_2));
        Assert.assertThat(state.getThreadsForCurrentWindow(), containsExactly(THREAD_ID_1, THREAD_ID_2));
        Assert.assertEquals(e1.getEventId(), state.getMinEventIdForWindow());

        StateAtWindowBorder nextWindowState = new StateAtWindowBorder();
        nextWindowState.copyFrom(state);

        lastEstablish = nextWindowState.getLastEstablishEvent(SIGNAL_NUMBER_1);
        Assert.assertTrue(lastEstablish.isPresent());
        Assert.assertEquals(SIGNAL_HANDLER_2, lastEstablish.get().getSignalHandlerAddress());
        Assert.assertThat(nextWindowState.getUnfinishedTtids(), containsExactly(THREAD_ID_2));
        Assert.assertTrue(nextWindowState.threadWasStarted(THREAD_ID_1));
        Assert.assertTrue(nextWindowState.threadWasStarted(THREAD_ID_2));
        Assert.assertTrue(nextWindowState.threadEnded(THREAD_ID_1));
        Assert.assertFalse(nextWindowState.threadEnded(THREAD_ID_2));
        Assert.assertThat(nextWindowState.getThreadsForCurrentWindow(), containsExactly(THREAD_ID_1, THREAD_ID_2));
        Assert.assertEquals(e1.getEventId(), nextWindowState.getMinEventIdForWindow());

        nextWindowState.processEvent(e2, THREAD_ID_1);
        Assert.assertEquals(e1.getEventId(), nextWindowState.getMinEventIdForWindow());

        nextWindowState.initializeForNextWindow();
        nextWindowState.processEvent(e2, THREAD_ID_1);

        lastEstablish = nextWindowState.getLastEstablishEvent(SIGNAL_NUMBER_1);
        Assert.assertTrue(lastEstablish.isPresent());
        Assert.assertEquals(SIGNAL_HANDLER_2, lastEstablish.get().getSignalHandlerAddress());
        Assert.assertThat(nextWindowState.getUnfinishedTtids(), containsExactly(THREAD_ID_2));
        Assert.assertTrue(nextWindowState.threadWasStarted(THREAD_ID_1));
        Assert.assertTrue(nextWindowState.threadWasStarted(THREAD_ID_2));
        Assert.assertTrue(nextWindowState.threadEnded(THREAD_ID_1));
        Assert.assertFalse(nextWindowState.threadEnded(THREAD_ID_2));
        Assert.assertThat(nextWindowState.getThreadsForCurrentWindow(), containsExactly(THREAD_ID_2));
        Assert.assertEquals(e2.getEventId(), nextWindowState.getMinEventIdForWindow());
        */
    }
}
