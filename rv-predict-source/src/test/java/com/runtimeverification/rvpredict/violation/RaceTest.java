package com.runtimeverification.rvpredict.violation;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.testutils.MoreAsserts;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.trace.TraceState;
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

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RaceTest {
    private static final int WINDOW_SIZE = 100;
    private static final long ADDRESS_1 = 200;
    private static final long VALUE_1 = 300;
    private static final long BASE_ID = 0;
    private static final long BASE_PC = 400;
    private static final long THREAD_1 = 1;
    private static final long THREAD_2 = 1;
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
    public void generatesStackBracketInReport() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.enterFunction(ADDRESS_1 - 1),
                        tu.enterFunction(ADDRESS_1 + 1),
                        e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));


        mockConfiguration.windowSize = WINDOW_SIZE;
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        Trace trace = traceState.initNextTraceWindow(rawTraces);

        Race race = new Race(extractSingleEvent(e1), extractSingleEvent(e2), trace, mockConfiguration);
        race.setFirstSignalStack(Collections.emptyList());
        race.setSecondSignalStack(Collections.emptyList());

        String report = race.generateRaceReport();
        String[] pieces = report.split("\n");
        Optional<String>  maybeBracketed = Optional.empty();
        for (String piece : pieces) {
            if (piece.contains("Bracketed")) {
                maybeBracketed = Optional.of(piece);
                break;
            }
        }
        Assert.assertTrue(maybeBracketed.isPresent());
        String bracketed = maybeBracketed.get();
        MoreAsserts.assertSubstring("[0x00000000000000c7]", bracketed);
        MoreAsserts.assertSubstring("[0x00000000000000c9]", bracketed);
    }

    private static ReadonlyEventInterface extractSingleEvent(List<ReadonlyEventInterface> events) {
        Assert.assertEquals(1, events.size());
        return events.get(0);
    }
}
