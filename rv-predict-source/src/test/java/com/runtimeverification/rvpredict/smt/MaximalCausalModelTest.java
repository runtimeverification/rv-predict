package com.runtimeverification.rvpredict.smt;

import com.microsoft.z3.Params;
import com.microsoft.z3.Solver;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.main.RaceDetector;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.CompactEventFactory;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.smt.visitors.Z3Filter;
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

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MaximalCausalModelTest {
    private static final int WINDOW_SIZE = 100;
    private static final int TIMEOUT_MILLIS = 2000;
    private static final int LONG_SIZE_IN_BYTES = 8;
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
    private static final long LOCK_1 = 500;
    private static final long SIGNAL_NUMBER_1 = 1;
    private static final long SIGNAL_HANDLER_1 = 600;
    private static final long SIGNAL_HANDLER_2 = 601;
    private static final long ALL_SIGNALS_DISABLED_MASK = 0xffffffffffffffffL;
    private static final long GENERATION_1 = 700;
    private static final long SIGNAL_1_ENABLED_MASK = ~(1 << SIGNAL_NUMBER_1);

    private int nextIdDelta = 0;
    private int nextPcDelta = 0;
    private int nextThreadNumber = 1;
    private final CompactEventFactory compactEventFactory = new CompactEventFactory();

    @Mock private Configuration mockConfiguration;
    @Mock private Context mockContext;
    @Mock private Metadata mockMetadata;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        nextPcDelta = 0;
        nextThreadNumber = 1;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
        when(mockContext.createUniqueSignalHandlerId(SIGNAL_NUMBER_1)).thenReturn(1L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_1)).thenReturn(2L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_2)).thenReturn(3L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_3)).thenReturn(4L);
    }

    @Test
    public void detectsSimpleRace() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotDetectRaceBeforeThreadStart() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL),
                        threadStart(THREAD_2, THREAD_1, NO_SIGNAL),
                        threadJoin(THREAD_2, THREAD_1, NO_SIGNAL)),
                createRawTrace(e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }
    @Test
    public void doesNotDetectRaceAfterThreadJoin() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        threadStart(THREAD_2, THREAD_1, NO_SIGNAL),
                        threadJoin(THREAD_2, THREAD_1, NO_SIGNAL),
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void detectsRaceBetweenThreadStartAndJoin() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        threadStart(THREAD_2, THREAD_1, NO_SIGNAL),
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL),
                        threadJoin(THREAD_2, THREAD_1, NO_SIGNAL)),
                createRawTrace(e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotDetectRaceWhenLocked() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        lock(LOCK_1, THREAD_1, NO_SIGNAL),
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL),
                        unlock(LOCK_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(
                        lock(LOCK_1, THREAD_2, NO_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL),
                        unlock(LOCK_1, THREAD_2, NO_SIGNAL)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void detectsRaceWhenOnlyOneThreadIsLocked() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(
                        lock(LOCK_1, THREAD_2, NO_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL),
                        unlock(LOCK_1, THREAD_2, NO_SIGNAL)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void noRaceBecauseOfRestrictsOnDifferentThread() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_1, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                e1 = nonAtomicStore(ADDRESS_3, VALUE_1, THREAD_3, NO_SIGNAL),
                lock(LOCK_1, THREAD_3, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_3, NO_SIGNAL),
                unlock(LOCK_1, THREAD_3, NO_SIGNAL),
                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicLoad(ADDRESS_2, VALUE_2, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicStore(ADDRESS_1, VALUE_2, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicLoad(ADDRESS_1, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                e2 = nonAtomicStore(ADDRESS_3, VALUE_1, THREAD_1, NO_SIGNAL)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                extractRawTrace(events, THREAD_1, NO_SIGNAL),
                extractRawTrace(events, THREAD_2, NO_SIGNAL),
                extractRawTrace(events, THREAD_3, NO_SIGNAL));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    private RawTrace extractRawTrace(List<List<ReadonlyEventInterface>> events, long thread, int signalDepth) {
        List<List<ReadonlyEventInterface>> traceEvents = new ArrayList<>();
        for (List<ReadonlyEventInterface> eventList : events) {
            ReadonlyEventInterface firstEvent = eventList.get(0);
            if (firstEvent.getOriginalThreadId() == thread
                    && firstEvent.getSignalDepth() == signalDepth) {
                traceEvents.add(eventList);
            }
        }
        @SuppressWarnings("unchecked")
        List<ReadonlyEventInterface> traceArray[] = new List[traceEvents.size()];
        for (int i = 0; i < traceEvents.size(); i++) {
            traceArray[i] = traceEvents.get(i);
        }
        return createRawTrace(traceArray);
    }

    @Test
    public void detectsRaceWithSignalOnDifferentThread() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(
                        setSignalHandler(
                                SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_2, NO_SIGNAL),
                        enableSignal(SIGNAL_NUMBER_1, THREAD_2, NO_SIGNAL)),
                createRawTrace(
                        enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_2, ONE_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void detectsRaceWithSignalOnSameThread() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        setSignalHandler(
                                SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                        enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(
                        enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_1, ONE_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL)));

        Assert.assertTrue(hasRace(
                rawTraces, extractSingleEvent(e1), extractSingleEvent(e2), true));
    }

    @Test
    public void doesNotDetectRaceBeforeEnablingSignals() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        setSignalHandler(
                                SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL),
                        enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(nonAtomicLoad(ADDRESS_2, VALUE_1, THREAD_2, NO_SIGNAL)),
                createRawTrace(
                        enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_2, ONE_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotDetectRaceWithDifferentHandlerAddress() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                // Disable the racy instruction on thread 2.
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                setSignalHandler(
                        SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                // Enable the signal
                enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),

                // Run the signal
                enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_1, ONE_SIGNAL),
                e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_1, ONE_SIGNAL),

                // Disable the signal and set a different signal handler, then enable it again.
                disableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                setSignalHandler(
                        SIGNAL_NUMBER_1, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                // Enable the racy instruction on thread 2.
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_1, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),

                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicLoad(ADDRESS_2, VALUE_1, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL));

        List<RawTrace> rawTraces = Arrays.asList(
                extractRawTrace(events, THREAD_1, NO_SIGNAL),
                extractRawTrace(events, THREAD_2, NO_SIGNAL),
                extractRawTrace(events, THREAD_1, ONE_SIGNAL));


        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void detectsRaceWithTheSameHandlerAddress() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                // Disable the racy instruction on thread 2.
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                setSignalHandler(
                        SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                // Enable the signal
                enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),

                // Run the signal
                enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_1, ONE_SIGNAL),
                e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_1, ONE_SIGNAL),

                // Disable the signal and set a different signal handler, then enable it again.
                disableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                setSignalHandler(
                        SIGNAL_NUMBER_1, SIGNAL_HANDLER_2, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                // Enable the racy instruction on thread 2.
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_1, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                // Disable the racy instruction on thread 2.
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                // Disable the signal and set the racy signal handler, then enable it again.
                disableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                setSignalHandler(
                        SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                // Enable the racy instruction on thread 2.
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_1, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                // Disable the racy instruction on thread 2.
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),

                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicLoad(ADDRESS_2, VALUE_1, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL));

        List<RawTrace> rawTraces = Arrays.asList(
                extractRawTrace(events, THREAD_1, NO_SIGNAL),
                extractRawTrace(events, THREAD_2, NO_SIGNAL),
                extractRawTrace(events, THREAD_1, ONE_SIGNAL));


        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotDetectRaceWhenDisabledAndInterruptsThread() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                setSignalHandler(
                        SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),

                enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_1, ONE_SIGNAL),
                e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_1, ONE_SIGNAL),

                disableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_1, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),

                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicLoad(ADDRESS_2, VALUE_1, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL));

        List<RawTrace> rawTraces = Arrays.asList(
                extractRawTrace(events, THREAD_1, NO_SIGNAL),
                extractRawTrace(events, THREAD_2, NO_SIGNAL),
                extractRawTrace(events, THREAD_1, ONE_SIGNAL));


        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void detectsRaceBetweenEnableAndDisable() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                setSignalHandler(
                        SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_1, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),

                enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_1, ONE_SIGNAL),
                e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_1, ONE_SIGNAL),

                disableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),

                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicLoad(ADDRESS_2, VALUE_1, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL));

        List<RawTrace> rawTraces = Arrays.asList(
                extractRawTrace(events, THREAD_1, NO_SIGNAL),
                extractRawTrace(events, THREAD_2, NO_SIGNAL),
                extractRawTrace(events, THREAD_1, ONE_SIGNAL));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void movesSignal() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                setSignalHandler(
                        SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_1, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),

                enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_1, ONE_SIGNAL),
                e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_1, ONE_SIGNAL),

                disableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),

                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicLoad(ADDRESS_2, VALUE_1, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL));

        List<RawTrace> rawTraces = Arrays.asList(
                extractRawTrace(events, THREAD_1, NO_SIGNAL),
                extractRawTrace(events, THREAD_2, NO_SIGNAL),
                extractRawTrace(events, THREAD_1, ONE_SIGNAL));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotDetectRaceBeforeEnabling() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                setSignalHandler(
                        SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_1, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),

                enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_1, ONE_SIGNAL),
                e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_1, ONE_SIGNAL),

                disableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                lock(LOCK_1, THREAD_1, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_1, NO_SIGNAL),
                unlock(LOCK_1, THREAD_1, NO_SIGNAL),

                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicStore(ADDRESS_2, VALUE_2, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                lock(LOCK_1, THREAD_2, NO_SIGNAL),
                nonAtomicLoad(ADDRESS_2, VALUE_1, THREAD_2, NO_SIGNAL),
                unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL));

        List<RawTrace> rawTraces = Arrays.asList(
                extractRawTrace(events, THREAD_1, NO_SIGNAL),
                extractRawTrace(events, THREAD_2, NO_SIGNAL),
                extractRawTrace(events, THREAD_1, ONE_SIGNAL));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotGenerateRacesByInterruptingThreadBeforeBeingStarted() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        nonAtomicStore(ADDRESS_2, VALUE_1, THREAD_1, NO_SIGNAL),
                        setSignalHandler(
                                SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL),
                        enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                        threadStart(THREAD_2, THREAD_1, NO_SIGNAL)),
                createRawTrace(
                        nonAtomicLoad(ADDRESS_2, VALUE_1, THREAD_2, NO_SIGNAL)),
                createRawTrace(
                        enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_2, ONE_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void doesNotGenerateRacesByInterruptingThreadAfterBeingJoined() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        setSignalHandler(
                                SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                        nonAtomicStore(ADDRESS_2, VALUE_1, THREAD_1, NO_SIGNAL),
                        enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                        threadStart(THREAD_2, THREAD_1, NO_SIGNAL),
                        disableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                        threadJoin(THREAD_2, THREAD_1, NO_SIGNAL),
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(
                        nonAtomicLoad(ADDRESS_2, VALUE_1, THREAD_2, NO_SIGNAL)),
                createRawTrace(
                        enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_2, ONE_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void signalCanGenerateRacesEvenIfFullyDisabledOnAnotherThread() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        setSignalHandler(
                                SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                        disableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                        threadStart(THREAD_3, THREAD_1, NO_SIGNAL),
                        threadJoin(THREAD_3, THREAD_1, NO_SIGNAL),
                        enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                        threadStart(THREAD_2, THREAD_1, NO_SIGNAL),
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL),
                        threadJoin(THREAD_2, THREAD_1, NO_SIGNAL)),
                createRawTrace(
                        nonAtomicLoad(ADDRESS_2, VALUE_1, THREAD_2, NO_SIGNAL)),
                createRawTrace(
                        nonAtomicStore(ADDRESS_2, VALUE_1, THREAD_3, NO_SIGNAL)),
                createRawTrace(
                        enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_2, ONE_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void signalsDoNotGenerateRacesAfterDisestablishingThem() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        nonAtomicStore(ADDRESS_2, VALUE_1, THREAD_1, NO_SIGNAL),
                        setSignalHandler(
                                SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                        nonAtomicStore(ADDRESS_3, VALUE_2, THREAD_1, NO_SIGNAL),
                        disableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL),
                        threadStart(THREAD_2, THREAD_1, NO_SIGNAL),
                        lock(LOCK_1, THREAD_1, NO_SIGNAL),
                        nonAtomicLoad(ADDRESS_3, VALUE_1, THREAD_1, NO_SIGNAL),
                        unlock(LOCK_1, THREAD_1, NO_SIGNAL),
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(
                        enableSignal(SIGNAL_NUMBER_1, THREAD_2, NO_SIGNAL),
                        disestablishSignal(SIGNAL_NUMBER_1, THREAD_2, NO_SIGNAL),
                        lock(LOCK_1, THREAD_2, NO_SIGNAL),
                        nonAtomicStore(ADDRESS_3, VALUE_1, THREAD_2, NO_SIGNAL),
                        unlock(LOCK_1, THREAD_2, NO_SIGNAL),
                        nonAtomicLoad(ADDRESS_2, VALUE_1, THREAD_2, NO_SIGNAL)),
                createRawTrace(
                        enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_2, ONE_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_2, NO_SIGNAL)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void getSignalMaskMayShowThatASignalIsEnabled() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        getSignalMask(SIGNAL_1_ENABLED_MASK, THREAD_1, NO_SIGNAL),
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL),
                        enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(
                        enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_1, ONE_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2), true));
    }

    @Test
    public void enableSignalStopsFromDetectingRacesBeforeEnabling() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL),
                        enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(
                        enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_1, ONE_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL)));

        Assert.assertFalse(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2), true));
    }

    @Test
    public void getSetSignalMaskMayShowThatASignalIsEnabledBefore() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL),
                        getSetSignalMask(SIGNAL_1_ENABLED_MASK, ALL_SIGNALS_DISABLED_MASK, THREAD_1, NO_SIGNAL),
                        enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(
                        enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_1, ONE_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2), true));
    }

    @Test
    public void setSignalMaskEnablesSignals() throws InvalidTraceDataException {
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                createRawTrace(
                        setSignalMask(SIGNAL_1_ENABLED_MASK, THREAD_1, NO_SIGNAL),
                        e1 = nonAtomicLoad(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL),
                        enableSignal(SIGNAL_NUMBER_1, THREAD_1, NO_SIGNAL)),
                createRawTrace(
                        enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, THREAD_1, ONE_SIGNAL),
                        e2 = nonAtomicStore(ADDRESS_1, VALUE_1, THREAD_1, NO_SIGNAL)));

        Assert.assertTrue(hasRace(rawTraces, extractSingleEvent(e1), extractSingleEvent(e2), true));
    }

    // TODO: Tests with writes that enable certain reads, both with and without signals.
    // TODO: Test that a signals stops their thread, i.e. it does not conflict with its own thread in a complex way,
    // i.e. it does not race with the interruption point, but it enables a subsequent read which allows one to
    // reach a racing instruction.
    // TODO: Tests for get, set and getset for signal masks.
    // TODO: Test that threads are not interrupted before being started / after being joined.
    // TODO: Test that a signal can interrupt an empty thread (i.e. a thread which otherwise has no interactions).
    // TODO: Test for disestablishSignal
    // TODO: Test for signals using the same lock as the interrupted thread.

    private List<ReadonlyEventInterface> enterSignal(
            long signalNumber, long signalHandler, long threadId, int signalDepth) throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.enterSignal(mockContext, GENERATION_1, signalNumber, signalHandler);
    }

    private List<ReadonlyEventInterface> setSignalHandler(
            long signalNumber, long signalHandler, long disabledSignalMask, long threadId, int signalDepth)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        when(mockContext.getMemoizedSignalMask(123)).thenReturn(disabledSignalMask);
        return compactEventFactory.establishSignal(mockContext, signalHandler, signalNumber, 123);
    }

    private List<ReadonlyEventInterface> disableSignal(long signalNumber, long threadId, int signalDepth)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        when(mockContext.getMemoizedSignalMask(123)).thenReturn(1L << Math.toIntExact(signalNumber));
        return compactEventFactory.blockSignals(mockContext, 123);
    }

    private List<ReadonlyEventInterface> enableSignal(long signalNumber, long threadId, int signalDepth)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        when(mockContext.getMemoizedSignalMask(123)).thenReturn(1L << Math.toIntExact(signalNumber));
        return compactEventFactory.unblockSignals(mockContext, 123);
    }

    private List<ReadonlyEventInterface> lock(long lockId, long threadId, int signalDepth)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.lockManipulation(mockContext, CompactEventReader.LockManipulationType.LOCK, lockId);
    }

    private List<ReadonlyEventInterface> unlock(long lockId, long threadId, int signalDepth)
            throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.lockManipulation(
                mockContext, CompactEventReader.LockManipulationType.UNLOCK, lockId);
    }

    private List<ReadonlyEventInterface> threadStart(long newThread, long currentThread, int signalDepth)
            throws InvalidTraceDataException {
        prepareContextForEvent(currentThread, signalDepth);
        return compactEventFactory.threadSync(
                mockContext, CompactEventReader.ThreadSyncType.FORK, newThread);
    }

    private List<ReadonlyEventInterface> threadJoin(long newThread, long currentThread, int signalDepth)
            throws InvalidTraceDataException {
        prepareContextForEvent(currentThread, signalDepth);
        return compactEventFactory.threadSync(
                mockContext, CompactEventReader.ThreadSyncType.JOIN, newThread);
    }

    private List<ReadonlyEventInterface> nonAtomicLoad(
            long address, long value, long threadId, int signalDepth) throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.dataManipulation(
                mockContext, CompactEventReader.DataManipulationType.LOAD, LONG_SIZE_IN_BYTES,
                address, value,
                CompactEventReader.Atomicity.NOT_ATOMIC);
    }

    private List<ReadonlyEventInterface> nonAtomicStore(
            long address, long value, long threadId, int signalDepth) throws InvalidTraceDataException {
        prepareContextForEvent(threadId, signalDepth);
        return compactEventFactory.dataManipulation(
                        mockContext, CompactEventReader.DataManipulationType.STORE, LONG_SIZE_IN_BYTES,
                        address, value,
                        CompactEventReader.Atomicity.NOT_ATOMIC);
    }

    private ReadonlyEventInterface extractSingleEvent(List<ReadonlyEventInterface> events) {
        Assert.assertEquals(1, events.size());
        return events.get(0);
    }

    private void prepareContextForEvent(long threadId, int signalDepth) {
        when(mockContext.getPC()).thenReturn(BASE_PC + nextPcDelta);
        nextPcDelta++;
        when(mockContext.getThreadId()).thenReturn(threadId);
        when(mockContext.getSignalDepth()).thenReturn(signalDepth);
    }

    @SafeVarargs
    private final RawTrace createRawTrace(List<ReadonlyEventInterface>... events) {
        int size = 0;
        for (List<ReadonlyEventInterface> eventList : events) {
            size += eventList.size();
        }
        int paddedSize = 1;
        while (paddedSize <= size) {
            paddedSize = paddedSize * 2;
        }
        ReadonlyEventInterface[] paddedEvents = new ReadonlyEventInterface[paddedSize];
        int pos = 0;
        for (List<ReadonlyEventInterface> eventList : events) {
            for (ReadonlyEventInterface event : eventList) {
                paddedEvents[pos] = event;
                pos++;
            }
        }
        int currentThreadNumber = this.nextThreadNumber;
        this.nextThreadNumber++;
        return new RawTrace(0, pos, paddedEvents, paddedEvents[0].getSignalDepth(), currentThreadNumber);
    }

    private boolean hasRace(
            List<RawTrace> rawTraces,
            ReadonlyEventInterface e1, ReadonlyEventInterface e2) {
        return hasRace(rawTraces, e1, e2, false);
    }
    private boolean hasRace(
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

        Map<String, Race> races = model.checkRaceSuspects(sigToRaceSuspects);
        return races.size() > 0;
    }
}
