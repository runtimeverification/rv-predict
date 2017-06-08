package com.runtimeverification.rvpredict.smt;

import com.microsoft.z3.Params;
import com.microsoft.z3.Solver;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.main.RaceDetector;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.smt.visitors.Z3Filter;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.trace.TraceState;
import com.runtimeverification.rvpredict.violation.Race;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.runtimeverification.rvpredict.testutils.TraceUtils.extractSingleEvent;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MaximalCausalModelTest {
    private static final int WINDOW_SIZE = 100;
    private static final int TIMEOUT_MILLIS = 2000;
    private static final long ADDRESS_1 = 200;
    private static final long ADDRESS_2 = 201;
    private static final long ADDRESS_3 = 202;
    private static final long VALUE_1 = 300;
    private static final long VALUE_2 = 301;
    private static final long BASE_ID = 0;
    private static final long BASE_PC = 400;
    private static final long THREAD_1 = 1;
    private static final long THREAD_2 = 2;
    private static final long THREAD_3 = 3;
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final int TWO_SIGNALS = 2;
    private static final long LOCK_1 = 500;
    private static final long SIGNAL_NUMBER_1 = 1;
    private static final long SIGNAL_NUMBER_2 = 2;
    private static final long SIGNAL_HANDLER_1 = 600;
    private static final long SIGNAL_HANDLER_2 = 601;
    private static final long ALL_SIGNALS_DISABLED_MASK = 0xffffffffffffffffL;
    private static final long SIGNAL_1_ENABLED_MASK = ~(1 << SIGNAL_NUMBER_1);
    private static final long SIGNAL_2_ENABLED_MASK = ~(1 << SIGNAL_NUMBER_2);
    private static final long GENERATION_1 = 700;

    private int nextIdDelta = 0;

    @Mock private Configuration mockConfiguration;
    @Mock private Context mockContext;
    @Mock private Metadata mockMetadata;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
        when(mockContext.createUniqueSignalHandlerId(SIGNAL_NUMBER_1)).thenReturn(1L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_1)).thenReturn(2L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_2)).thenReturn(3L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_3)).thenReturn(4L);
    }

    @Test
    public void detectsSimpleRace() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotDetectRaceBeforeThreadStart() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.threadStart(THREAD_2),
                        tu.threadJoin(THREAD_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }
    @Test
    public void doesNotDetectRaceAfterThreadJoin() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.threadStart(THREAD_2),
                        tu.threadJoin(THREAD_2),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void detectsRaceBetweenThreadStartAndJoin() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.threadStart(THREAD_2),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.threadJoin(THREAD_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotDetectRaceWhenLocked() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.lock(LOCK_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.unlock(LOCK_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.lock(LOCK_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.unlock(LOCK_1)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void detectsRaceWhenOnlyOneThreadIsLocked() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.lock(LOCK_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.unlock(LOCK_1)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void noRaceBecauseOfRestrictsOnDifferentThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_3, NO_SIGNAL),
                e1 = tu.nonAtomicStore(ADDRESS_3, VALUE_1),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.lock(LOCK_1),
                tu.nonAtomicLoad(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_2),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.lock(LOCK_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_2),
                tu.unlock(LOCK_1),
                e2 = tu.nonAtomicStore(ADDRESS_3, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_3, NO_SIGNAL));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void detectsRaceWithSignalOnDifferentThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.setSignalHandler(
                                SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void detectsRaceWithSignalOnSameThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setSignalHandler(
                                SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertTrue(hasRace(
                rawTraces, extractSingleEvent(e1), extractSingleEvent(e2), true));
    }

    @Test
    public void doesNotDetectRaceBeforeEnablingSignals() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setSignalHandler(
                                SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.nonAtomicLoad(ADDRESS_2, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotDetectRaceWithDifferentHandlerAddress() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                // Disable the racy instruction on thread 2.
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.setSignalHandler(
                        SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                // Enable the signal
                tu.enableSignal(SIGNAL_NUMBER_1),

                // Run the signal
                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                // Disable the signal and set a different signal handler, then enable it again.
                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.disableSignal(SIGNAL_NUMBER_1),
                tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK),
                tu.enableSignal(SIGNAL_NUMBER_1),
                // Enable the racy instruction on thread 2.
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.lock(LOCK_1),
                tu.nonAtomicLoad(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),
                e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL));


        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void detectsRaceWithTheSameHandlerAddress() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                // Disable the racy instruction on thread 2.
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                // Enable the signal
                tu.enableSignal(SIGNAL_NUMBER_1),

                // Run the signal
                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                // Disable the signal and set a different signal handler, then enable it again.
                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.disableSignal(SIGNAL_NUMBER_1),
                tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK),
                tu.enableSignal(SIGNAL_NUMBER_1),
                // Enable the racy instruction on thread 2.
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),
                // Disable the racy instruction on thread 2.
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                // Disable the signal and set the racy signal handler, then enable it again.
                tu.disableSignal(SIGNAL_NUMBER_1),
                tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                tu.enableSignal(SIGNAL_NUMBER_1),
                // Enable the racy instruction on thread 2.
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),
                // Disable the racy instruction on thread 2.
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.lock(LOCK_1),
                tu.nonAtomicLoad(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),
                e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL));


        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotDetectRaceWhenDisabledAndInterruptsThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                tu.enableSignal(SIGNAL_NUMBER_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.disableSignal(SIGNAL_NUMBER_1),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.lock(LOCK_1),
                tu.nonAtomicLoad(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),
                e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL));


        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void detectsRaceBetweenEnableAndDisable() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.enableSignal(SIGNAL_NUMBER_1),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.disableSignal(SIGNAL_NUMBER_1),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.lock(LOCK_1),
                tu.nonAtomicLoad(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),
                e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void movesSignal() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.enableSignal(SIGNAL_NUMBER_1),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.disableSignal(SIGNAL_NUMBER_1),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.lock(LOCK_1),
                tu.nonAtomicLoad(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),
                e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotDetectRaceBeforeEnabling() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.setSignalHandler(
                        SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.enableSignal(SIGNAL_NUMBER_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.disableSignal(SIGNAL_NUMBER_1),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.unlock(LOCK_1),
                tu.lock(LOCK_1),
                tu.nonAtomicLoad(ADDRESS_2, VALUE_1),
                tu.unlock(LOCK_1),
                e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotGenerateRacesByInterruptingThreadBeforeBeingStarted() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        tu.threadStart(THREAD_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.nonAtomicLoad(ADDRESS_2, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotGenerateRacesByInterruptingThreadAfterBeingJoined() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        tu.threadStart(THREAD_2),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.threadJoin(THREAD_2),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.nonAtomicLoad(ADDRESS_2, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void signalCanGenerateRacesEvenIfFullyDisabledOnAnotherThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.threadStart(THREAD_3),
                        tu.threadJoin(THREAD_3),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        tu.threadStart(THREAD_2),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.threadJoin(THREAD_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.nonAtomicLoad(ADDRESS_2, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_3, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void signalsDoNotGenerateRacesAfterDisestablishingThem() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                        tu.setSignalHandler(
                                SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        tu.nonAtomicStore(ADDRESS_3, VALUE_2),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.threadStart(THREAD_2),
                        tu.lock(LOCK_1),
                        tu.nonAtomicLoad(ADDRESS_3, VALUE_1),
                        tu.unlock(LOCK_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        tu.disestablishSignal(SIGNAL_NUMBER_1),
                        tu.lock(LOCK_1),
                        tu.nonAtomicStore(ADDRESS_3, VALUE_1),
                        tu.unlock(LOCK_1),
                        tu.nonAtomicLoad(ADDRESS_2, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void getSignalMaskMayShowThatASignalIsEnabled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.getSignalMask(SIGNAL_1_ENABLED_MASK),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertTrue(
                hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2), true));
    }

    @Test
    public void enableSignalStopsFromDetectingRacesBeforeEnabling() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertFalse(
                hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2), true));
    }

    @Test
    public void getSetSignalMaskMayShowThatASignalIsEnabledBefore() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.getSetSignalMask(SIGNAL_1_ENABLED_MASK, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertTrue(
                hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2), true));
    }

    @Test
    public void setSignalMaskEnablesSignals() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setSignalMask(SIGNAL_1_ENABLED_MASK),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertTrue(
                hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2), true));
    }

    public void recurrentSignalFix() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setSignalMask(SIGNAL_1_ENABLED_MASK),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        Assert.assertTrue(
                hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2), true));
    }

    @Test
    public void detectedSignalRaceContainsInterruptedEventWhenOnSameThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Optional<Race> maybeRace =
                findRace(rawTraces, event1, event2);
        Assert.assertTrue(maybeRace.isPresent());
        Race race = maybeRace.get();
        List<Race.SignalStackEvent> signalEvents = race.getFirstSignalStack();
        Assert.assertEquals(1, signalEvents.size());
        Race.SignalStackEvent stackEvent = signalEvents.get(0);
        Optional<ReadonlyEventInterface> maybeEvent = stackEvent.getEvent();
        Assert.assertTrue(maybeEvent.isPresent());
        Assert.assertEquals(event1.getEventId(), maybeEvent.get().getEventId());
        Assert.assertEquals(1, stackEvent.getTtid());
        signalEvents = race.getSecondSignalStack();
        Assert.assertEquals(2, signalEvents.size());
        stackEvent = signalEvents.get(0);
        maybeEvent = stackEvent.getEvent();
        Assert.assertTrue(maybeEvent.isPresent());
        Assert.assertEquals(event2.getEventId(), maybeEvent.get().getEventId());
        Assert.assertEquals(2, stackEvent.getTtid());
        stackEvent = signalEvents.get(1);
        maybeEvent = stackEvent.getEvent();
        Assert.assertTrue(maybeEvent.isPresent());
        Assert.assertEquals(event1.getEventId(), maybeEvent.get().getEventId());
        Assert.assertEquals(1, stackEvent.getTtid());
    }

    @Test
    public void detectedSignalRaceContainsInterruptedEventWhenOnDifferentThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.lock(LOCK_1),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_2),
                        tu.unlock(LOCK_1),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        tu.threadStart(THREAD_2),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.lock(LOCK_1),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.unlock(LOCK_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Optional<Race> maybeRace =
                findRace(rawTraces, event1, event2);
        Assert.assertTrue(maybeRace.isPresent());
        Race race = maybeRace.get();
        List<Race.SignalStackEvent> signalEvents = race.getFirstSignalStack();
        Assert.assertEquals(1, signalEvents.size());
        Race.SignalStackEvent stackEvent = signalEvents.get(0);
        Optional<ReadonlyEventInterface> maybeEvent = stackEvent.getEvent();
        Assert.assertTrue(maybeEvent.isPresent());
        Assert.assertEquals(event1.getEventId(), maybeEvent.get().getEventId());
        Assert.assertEquals(1, stackEvent.getTtid());

        signalEvents = race.getSecondSignalStack();
        Assert.assertEquals(2, signalEvents.size());
        stackEvent = signalEvents.get(0);
        maybeEvent = stackEvent.getEvent();
        Assert.assertTrue(maybeEvent.isPresent());
        Assert.assertEquals(event2.getEventId(), maybeEvent.get().getEventId());
        Assert.assertEquals(3, stackEvent.getTtid());
        stackEvent = signalEvents.get(1);
        Assert.assertEquals(2, stackEvent.getTtid());
    }

    @Test
    public void stackTraceBeforeTheFirstEventOfAThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        tu.threadStart(THREAD_2),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.disableSignal(SIGNAL_NUMBER_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Optional<Race> maybeRace =
                findRace(rawTraces, event1, event2);
        Assert.assertTrue(maybeRace.isPresent());
        Race race = maybeRace.get();
        List<Race.SignalStackEvent> signalEvents = race.getFirstSignalStack();
        Assert.assertEquals(1, signalEvents.size());
        Race.SignalStackEvent stackEvent = signalEvents.get(0);
        Optional<ReadonlyEventInterface> maybeEvent = stackEvent.getEvent();
        Assert.assertTrue(maybeEvent.isPresent());
        Assert.assertEquals(event1.getEventId(), maybeEvent.get().getEventId());
        Assert.assertEquals(1, stackEvent.getTtid());

        signalEvents = race.getSecondSignalStack();
        Assert.assertEquals(2, signalEvents.size());
        stackEvent = signalEvents.get(0);
        maybeEvent = stackEvent.getEvent();
        Assert.assertTrue(maybeEvent.isPresent());
        Assert.assertEquals(event2.getEventId(), maybeEvent.get().getEventId());
        Assert.assertEquals(3, stackEvent.getTtid());
        stackEvent = signalEvents.get(1);
        Assert.assertFalse(stackEvent.getEvent().isPresent());
        Assert.assertEquals(2, stackEvent.getTtid());
    }

    @Test
    public void eventIdsDoNotCollide() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(1L).thenReturn(2L).thenReturn(3L).thenReturn(4L)
                .thenReturn(1L + WINDOW_SIZE).thenReturn(2L + WINDOW_SIZE).thenReturn(3L + WINDOW_SIZE)
                .thenReturn(4L + WINDOW_SIZE);
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.lock(LOCK_1),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.unlock(LOCK_1),
                        e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.lock(LOCK_1),
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.unlock(LOCK_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);

        Assert.assertTrue(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void interruptedThreadRacesWithSignalMovedToInterruptSignal() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.disableSignal(SIGNAL_NUMBER_2),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        tu.setSignalHandler(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.enableSignal(SIGNAL_NUMBER_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.enableSignal(SIGNAL_NUMBER_2),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertTrue(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalRacesWithSignalMovedToInterruptIt() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.disableSignal(SIGNAL_NUMBER_1),
                tu.disableSignal(SIGNAL_NUMBER_2),
                tu.enableSignal(SIGNAL_NUMBER_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),

                tu.switchThread(THREAD_1, TWO_SIGNALS),
                tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal(),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.exitSignal(),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                tu.disableSignal(SIGNAL_NUMBER_1),
                tu.enableSignal(SIGNAL_NUMBER_2));
        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL),
                tu.extractRawTrace(events, THREAD_1, TWO_SIGNALS));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertTrue(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalsDisabledByAtomicEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        tu.atomicStore(ADDRESS_1, VALUE_2),
                        e1 = tu.nonAtomicStore(ADDRESS_2, VALUE_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.atomicLoad(ADDRESS_1, VALUE_1),
                        e2 = tu.nonAtomicStore(ADDRESS_2, VALUE_2)));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);

        Assert.assertFalse(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void raceWithSignalThatInterruptsSignalExplicitSigset() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.disableSignal(SIGNAL_NUMBER_1),
                tu.disableSignal(SIGNAL_NUMBER_2),
                tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, SIGNAL_2_ENABLED_MASK),
                tu.enableSignal(SIGNAL_NUMBER_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),

                tu.switchThread(THREAD_1, TWO_SIGNALS),
                tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal(),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.exitSignal(),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                tu.disableSignal(SIGNAL_NUMBER_1),
                tu.enableSignal(SIGNAL_NUMBER_2));
        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL),
                tu.extractRawTrace(events, THREAD_1, TWO_SIGNALS));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertTrue(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalEndsBeforeInterruptedSignal() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.disableSignal(SIGNAL_NUMBER_2),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        tu.setSignalHandler(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.enableSignal(SIGNAL_NUMBER_2),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertFalse(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalInterruptsSignalWhenAllowedByTheHandlerMask() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.disableSignal(SIGNAL_NUMBER_2),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, SIGNAL_2_ENABLED_MASK),
                        tu.setSignalHandler(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.enableSignal(SIGNAL_NUMBER_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertTrue(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalsInterruptingTheSameThreadCannotOverlap() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertFalse(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalsInterruptingDifferentThreadsCanOverlap() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_1)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertTrue(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalsThatInterruptBetweenTwoSigsetEventsUseTheMaskFromTheFirst() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.disableSignal(SIGNAL_NUMBER_2),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, SIGNAL_2_ENABLED_MASK),
                        tu.setSignalHandler(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertTrue(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalDoesNotInterruptSignalWhenImplicitlyDisabled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.disableSignal(SIGNAL_NUMBER_2),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, SIGNAL_2_ENABLED_MASK),
                        tu.setSignalHandler(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.enableSignal(SIGNAL_NUMBER_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.getSignalMask(ALL_SIGNALS_DISABLED_MASK),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertFalse(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalIsNotEnabledByHandlerMaskWithDifferentHandler() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.disableSignal(SIGNAL_NUMBER_2),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_2, SIGNAL_2_ENABLED_MASK),
                        tu.setSignalHandler(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_2, GENERATION_1),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                        tu.nonAtomicLoad(ADDRESS_2, VALUE_2),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertFalse(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalDoesNotInterruptSignalWhenDisabledByTheHandlerMask() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.disableSignal(SIGNAL_NUMBER_2),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        tu.setSignalHandler(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.enableSignal(SIGNAL_NUMBER_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertFalse(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalsEnabledOnThreadStartedBySignalIfEnabledExplicitlyBySignal() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.disableSignal(SIGNAL_NUMBER_2),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK),
                        tu.setSignalHandler(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.enableSignal(SIGNAL_NUMBER_2),
                        tu.threadStart(THREAD_2),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_2)
                ));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertTrue(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalsDisabledOnThreadStartedBySignalIfDisabledExplicitlyBySignal() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        tu.disableSignal(SIGNAL_NUMBER_2),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                        tu.setSignalHandler(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, SIGNAL_2_ENABLED_MASK),
                        tu.setSignalHandler(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK),
                        tu.enableSignal(SIGNAL_NUMBER_1),
                        tu.disableSignal(SIGNAL_NUMBER_1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                        tu.enableSignal(SIGNAL_NUMBER_2)),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        tu.disableSignal(SIGNAL_NUMBER_2),
                        tu.threadStart(THREAD_2),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                        tu.exitSignal()),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.nonAtomicStore(ADDRESS_2, VALUE_2)
                ));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertFalse(hasRace(rawTraces, event1, event2, true));
    }

    @Test
    public void signalsEnabledOnThreadStartedBySignalIfEnabledImplicitlyBySignal() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.disableSignal(SIGNAL_NUMBER_1),
                tu.disableSignal(SIGNAL_NUMBER_2),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),
                tu.setSignalHandler(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK),
                tu.enableSignal(SIGNAL_NUMBER_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),

                tu.switchThread(THREAD_1, TWO_SIGNALS),
                tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal(),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.threadStart(THREAD_2),
                tu.exitSignal(),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_2, VALUE_2),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1));

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL),
                tu.extractRawTrace(events, THREAD_1, TWO_SIGNALS),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL));

        ReadonlyEventInterface event1 = extractSingleEvent(e1);
        ReadonlyEventInterface event2 = extractSingleEvent(e2);
        Assert.assertTrue(hasRace(rawTraces, event1, event2, true));
    }

    // TODO: Tests with writes that enable certain reads, both with and without signals.
    // TODO: Test that a signals stops their thread, i.e. it does not conflict with its own thread in a complex way,
    // i.e. it does not race with the interruption point, but it enables a subsequent read which allows one to
    // reach a racing instruction.
    // TODO: Test that a signal can interrupt an empty thread (i.e. a thread which otherwise has no interactions).
    // TODO: Test for signals using the same lock as the interrupted thread.
    // TODO: Test that atomic variables do not generate races.
    // TODO: Test that signals that read a certain mask value (implicitly or explicitly) must run after that mask is
    // set.

    private boolean hasRace(
            List<RawTrace> rawTraces,
            ReadonlyEventInterface e1, ReadonlyEventInterface e2) {
        return hasRace(rawTraces, e1, e2, false);
    }

    private boolean hasRace(
            List<RawTrace> rawTraces,
            ReadonlyEventInterface e1, ReadonlyEventInterface e2,
            boolean detectInterruptedThreadRace) {
        Map<String, Race> races = findRaces(rawTraces, e1, e2, detectInterruptedThreadRace);
        return races.size() > 0;
    }

    private Optional<Race> findRace(
            List<RawTrace> rawTraces,
            ReadonlyEventInterface e1, ReadonlyEventInterface e2) {
        Map<String, Race> races = findRaces(rawTraces, e1, e2, true);
        Assert.assertTrue(races.size() < 2);
        if (!races.isEmpty()) {
            return Optional.of(races.values().iterator().next());
        }
        return Optional.empty();
    }

    private Map<String, Race> findRaces(
            List<RawTrace> rawTraces,
            ReadonlyEventInterface e1, ReadonlyEventInterface e2,
            boolean detectInterruptedThreadRace) {
        com.microsoft.z3.Context context = RaceDetector.getZ3Context();
        Z3Filter z3Filter = new Z3Filter(context, WINDOW_SIZE);
        Solver solver = context.mkSimpleSolver();
        Params params = context.mkParams();
        params.add("timeout", TIMEOUT_MILLIS);
        solver.setParameters(params);

        mockConfiguration.windowSize = WINDOW_SIZE;
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        Trace trace = traceState.initNextTraceWindow(rawTraces);
        MaximalCausalModel model = MaximalCausalModel.create(trace, z3Filter, solver, detectInterruptedThreadRace);

        Map<String, List<Race>> sigToRaceSuspects = new HashMap<>();
        ArrayList<Race> raceSuspects = new ArrayList<>();
        raceSuspects.add(new Race(e1, e2, trace, mockConfiguration));
        sigToRaceSuspects.put("race", raceSuspects);

        return model.checkRaceSuspects(sigToRaceSuspects);
    }
}
