package com.runtimeverification.rvpredict.violation;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.LockRepresentation;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.testutils.MoreAsserts;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.trace.TraceState;
import com.runtimeverification.error.data.ErrorCategory;
import com.runtimeverification.error.data.RawComponentField;
import com.runtimeverification.error.data.RawStackError;
import com.runtimeverification.error.data.RawStackTrace;
import com.runtimeverification.error.data.RawStackTraceComponent;
import com.runtimeverification.error.data.RawFrame;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RaceTest {
    private static final int WINDOW_SIZE = 100;
    private static final long ADDRESS_1 = 200;
    private static final long VALUE_1 = 300;
    private static final long BASE_ID = 0;
    private static final long BASE_PC = 400;
    private static final long PROGRAM_COUNTER_1 = 450;
    private static final long PROGRAM_COUNTER_2 = 451;
    private static final long PROGRAM_COUNTER_3 = 452;
    private static final long PROGRAM_COUNTER_4 = 453;
    private static final long CALL_SITE_ADDRESS_1 = 500;
    private static final long CALL_SITE_ADDRESS_2 = 501;
    private static final long THREAD_1 = 1;
    private static final long THREAD_2 = 2;
    private static final int NO_SIGNAL = 0;

    private int nextIdDelta = 0;

    @Mock private Configuration mockConfiguration;
    @Mock private Context mockContext;
    @Mock private Metadata mockMetadata;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
        when(mockContext.createUniqueDataAddressId(ADDRESS_1)).thenReturn(2L);
        when(mockMetadata.getLocationSig(anyLong())).thenReturn("");
        when(mockMetadata.getVariableSig(anyLong())).thenReturn("");
        when(mockConfiguration.isCompactTrace()).thenReturn(true);
        when(mockConfiguration.isLLVMPrediction()).thenReturn(true);
        when(mockConfiguration.stacks()).thenReturn(true);
    }

    @Test
    public void raceDescriptionGeneratedByMetadata() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.enterFunction(ADDRESS_1 - 1, OptionalLong.of(CALL_SITE_ADDRESS_1)),
                        tu.enterFunction(ADDRESS_1 + 1, OptionalLong.of(CALL_SITE_ADDRESS_2)),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));


        mockConfiguration.windowSize = WINDOW_SIZE;
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getRaceDataSig(
                extractSingleEvent(e1), extractSingleEvent(e2), trace, mockConfiguration))
                .thenReturn("<mock race report>");

        Race race = new Race(extractSingleEvent(e1), extractSingleEvent(e2), trace, mockConfiguration);
        race.setFirstSignalStack(Collections.emptyList());
        race.setSecondSignalStack(Collections.emptyList());

        String report = race.generateRaceReport();
        String[] pieces = report.split("\n");
        Optional<String>  maybeRaceDescription = Optional.empty();
        for (String piece : pieces) {
            if (piece.contains("Data race")) {
                maybeRaceDescription = Optional.of(piece);
                break;
            }
        }
        Assert.assertTrue(maybeRaceDescription.isPresent());
        String raceDescription = maybeRaceDescription.get();
        MoreAsserts.assertSubstring("<mock race report>", raceDescription);

        Optional<RawStackError> reportData = race.generateErrorData(mockMetadata);
        Assert.assertTrue(reportData.isPresent());
        Assert.assertEquals("Data race on %s", reportData.get().description_format);
        Assert.assertEquals(1, reportData.get().description_fields.size());
        Assert.assertEquals("<mock race report>", reportData.get().description_fields.get(0).address);
        Assert.assertEquals("CEER4", reportData.get().error_id);
        Assert.assertEquals(ErrorCategory.Tag.UNDEFINED, reportData.get().category.tag());
    }

    @Test
    public void raceDescriptionContainsCallSiteAddress() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.enterFunction(ADDRESS_1 - 1, OptionalLong.of(CALL_SITE_ADDRESS_1)),
                        tu.enterFunction(ADDRESS_1 + 1, OptionalLong.of(CALL_SITE_ADDRESS_2)),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));


        mockConfiguration.windowSize = WINDOW_SIZE;
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getLocationSig(CALL_SITE_ADDRESS_1)).thenReturn("<call site address 1>");
        when(mockMetadata.getLocationSig(CALL_SITE_ADDRESS_2)).thenReturn("<call site address 2>");

        Race race = new Race(extractSingleEvent(e1), extractSingleEvent(e2), trace, mockConfiguration);
        race.setFirstSignalStack(Collections.emptyList());
        race.setSecondSignalStack(Collections.emptyList());

        String report = race.generateRaceReport();
        MoreAsserts.assertSubstring("<call site address 1>", report);
        MoreAsserts.assertSubstring("<call site address 2>", report);

	Optional<RawStackError> reportData = race.generateErrorData(mockMetadata);
	Assert.assertTrue(reportData.isPresent());
	Assert.assertEquals(2, reportData.get().stack_traces.size());
	RawStackTrace t1 = reportData.get().stack_traces.get(0);
	RawStackTrace t2 = reportData.get().stack_traces.get(1);
	assertStackTrace(t1, "1", 3, "Read in thread 1", "", "<call site address 2>", "<call site address 1>");
	assertStackTrace(t2, "2", 1, "Write in thread 2");
    }

    private void assertStackTrace(RawStackTrace t, String id, int numFrames, String description_format, String... addresses) {
	    Assert.assertEquals(1, t.components.size());
	    Assert.assertEquals(id, t.thread_id);
	    RawStackTraceComponent c = t.components.get(0);
	    Assert.assertEquals(numFrames, c.frames.size());
            Assert.assertEquals(description_format, c.description_format);
	    for (int i = 0; i < addresses.length; i++) {
		    Assert.assertEquals(addresses[i], c.frames.get(i).address);
	    }
    }
    

    @Test
    public void raceDescriptionContainsMethodStartWhenCallSiteAddressIsMissing() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.setPc(PROGRAM_COUNTER_1),
                        tu.enterFunction(ADDRESS_1, OptionalLong.of(CALL_SITE_ADDRESS_1)),  // method 1
                        tu.setPc(PROGRAM_COUNTER_2),
                        tu.enterFunction(ADDRESS_1, OptionalLong.empty()),  // method 2
                        tu.setPc(PROGRAM_COUNTER_3),
                        tu.enterFunction(ADDRESS_1, OptionalLong.of(CALL_SITE_ADDRESS_2)),  // method 3
                        tu.setPc(PROGRAM_COUNTER_4),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));


        mockConfiguration.windowSize = WINDOW_SIZE;
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getLocationSig(CALL_SITE_ADDRESS_1)).thenReturn("<method 1 call site somewhere>");
        when(mockMetadata.getLocationSig(CALL_SITE_ADDRESS_2)).thenReturn("<method 3 call site in method 2>");
        when(mockMetadata.getLocationSig(PROGRAM_COUNTER_1)).thenReturn("<method 1 start>");
        when(mockMetadata.getLocationSig(PROGRAM_COUNTER_2)).thenReturn("<method 2 start>");
        when(mockMetadata.getLocationSig(PROGRAM_COUNTER_3)).thenReturn("<method 3 start>");
        when(mockMetadata.getLocationSig(PROGRAM_COUNTER_4)).thenReturn("<instruction 4 in method 3>");

        Race race = new Race(extractSingleEvent(e1), extractSingleEvent(e2), trace, mockConfiguration);
        race.setFirstSignalStack(Collections.emptyList());
        race.setSecondSignalStack(Collections.emptyList());

        String report = race.generateRaceReport();
        MoreAsserts.assertSubstring("<instruction 4 in method 3>", report);
        MoreAsserts.assertSubstring("<method 3 call site in method 2>", report);
        MoreAsserts.assertSubstring("<method 1 start>", report);
        MoreAsserts.assertSubstring("<method 1 call site somewhere>", report);

        MoreAsserts.assertNotSubstring("<method 2 start>", report);
        MoreAsserts.assertNotSubstring("<method 3 start>", report);

	Optional<RawStackError> reportData = race.generateErrorData(mockMetadata);
	Assert.assertTrue(reportData.isPresent());
	Assert.assertEquals(2, reportData.get().stack_traces.size());
	RawStackTrace t1 = reportData.get().stack_traces.get(0);
	RawStackTrace t2 = reportData.get().stack_traces.get(1);
	assertStackTrace(t2, "1", 4, "Read in thread 1", "<instruction 4 in method 3>", "<method 3 call site in method 2>", "<method 1 start>", "<method 1 call site somewhere>");
	assertStackTrace(t1, "2", 1, "Write in thread 2");

    }

    @Test
    public void raceDescriptionContainsLocksFormattedByMetadata() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.enterFunction(ADDRESS_1 - 1, OptionalLong.of(CALL_SITE_ADDRESS_1)),  // method 1
                        e3 = tu.lock(10),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));


        mockConfiguration.windowSize = WINDOW_SIZE;
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getLockSig(extractSingleEvent(e3), trace))
                .thenReturn("<mock lock representation>");
        when(mockMetadata.getLocationSig(CALL_SITE_ADDRESS_1)).thenReturn("<call site address 1>");

        Race race = new Race(extractSingleEvent(e1), extractSingleEvent(e2), trace, mockConfiguration);
        race.setFirstSignalStack(Collections.emptyList());
        race.setSecondSignalStack(Collections.emptyList());

        String report = race.generateRaceReport();
        String[] pieces = report.split("\n");
        boolean hasHoldingLock = false;
        boolean hasStackLock = false;
        for (String piece : pieces) {
            if (piece.contains("holding lock")) {
                MoreAsserts.assertSubstring("<mock lock representation>", piece);
                hasHoldingLock = true;
            }
            if (piece.contains("- locked")) {
                MoreAsserts.assertSubstring("<mock lock representation>", piece);
                hasStackLock = true;
            }
        }
        Assert.assertTrue(hasHoldingLock);
        Assert.assertTrue(hasStackLock);

        Optional<RawStackError> reportData = race.generateErrorData(mockMetadata);
        Assert.assertTrue(reportData.isPresent());
        Assert.assertEquals(2, reportData.get().stack_traces.size());
        RawStackTrace t1 = reportData.get().stack_traces.get(0);
        RawStackTrace t2 = reportData.get().stack_traces.get(1);
        assertStackTrace(t1, "1", 2, "Read in thread 1 holding lock %s", "", "<call site address 1>");
        assertStackTrace(t2, "2", 1, "Write in thread 2");
        RawStackTraceComponent c1 = t1.components.get(0);
        Assert.assertEquals(1, c1.description_fields.size());
        Assert.assertEquals(RawComponentField.Tag.LOCK, c1.description_fields.get(0).tag());
        Assert.assertEquals("<mock lock representation>", c1.description_fields.get(0).getLock().address);
        RawFrame f1 = t1.components.get(0).frames.get(0);
        Assert.assertEquals(1, f1.locks.size());
        Assert.assertEquals("<mock lock representation>", f1.locks.get(0).id.address);
    }

    private static ReadonlyEventInterface extractSingleEvent(List<ReadonlyEventInterface> events) {
        Assert.assertEquals(1, events.size());
        return events.get(0);
    }
}
