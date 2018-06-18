package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;
import java.util.OptionalLong;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.assertException;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsExactly;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasMapSize;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasSize;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmpty;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmptyMap;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StateAtWindowBorderTest {
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final long NO_SIGNAL_ENABLED_MASK = ~0L;
    private static final long SIGNAL_NUMBER_1 = 3;
    private static final long SIGNAL_NUMBER_2 = 4;
    private static final long SIGNAL_HANDLER_1 = 101;
    private static final long SIGNAL_HANDLER_2 = 102;
    private static final int THREAD_ID_1 = 201;
    private static final int THREAD_ID_2 = 202;
    private static final long PC_BASE = 400;
    private static final long BASE_ID = 500;
    private static final long ADDRESS_1 = 601;
    private static final long VALUE_1 = 701;
    private static final long GENERATION_1 = 801;
    private static final long CANONICAL_FRAME_ADDRESS_1 = 901;
    private static final OptionalLong CALL_SITE_1 = OptionalLong.of(1001);
    private static final long LOCATION_ID_1 = 1110;
    private static final long LOCATION_ID_2 = 1120;

    private long nextIdDelta = 0;

    @Mock private Context mockContext;
    @Mock private MetadataInterface mockMetadata;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);

    }

    @Test
    public void initialState() {
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().isEmpty());
        Assert.assertFalse(state.threadWasStarted(THREAD_ID_1));
        Assert.assertFalse(state.threadEnded(THREAD_ID_1));
        Assert.assertTrue(state.getThreadsForCurrentWindow().isEmpty());
    }

    @Test
    public void startThread() {
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
        state.registerThread(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertThat(state.getUnfinishedTtids(), containsExactly(THREAD_ID_1));
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertFalse(state.threadEnded(THREAD_ID_1));
        Assert.assertThat(state.getThreadsForCurrentWindow(), containsExactly(THREAD_ID_1));
    }

    @Test
    public void joinThread() {
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
        state.joinThread(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().isEmpty());
        Assert.assertFalse(state.threadWasStarted(THREAD_ID_1));
        Assert.assertTrue(state.threadEnded(THREAD_ID_1));
        Assert.assertTrue(state.getThreadsForCurrentWindow().isEmpty());
    }

    @Test
    public void startJoinThread() {
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
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
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
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
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
        state.threadEvent(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertThat(state.getUnfinishedTtids(), containsExactly(THREAD_ID_1));
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertFalse(state.threadEnded(THREAD_ID_1));
        Assert.assertThat(state.getThreadsForCurrentWindow(), containsExactly(THREAD_ID_1));
    }

    @Test
    public void threadEventThenJoin() {
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
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
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
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
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
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
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
        state.registerThread(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().contains(THREAD_ID_1));
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertFalse(state.threadEnded(THREAD_ID_1));
        Assert.assertTrue(state.getThreadsForCurrentWindow().contains(THREAD_ID_1));
    }

    @Test
    public void registerSignal() {
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
        state.registerSignal(THREAD_ID_1);

        Assert.assertFalse(state.getLastEstablishEvent(SIGNAL_NUMBER_1).isPresent());
        Assert.assertTrue(state.getUnfinishedTtids().contains(THREAD_ID_1));
        Assert.assertTrue(state.threadWasStarted(THREAD_ID_1));
        Assert.assertFalse(state.threadEnded(THREAD_ID_1));
        Assert.assertTrue(state.getThreadsForCurrentWindow().contains(THREAD_ID_1));
    }

    @Test
    public void establishSignal() throws InvalidTraceDataException {
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
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
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        ReadonlyEventInterface e1 = TraceUtils.extractSingleEvent(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        ReadonlyEventInterface e2 = TraceUtils.extractSingleEvent(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        Assert.assertTrue(e1.getEventId() < e2.getEventId());

        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
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

        StateAtWindowBorder nextWindowState = new StateAtWindowBorder(0, mockMetadata);
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
        // Already joined in the previous window, so we forgot it.
        Assert.assertFalse(nextWindowState.threadWasStarted(THREAD_ID_1));
        Assert.assertTrue(nextWindowState.threadWasStarted(THREAD_ID_2));
        // Already joined in the previous window, so we forgot it.
        Assert.assertFalse(nextWindowState.threadEnded(THREAD_ID_1));
        Assert.assertFalse(nextWindowState.threadEnded(THREAD_ID_2));
        Assert.assertThat(nextWindowState.getThreadsForCurrentWindow(), containsExactly(THREAD_ID_2));
        Assert.assertEquals(e2.getEventId(), nextWindowState.getMinEventIdForWindow());
    }

    @Test
    public void noFormerSignalTracesWhenNotAdded() {
        StateAtWindowBorder state = new StateAtWindowBorder(1, mockMetadata);

        Assert.assertThat(state.getFormerSignalTraces(), isEmpty());
    }

    @Test
    public void formerSignalTracesWhenAdded() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, ONE_SIGNAL, PC_BASE);
        StateAtWindowBorder state = new StateAtWindowBorder(1, mockMetadata);

        RawTrace trace = tu.createRawTrace(
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal());

        state.onSignalThread(trace);

        Assert.assertThat(state.getFormerSignalTraces(), hasSize(1));
    }

    @Test
    public void formerSignalTracesDroppedWhenOverLimit() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, ONE_SIGNAL, PC_BASE);
        StateAtWindowBorder state = new StateAtWindowBorder(2, mockMetadata);

        state.onSignalThread(tu.createRawTrace(
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal()));
        Assert.assertThat(state.getFormerSignalTraces(), hasSize(1));

        state.onSignalThread(tu.createRawTrace(
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal()));
        Assert.assertThat(state.getFormerSignalTraces(), hasSize(2));

        state.onSignalThread(tu.createRawTrace(
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal()));
        Assert.assertThat(state.getFormerSignalTraces(), hasSize(2));

        state.onSignalThread(tu.createRawTrace(
                tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal()));
        Assert.assertThat(state.getFormerSignalTraces(), hasSize(3));

        state.onSignalThread(tu.createRawTrace(
                tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal()));
        Assert.assertThat(state.getFormerSignalTraces(), hasSize(4));

        state.onSignalThread(tu.createRawTrace(
                tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal()));
        Assert.assertThat(state.getFormerSignalTraces(), hasSize(4));
    }

    @Test
    public void keepsTrackOfStackTraces() throws InvalidTraceDataException {
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        Assert.assertThat(state.getStackTraces(), isEmptyMap());

        state.processEvent(TraceUtils.extractSingleEvent(tu.nonAtomicStore(ADDRESS_1, VALUE_1)), THREAD_ID_1);
        Assert.assertThat(state.getStackTraces(), isEmptyMap());

        tu.setPc(LOCATION_ID_1);
        ReadonlyEventInterface e1 = TraceUtils.extractSingleEvent(
                tu.enterFunction(CANONICAL_FRAME_ADDRESS_1, CALL_SITE_1));
        state.processEvent(e1, THREAD_ID_1);
        Assert.assertThat(state.getStackTraces(), hasMapSize(1));
        Assert.assertThat(state.getStackTraces().get(THREAD_ID_1), containsExactly(e1));

        state.processEvent(TraceUtils.extractSingleEvent(tu.nonAtomicStore(ADDRESS_1, VALUE_1)), THREAD_ID_1);
        Assert.assertThat(state.getStackTraces(), hasMapSize(1));
        Assert.assertThat(state.getStackTraces().get(THREAD_ID_1), containsExactly(e1));

        tu.setPc(LOCATION_ID_2);
        ReadonlyEventInterface e2 = TraceUtils.extractSingleEvent(
                tu.enterFunction(CANONICAL_FRAME_ADDRESS_1, CALL_SITE_1));
        state.processEvent(e2, THREAD_ID_1);
        Assert.assertThat(state.getStackTraces(), hasMapSize(1));
        Assert.assertThat(state.getStackTraces().get(THREAD_ID_1), containsExactly(e1, e2));

        state.processEvent(TraceUtils.extractSingleEvent(tu.nonAtomicStore(ADDRESS_1, VALUE_1)), THREAD_ID_1);
        Assert.assertThat(state.getStackTraces(), hasMapSize(1));
        Assert.assertThat(state.getStackTraces().get(THREAD_ID_1), containsExactly(e1, e2));

        tu.setPc(LOCATION_ID_2);
        state.processEvent(TraceUtils.extractSingleEvent(tu.exitFunction()), THREAD_ID_1);
        Assert.assertThat(state.getStackTraces(), hasMapSize(1));
        Assert.assertThat(state.getStackTraces().get(THREAD_ID_1), containsExactly(e1));

        tu.setPc(LOCATION_ID_1);
        state.processEvent(TraceUtils.extractSingleEvent(tu.exitFunction()), THREAD_ID_1);
        Assert.assertThat(state.getStackTraces(), hasMapSize(1));
        Assert.assertThat(state.getStackTraces().get(THREAD_ID_1), isEmpty());
    }

    @Test
    public void crashesForUnmatchedStackEvents() {
        StateAtWindowBorder state = new StateAtWindowBorder(0, mockMetadata);
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);

        tu.setPc(LOCATION_ID_1);
        state.processEvent(
                TraceUtils.extractSingleEvent(tu.enterFunction(CANONICAL_FRAME_ADDRESS_1, CALL_SITE_1)),
                THREAD_ID_1);

        tu.setPc(LOCATION_ID_2);
        assertException(
                IllegalStateException.class,
                () -> state.processEvent(TraceUtils.extractSingleEvent(tu.exitFunction()), THREAD_ID_1));
    }
}
