package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.performance.AnalysisLimit;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.SharedLibraries;
import com.runtimeverification.rvpredict.trace.ThreadInfos;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.trace.TraceState;
import com.runtimeverification.rvpredict.util.Logger;
import com.runtimeverification.rvpredict.violation.Race;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.FileNotFoundException;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.runtimeverification.rvpredict.testutils.TraceUtils.extractEventByType;
import static com.runtimeverification.rvpredict.testutils.TraceUtils.extractSingleEvent;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JavaHappensBeforeRaceDetectorTest {
    private static final int WINDOW_SIZE = 100;
    private static final long ADDRESS_1 = 200;
    private static final long ADDRESS_2 = 201;
    private static final long ADDRESS_3_VOLATILE = 202;
    private static final long VALUE_1 = 300;
    private static final long VALUE_2 = 301;
    private static final long BASE_ID = 0;
    private static final long BASE_PC = 400;
    private static final long THREAD_1 = 1;
    private static final long THREAD_2 = 2;
    private static final long THREAD_3 = 3;
    private static final int NO_SIGNAL = 0;
    private static final long LOCK_1 = 500;

    private int nextIdDelta = 0;

    @Mock private Configuration mockConfiguration;
    @Mock private Context mockContext;
    @Mock private Metadata mockMetadata;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
        when(mockContext.createUniqueDataAddressId(ADDRESS_1)).thenReturn(2L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_2)).thenReturn(3L);
        when(mockContext.createUniqueDataAddressId(ADDRESS_3_VOLATILE)).thenReturn(4L);
        when(mockMetadata.isVolatile(anyLong())).
                then(invocation -> invocation.getArguments()[0].equals(Long.valueOf(4L)));
        when(mockMetadata.getLocationSig(anyLong(), any())).thenReturn("unknown location");
        Logger logger = new Logger();
        when(mockConfiguration.logger()).thenReturn(logger);
    }

    @Test
    public void simpleWriteWriteRace() throws Exception {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL));

        Assert.assertTrue(
                hasRace(Collections.singletonList(rawTraces), extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void simpleWriteWriteNoRace() throws Exception {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.lock(LOCK_1),
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.lock(LOCK_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.unlock(LOCK_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL));

        Assert.assertFalse(
                hasRace(Collections.singletonList(rawTraces), extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    // According to the Java Memory Model spec, volatile writes are conflicting accesses.
    // However, this seems to be a problem with the specification itself, according to
    // http://cs.oswego.edu/pipermail/concurrency-interest/2012-January/008927.html
    @Ignore
    public void simpleVolatileWriteWrite() throws Exception {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                e1 = tu.nonAtomicStore(ADDRESS_3_VOLATILE, VALUE_2),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                e2 = tu.nonAtomicStore(ADDRESS_3_VOLATILE, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL));

        Assert.assertTrue(
                hasRace(Collections.singletonList(rawTraces), extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void simpleVolatileNoRace() throws Exception {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.nonAtomicStore(ADDRESS_3_VOLATILE, VALUE_2),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.nonAtomicLoad(ADDRESS_3_VOLATILE, VALUE_2),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL));

        Assert.assertFalse(
                hasRace(Collections.singletonList(rawTraces), extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void simpleWriteReadRace() throws Exception {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                e2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL));

        Assert.assertTrue(
                hasRace(Collections.singletonList(rawTraces), extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void simpleWriteReadAtomicNoRace() throws Exception {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                e1 = tu.atomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                e2 = tu.atomicLoad(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL));

        Assert.assertFalse(
                hasRace(Collections.singletonList(rawTraces),
                        extractEventByType(e1, EventType.WRITE),
                        extractEventByType(e2, EventType.READ)));
    }

    @Test
    public void simpleReadWriteSynchronizedRace() throws Exception {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.unlock(LOCK_1),
                e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.lock(LOCK_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.unlock(LOCK_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL));

        Assert.assertTrue(
                hasRace(Collections.singletonList(rawTraces), extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void simpleSynchronizedReadWriteNoRace() throws Exception {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.lock(LOCK_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                tu.unlock(LOCK_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.lock(LOCK_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.unlock(LOCK_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL));

        Assert.assertFalse(
                hasRace(Collections.singletonList(rawTraces), extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void simpleSpawnNoRace() throws Exception {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.threadStart(THREAD_2),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL));

        Assert.assertFalse(
                hasRace(Collections.singletonList(rawTraces), extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    @Test
    public void simpleJoinNoRace() throws Exception {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_2, NO_SIGNAL),
                tu.threadJoin(THREAD_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        List<RawTrace> rawTraces = Arrays.asList(
                tu.extractRawTrace(events, THREAD_1, NO_SIGNAL),
                tu.extractRawTrace(events, THREAD_2, NO_SIGNAL));

        Assert.assertFalse(
                hasRace(Collections.singletonList(rawTraces), extractSingleEvent(e1), extractSingleEvent(e2)));
    }

    private boolean hasRace(List<List<RawTrace>> rawTracesList, ReadonlyEventInterface e1, ReadonlyEventInterface e2) {
        mockConfiguration.windowSize = WINDOW_SIZE;
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        ThreadInfos threadInfos = traceState.getThreadInfos();
        JavaHappensBeforeRaceDetector detector = new JavaHappensBeforeRaceDetector(mockConfiguration, mockMetadata);

        Trace trace = null;
        assert !rawTracesList.isEmpty();
        for (List<RawTrace> rawTraces : rawTracesList) {
            for (RawTrace rawTrace : rawTraces) {
                threadInfos.registerThreadInfo(rawTrace.getThreadInfo());
            }
            traceState.preStartWindow();
            trace = traceState.initNextTraceWindow(rawTraces);
            detector.run(trace, new AnalysisLimit(Clock.systemUTC(), "Test", Optional.empty(), 0, mockConfiguration.logger()));
            Race testRace = new Race(e1, e2, trace, mockConfiguration);
            String testRaceSig = testRace.toString();
            return detector.races.containsKey(testRaceSig);
        }
        return false;
    }
}
