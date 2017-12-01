package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.deadlock.LockGraph;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.CompactEvent;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.testutils.MoreAsserts;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.util.Constants;
import com.runtimeverification.rvpredict.util.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TraceCacheTest {
    private static final long FIRST_EVENT_ID = 100;
    private static final long FIRST_LOCATION_ID = 200;
    private static final long THREAD_ID = 300;
    private static final long THREAD_ID_2 = 301;
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final long SIGNAL_NUMBER = 4;
    private static final int NEW_THREAD_ID = 500;
    private static final int NEW_THREAD_ID_2 = 501;
    private static final int NEW_THREAD_ID_3 = 502;
    private static final int NEW_THREAD_ID_4 = 503;
    private static final long SIGNAL_HANDLER_ADDRESS = 600;
    private static final long PC_BASE = 700;
    private static final long DATA_ADDRESS_1 = 800;
    private static final long VALUE_1 = 900;
    private static final long GENERATION = 1000;

    private static final ThreadInfo TTID_1_OTID_1_THREAD = ThreadInfo.createThreadInfo(
            NEW_THREAD_ID, THREAD_ID, OptionalInt.empty());
    private static final ThreadInfo TTID_2_OTID_2_THREAD = ThreadInfo.createThreadInfo(
            NEW_THREAD_ID_2, THREAD_ID_2, OptionalInt.empty());
    private static final ThreadInfo TTID_3_OTID_1_SIGNAL = ThreadInfo.createSignalInfo(
            NEW_THREAD_ID_3, THREAD_ID, SIGNAL_NUMBER, SIGNAL_HANDLER_ADDRESS, 1);
    private static final ThreadInfo TTID_4_OTID_1_SIGNAL = ThreadInfo.createSignalInfo(
            NEW_THREAD_ID_4, THREAD_ID, SIGNAL_NUMBER, SIGNAL_HANDLER_ADDRESS, 1);

    @Mock private Context mockContext;
    @Mock private Configuration mockConfiguration;
    @Mock private TraceState mockTraceState;
    @Mock private LockGraph mockLockGraph;
    @Mock private Trace mockTrace;
    @Mock private ThreadInfos mockThreadInfos;

    @Captor private ArgumentCaptor<List<RawTrace>> rawTraceArgumentCaptor;

    private long eventId;
    private long locationId;

    @Before
    public void setUp() {
        eventId = FIRST_EVENT_ID;
        locationId = FIRST_LOCATION_ID;
        when(mockTraceState.getThreadInfos()).thenReturn(mockThreadInfos);
        Logger logger = new Logger();
        when(mockConfiguration.logger()).thenReturn(logger);
    }

    @Test
    public void createsOneRawTracePerThread() throws IOException {
        mockConfiguration.windowSize = 10;
        when(mockConfiguration.stacks()).thenReturn(false);
        when(mockConfiguration.isLLVMPrediction()).thenReturn(true);
        when(mockTraceState.initNextTraceWindow(any())).thenReturn(mockTrace);
        when(mockThreadInfos.getTtidFromOtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockTraceState.createAndRegisterThreadInfo(THREAD_ID, OptionalInt.empty()))
                .thenReturn(TTID_1_OTID_1_THREAD);
        when(mockTraceState.createAndRegisterThreadInfo(THREAD_ID_2, OptionalInt.empty()))
                .thenReturn(TTID_2_OTID_2_THREAD);

        when(mockThreadInfos.getThreadInfo(TTID_1_OTID_1_THREAD.getId())).thenReturn(TTID_1_OTID_1_THREAD);
        when(mockThreadInfos.getThreadInfo(TTID_2_OTID_2_THREAD.getId())).thenReturn(TTID_2_OTID_2_THREAD);

        when(mockThreadInfos.getTtidFromOtid(TTID_1_OTID_1_THREAD.getOriginalThreadId()))
                .thenReturn(OptionalInt.of(TTID_1_OTID_1_THREAD.getId()));
        when(mockThreadInfos.getTtidFromOtid(TTID_2_OTID_2_THREAD.getOriginalThreadId()))
                .thenReturn(OptionalInt.of(TTID_2_OTID_2_THREAD.getId()));

        List<ReadonlyEventInterface> beginThread1 = beginThread(THREAD_ID);
        List<ReadonlyEventInterface> readData1Thread1 = readData(THREAD_ID, NO_SIGNAL);

        List<ReadonlyEventInterface> beginThread2 = beginThread(THREAD_ID + 1);
        List<ReadonlyEventInterface> readDataThread2 = readData(THREAD_ID + 1, NO_SIGNAL);

        List<ReadonlyEventInterface> readData2Thread1 = readData(THREAD_ID, NO_SIGNAL);

        ListEventReader eventReader =
                new ListEventReader(Arrays.asList(
                        beginThread1, readData1Thread1, beginThread2, readDataThread2, readData2Thread1));

        TraceCache traceCache =
                TraceCache.createForTesting(
                        mockConfiguration, mockTraceState, mockLockGraph, Collections.singletonList(eventReader));

        Assert.assertEquals(mockTrace, traceCache.getTraceWindow());

        verify(mockTraceState).initNextTraceWindow(rawTraceArgumentCaptor.capture());

        List<RawTrace> rawTraces = rawTraceArgumentCaptor.getValue();
        MoreAsserts.assertNotNull(rawTraces);

        Assert.assertEquals(2, rawTraces.size());

        int eventIndex = 0;
        eventIndex = assertEqualEvents(beginThread1, rawTraces.get(0), eventIndex);
        eventIndex = assertEqualEvents(readData1Thread1, rawTraces.get(0), eventIndex);
        eventIndex = assertEqualEvents(readData2Thread1, rawTraces.get(0), eventIndex);
        Assert.assertEquals(eventIndex, rawTraces.get(0).size());


        Assert.assertEquals(2, rawTraces.get(1).size());
        eventIndex = 0;
        eventIndex = assertEqualEvents(beginThread2, rawTraces.get(1), eventIndex);
        eventIndex = assertEqualEvents(readDataThread2, rawTraces.get(1), eventIndex);
        Assert.assertEquals(eventIndex, rawTraces.get(1).size());
    }

    @Test
    public void createsOneRawTracePerSignal() throws IOException {
        mockConfiguration.windowSize = 10;
        when(mockConfiguration.stacks()).thenReturn(false);
        when(mockConfiguration.isLLVMPrediction()).thenReturn(true);
        when(mockTraceState.initNextTraceWindow(any())).thenReturn(mockTrace);
        when(mockThreadInfos.getTtidFromOtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockTraceState.createAndRegisterThreadInfo(THREAD_ID, OptionalInt.empty()))
                .thenReturn(TTID_1_OTID_1_THREAD);
        when(mockTraceState.createAndRegisterSignalInfo(THREAD_ID, SIGNAL_NUMBER, SIGNAL_HANDLER_ADDRESS, ONE_SIGNAL))
                .thenReturn(TTID_3_OTID_1_SIGNAL);

        when(mockThreadInfos.getThreadInfo(TTID_1_OTID_1_THREAD.getId())).thenReturn(TTID_1_OTID_1_THREAD);
        when(mockThreadInfos.getThreadInfo(TTID_3_OTID_1_SIGNAL.getId())).thenReturn(TTID_3_OTID_1_SIGNAL);

        when(mockThreadInfos.getTtidFromOtid(TTID_1_OTID_1_THREAD.getOriginalThreadId()))
                .thenReturn(OptionalInt.of(TTID_1_OTID_1_THREAD.getId()));

        List<ReadonlyEventInterface> beginThread1 = beginThread(THREAD_ID);
        List<ReadonlyEventInterface> readData1Thread1 = readData(THREAD_ID, NO_SIGNAL);

        List<ReadonlyEventInterface> beginSignal =
                beginSignal(SIGNAL_NUMBER, SIGNAL_HANDLER_ADDRESS, THREAD_ID, ONE_SIGNAL);
        List<ReadonlyEventInterface> readDataSignal = readData(THREAD_ID, ONE_SIGNAL);
        List<ReadonlyEventInterface> readData2Thread1 = readData(THREAD_ID, NO_SIGNAL);

        ListEventReader eventReader =
                new ListEventReader(Arrays.asList(
                        beginThread1, readData1Thread1, beginSignal, readDataSignal, readData2Thread1));

        TraceCache traceCache =
                TraceCache.createForTesting(
                        mockConfiguration, mockTraceState, mockLockGraph, Collections.singletonList(eventReader));

        Assert.assertEquals(mockTrace, traceCache.getTraceWindow());

        verify(mockTraceState).initNextTraceWindow(rawTraceArgumentCaptor.capture());

        List<RawTrace> rawTraces = rawTraceArgumentCaptor.getValue();
        MoreAsserts.assertNotNull(rawTraces);

        Assert.assertEquals(2, rawTraces.size());
        Assert.assertEquals(0, rawTraces.get(0).getSignalDepth());
        int eventIndex = 0;
        eventIndex = assertEqualEvents(beginThread1, rawTraces.get(0), eventIndex);
        eventIndex = assertEqualEvents(readData1Thread1, rawTraces.get(0), eventIndex);
        eventIndex = assertEqualEvents(readData2Thread1, rawTraces.get(0), eventIndex);
        Assert.assertEquals(eventIndex, rawTraces.get(0).size());

        Assert.assertEquals(1, rawTraces.get(1).getSignalDepth());
        eventIndex = 0;
        eventIndex = assertEqualEvents(beginSignal, rawTraces.get(1), eventIndex);
        eventIndex = assertEqualEvents(readDataSignal, rawTraces.get(1), eventIndex);
        Assert.assertEquals(eventIndex, rawTraces.get(1).size());
    }

    @Test
    public void splitsConsecutiveSignalRunsIntoRawTraces() throws IOException {
        mockConfiguration.windowSize = 100;
        when(mockConfiguration.stacks()).thenReturn(false);
        when(mockConfiguration.isLLVMPrediction()).thenReturn(true);
        when(mockTraceState.initNextTraceWindow(any())).thenReturn(mockTrace);
        when(mockThreadInfos.getTtidFromOtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockTraceState.createAndRegisterThreadInfo(THREAD_ID, OptionalInt.empty()))
                .thenReturn(TTID_1_OTID_1_THREAD);
        when(mockTraceState.createAndRegisterSignalInfo(THREAD_ID, SIGNAL_NUMBER, SIGNAL_HANDLER_ADDRESS, ONE_SIGNAL))
                .thenReturn(TTID_3_OTID_1_SIGNAL).thenReturn(TTID_4_OTID_1_SIGNAL);

        when(mockThreadInfos.getThreadInfo(TTID_1_OTID_1_THREAD.getId())).thenReturn(TTID_1_OTID_1_THREAD);
        when(mockThreadInfos.getThreadInfo(TTID_3_OTID_1_SIGNAL.getId())).thenReturn(TTID_3_OTID_1_SIGNAL);
        when(mockThreadInfos.getThreadInfo(TTID_4_OTID_1_SIGNAL.getId())).thenReturn(TTID_4_OTID_1_SIGNAL);

        when(mockThreadInfos.getTtidFromOtid(TTID_1_OTID_1_THREAD.getOriginalThreadId()))
                .thenReturn(OptionalInt.of(TTID_1_OTID_1_THREAD.getId()));

        List<ReadonlyEventInterface> beginThread1 = beginThread(THREAD_ID);
        List<ReadonlyEventInterface> readData1Thread1 = readData(THREAD_ID, NO_SIGNAL);

        List<ReadonlyEventInterface> beginSignal1 = beginSignal(
                SIGNAL_NUMBER, SIGNAL_HANDLER_ADDRESS, THREAD_ID, ONE_SIGNAL);
        List<ReadonlyEventInterface> readDataSignal1 = readData(THREAD_ID, ONE_SIGNAL);
        List<ReadonlyEventInterface> endSignal1 = endSignal(THREAD_ID, ONE_SIGNAL);

        List<ReadonlyEventInterface> beginSignal2 = beginSignal(
                SIGNAL_NUMBER, SIGNAL_HANDLER_ADDRESS, THREAD_ID, ONE_SIGNAL);
        List<ReadonlyEventInterface> readDataSignal2 = readData(THREAD_ID, ONE_SIGNAL);
        List<ReadonlyEventInterface> endSignal2 = endSignal(THREAD_ID, ONE_SIGNAL);

        List<ReadonlyEventInterface> readData2Thread1 = readData(THREAD_ID, NO_SIGNAL);

        ListEventReader eventReader =
                new ListEventReader(Arrays.asList(
                        beginThread1, readData1Thread1,
                        beginSignal1, readDataSignal1, endSignal1,
                        beginSignal2, readDataSignal2, endSignal2,
                        readData2Thread1));

        TraceCache traceCache =
                TraceCache.createForTesting(
                        mockConfiguration, mockTraceState, mockLockGraph, Collections.singletonList(eventReader));

        Assert.assertEquals(mockTrace, traceCache.getTraceWindow());

        verify(mockTraceState).initNextTraceWindow(rawTraceArgumentCaptor.capture());

        List<RawTrace> rawTraces = rawTraceArgumentCaptor.getValue();
        MoreAsserts.assertNotNull(rawTraces);

        Assert.assertEquals(3, rawTraces.size());

        Assert.assertEquals(0, rawTraces.get(0).getSignalDepth());
        int eventIndex = 0;
        eventIndex = assertEqualEvents(beginThread1, rawTraces.get(0), eventIndex);
        eventIndex = assertEqualEvents(readData1Thread1, rawTraces.get(0), eventIndex);
        eventIndex = assertEqualEvents(readData2Thread1, rawTraces.get(0), eventIndex);
        Assert.assertEquals(eventIndex, rawTraces.get(0).size());

        Assert.assertEquals(1, rawTraces.get(1).getSignalDepth());
        eventIndex = 0;
        eventIndex = assertEqualEvents(beginSignal1, rawTraces.get(1), eventIndex);
        eventIndex = assertEqualEvents(readDataSignal1, rawTraces.get(1), eventIndex);
        eventIndex = assertEqualEvents(endSignal1, rawTraces.get(1), eventIndex);
        Assert.assertEquals(eventIndex, rawTraces.get(1).size());

        Assert.assertEquals(1, rawTraces.get(2).getSignalDepth());
        eventIndex = 0;
        eventIndex = assertEqualEvents(beginSignal2, rawTraces.get(2), eventIndex);
        eventIndex = assertEqualEvents(readDataSignal2, rawTraces.get(2), eventIndex);
        eventIndex = assertEqualEvents(endSignal2, rawTraces.get(2), eventIndex);
        Assert.assertEquals(eventIndex, rawTraces.get(2).size());
    }

    @Test
    public void usesPreviousThreadIdForContinuingThread() throws IOException {
        mockConfiguration.windowSize = 100;
        when(mockConfiguration.stacks()).thenReturn(false);
        when(mockConfiguration.isLLVMPrediction()).thenReturn(true);
        when(mockTraceState.initNextTraceWindow(any())).thenReturn(mockTrace);
        when(mockThreadInfos.getTtidFromOtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockThreadInfos.getTtidFromOtid(THREAD_ID)).thenReturn(OptionalInt.of(NEW_THREAD_ID));
        when(mockThreadInfos.getThreadInfo(NEW_THREAD_ID)).thenReturn(TTID_1_OTID_1_THREAD);

        List<ReadonlyEventInterface> beginThread1 = beginThread(THREAD_ID);
        List<ReadonlyEventInterface> readData1Thread1 = readData(THREAD_ID, NO_SIGNAL);

        ListEventReader eventReader =
                new ListEventReader(Arrays.asList(beginThread1, readData1Thread1));

        TraceCache traceCache =
                TraceCache.createForTesting(
                        mockConfiguration, mockTraceState, mockLockGraph, Collections.singletonList(eventReader));

        Assert.assertEquals(mockTrace, traceCache.getTraceWindow());

        verify(mockTraceState).initNextTraceWindow(rawTraceArgumentCaptor.capture());

        List<RawTrace> rawTraces = rawTraceArgumentCaptor.getValue();
        MoreAsserts.assertNotNull(rawTraces);

        Assert.assertEquals(1, rawTraces.size());

        Assert.assertEquals(NO_SIGNAL, rawTraces.get(0).getSignalDepth());
        Assert.assertEquals(NEW_THREAD_ID, rawTraces.get(0).getThreadInfo().getId());
        Assert.assertEquals(THREAD_ID, rawTraces.get(0).getThreadInfo().getOriginalThreadId());
    }

    @Test
    public void usesPreviousThreadIdAndSignalNumberForContinuingSignal() throws IOException {
        mockConfiguration.windowSize = 100;
        when(mockConfiguration.stacks()).thenReturn(false);
        when(mockConfiguration.isLLVMPrediction()).thenReturn(true);
        when(mockTraceState.initNextTraceWindow(any())).thenReturn(mockTrace);
        when(mockThreadInfos.getTtidFromOtid(anyLong())).thenReturn(OptionalInt.empty());

        when(mockTraceState.getTtidForThreadOngoingAtWindowStart(THREAD_ID, ONE_SIGNAL))
                .thenReturn(OptionalInt.of(NEW_THREAD_ID_3));
        when(mockThreadInfos.getThreadInfo(NEW_THREAD_ID_3)).thenReturn(TTID_3_OTID_1_SIGNAL);

        List<ReadonlyEventInterface> readData = readData(THREAD_ID, ONE_SIGNAL);

        ListEventReader eventReader =
                new ListEventReader(Collections.singletonList(readData));

        TraceCache traceCache =
                TraceCache.createForTesting(
                        mockConfiguration, mockTraceState, mockLockGraph, Collections.singletonList(eventReader));

        Assert.assertEquals(mockTrace, traceCache.getTraceWindow());

        verify(mockTraceState).initNextTraceWindow(rawTraceArgumentCaptor.capture());

        List<RawTrace> rawTraces = rawTraceArgumentCaptor.getValue();
        MoreAsserts.assertNotNull(rawTraces);

        Assert.assertEquals(1, rawTraces.size());

        Assert.assertEquals(ONE_SIGNAL, rawTraces.get(0).getSignalDepth());
        Assert.assertEquals(NEW_THREAD_ID_3, rawTraces.get(0).getThreadInfo().getId());
        Assert.assertEquals(THREAD_ID, rawTraces.get(0).getThreadInfo().getOriginalThreadId());
        OptionalLong maybeSignalNumber = rawTraces.get(0).getThreadInfo().getSignalNumber();
        Assert.assertTrue(maybeSignalNumber.isPresent());
        Assert.assertEquals(SIGNAL_NUMBER, maybeSignalNumber.getAsLong());
    }

    @Test
    public void canProcessThreadWithIdLargerThanAThreadWithSignals() throws IOException, InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        mockConfiguration.windowSize = 100;
        when(mockConfiguration.stacks()).thenReturn(false);
        when(mockConfiguration.isLLVMPrediction()).thenReturn(true);
        when(mockTraceState.initNextTraceWindow(any())).thenReturn(mockTrace);

        when(mockTraceState.createAndRegisterSignalInfo(THREAD_ID, SIGNAL_NUMBER, SIGNAL_HANDLER_ADDRESS, ONE_SIGNAL))
                .thenReturn(TTID_3_OTID_1_SIGNAL);
        when(mockThreadInfos.getTtidFromOtid(THREAD_ID)).thenReturn(OptionalInt.of(NEW_THREAD_ID));
        when(mockThreadInfos.getThreadInfo(NEW_THREAD_ID)).thenReturn(TTID_1_OTID_1_THREAD);
        when(mockThreadInfos.getTtidFromOtid(THREAD_ID_2)).thenReturn(OptionalInt.of(NEW_THREAD_ID_2));
        when(mockThreadInfos.getThreadInfo(NEW_THREAD_ID_2)).thenReturn(TTID_2_OTID_2_THREAD);

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.nonAtomicLoad(DATA_ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER, SIGNAL_HANDLER_ADDRESS, GENERATION),
                tu.nonAtomicLoad(DATA_ADDRESS_1, VALUE_1),
                tu.exitSignal(),

                tu.switchThread(THREAD_ID_2, NO_SIGNAL),
                tu.nonAtomicLoad(DATA_ADDRESS_1, VALUE_1)
        );

        ListEventReader eventReader = new ListEventReader(events);

        TraceCache traceCache =
                TraceCache.createForTesting(
                        mockConfiguration, mockTraceState, mockLockGraph, Collections.singletonList(eventReader));

        Assert.assertEquals(mockTrace, traceCache.getTraceWindow());

        verify(mockTraceState).initNextTraceWindow(rawTraceArgumentCaptor.capture());

        List<RawTrace> rawTraces = rawTraceArgumentCaptor.getValue();
        MoreAsserts.assertNotNull(rawTraces);

        Assert.assertEquals(3, rawTraces.size());

        Assert.assertEquals(NO_SIGNAL, rawTraces.get(0).getSignalDepth());
        Assert.assertEquals(NEW_THREAD_ID, rawTraces.get(0).getThreadInfo().getId());
        Assert.assertEquals(THREAD_ID, rawTraces.get(0).getThreadInfo().getOriginalThreadId());

        Assert.assertEquals(ONE_SIGNAL, rawTraces.get(1).getSignalDepth());
        Assert.assertEquals(NEW_THREAD_ID_3, rawTraces.get(1).getThreadInfo().getId());
        Assert.assertEquals(THREAD_ID, rawTraces.get(1).getThreadInfo().getOriginalThreadId());

        Assert.assertEquals(NO_SIGNAL, rawTraces.get(2).getSignalDepth());
        Assert.assertEquals(NEW_THREAD_ID_2, rawTraces.get(2).getThreadInfo().getId());
        Assert.assertEquals(THREAD_ID_2, rawTraces.get(2).getThreadInfo().getOriginalThreadId());
    }

    @Test
    public void canProcessSingleSignalExitEvent() throws IOException, InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        mockConfiguration.windowSize = 100;
        when(mockConfiguration.stacks()).thenReturn(false);
        when(mockConfiguration.isLLVMPrediction()).thenReturn(true);
        when(mockTraceState.initNextTraceWindow(any())).thenReturn(mockTrace);

        when(mockTraceState.getTtidForThreadOngoingAtWindowStart(THREAD_ID, ONE_SIGNAL))
                .thenReturn(OptionalInt.of(NEW_THREAD_ID_3));
        when(mockThreadInfos.getThreadInfo(NEW_THREAD_ID_3)).thenReturn(TTID_3_OTID_1_SIGNAL);
        when(mockTraceState.createAndRegisterSignalInfo(THREAD_ID, SIGNAL_NUMBER, SIGNAL_HANDLER_ADDRESS, ONE_SIGNAL))
                .thenReturn(TTID_4_OTID_1_SIGNAL);
        when(mockThreadInfos.getTtidFromOtid(THREAD_ID)).thenReturn(OptionalInt.of(NEW_THREAD_ID));
        when(mockThreadInfos.getThreadInfo(NEW_THREAD_ID)).thenReturn(TTID_1_OTID_1_THREAD);

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.nonAtomicLoad(DATA_ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_ID, ONE_SIGNAL),
                tu.exitSignal(),
                tu.enterSignal(SIGNAL_NUMBER, SIGNAL_HANDLER_ADDRESS, GENERATION),
                tu.nonAtomicLoad(DATA_ADDRESS_1, VALUE_1),
                tu.exitSignal());

        ListEventReader eventReader = new ListEventReader(events);

        TraceCache traceCache =
                TraceCache.createForTesting(
                        mockConfiguration, mockTraceState, mockLockGraph, Collections.singletonList(eventReader));

        Assert.assertEquals(mockTrace, traceCache.getTraceWindow());

        verify(mockTraceState).initNextTraceWindow(rawTraceArgumentCaptor.capture());

        List<RawTrace> rawTraces = rawTraceArgumentCaptor.getValue();
        MoreAsserts.assertNotNull(rawTraces);

        Assert.assertEquals(3, rawTraces.size());

        Assert.assertEquals(NO_SIGNAL, rawTraces.get(0).getSignalDepth());
        Assert.assertEquals(NEW_THREAD_ID, rawTraces.get(0).getThreadInfo().getId());
        Assert.assertEquals(THREAD_ID, rawTraces.get(0).getThreadInfo().getOriginalThreadId());
        Assert.assertEquals(1, rawTraces.get(0).size());

        Assert.assertEquals(ONE_SIGNAL, rawTraces.get(1).getSignalDepth());
        Assert.assertEquals(NEW_THREAD_ID_3, rawTraces.get(1).getThreadInfo().getId());
        Assert.assertEquals(THREAD_ID, rawTraces.get(1).getThreadInfo().getOriginalThreadId());
        Assert.assertEquals(1, rawTraces.get(1).size());

        Assert.assertEquals(ONE_SIGNAL, rawTraces.get(2).getSignalDepth());
        Assert.assertEquals(NEW_THREAD_ID_4, rawTraces.get(2).getThreadInfo().getId());
        Assert.assertEquals(THREAD_ID, rawTraces.get(2).getThreadInfo().getOriginalThreadId());
        Assert.assertEquals(6, rawTraces.get(2).size());
    }

    @Test
    public void registersNewThreads() throws IOException {
        mockConfiguration.windowSize = 10;
        when(mockConfiguration.stacks()).thenReturn(false);
        when(mockConfiguration.isLLVMPrediction()).thenReturn(true);
        when(mockTraceState.initNextTraceWindow(any())).thenReturn(mockTrace);
        when(mockThreadInfos.getTtidFromOtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockTraceState.createAndRegisterThreadInfo(THREAD_ID, OptionalInt.empty()))
                .thenReturn(TTID_1_OTID_1_THREAD);
        when(mockTraceState.createAndRegisterThreadInfo(THREAD_ID_2, OptionalInt.empty()))
                .thenReturn(TTID_2_OTID_2_THREAD);

        when(mockThreadInfos.getThreadInfo(TTID_1_OTID_1_THREAD.getId())).thenReturn(TTID_1_OTID_1_THREAD);
        when(mockThreadInfos.getThreadInfo(TTID_2_OTID_2_THREAD.getId())).thenReturn(TTID_2_OTID_2_THREAD);

        when(mockThreadInfos.getTtidFromOtid(TTID_1_OTID_1_THREAD.getOriginalThreadId()))
                .thenReturn(OptionalInt.of(TTID_1_OTID_1_THREAD.getId()));
        when(mockThreadInfos.getTtidFromOtid(TTID_2_OTID_2_THREAD.getOriginalThreadId()))
                .thenReturn(OptionalInt.empty());

        List<ReadonlyEventInterface> beginThread1 = beginThread(THREAD_ID);
        List<ReadonlyEventInterface> readData1Thread1 = readData(THREAD_ID, NO_SIGNAL);
        List<ReadonlyEventInterface> startThread2Thread1 = startThread(THREAD_ID, NO_SIGNAL, THREAD_ID_2);

        ListEventReader eventReader =
                new ListEventReader(Arrays.asList(
                        beginThread1, readData1Thread1, startThread2Thread1));

        TraceCache traceCache =
                TraceCache.createForTesting(
                        mockConfiguration, mockTraceState, mockLockGraph, Collections.singletonList(eventReader));

        Assert.assertEquals(mockTrace, traceCache.getTraceWindow());

        verify(mockTraceState).initNextTraceWindow(rawTraceArgumentCaptor.capture());

        List<RawTrace> rawTraces = rawTraceArgumentCaptor.getValue();
        MoreAsserts.assertNotNull(rawTraces);

        Assert.assertEquals(1, rawTraces.size());

        verify(mockTraceState).createAndRegisterThreadInfo(THREAD_ID_2, OptionalInt.of(TTID_1_OTID_1_THREAD.getId()));
    }

    private List<ReadonlyEventInterface> endSignal(long threadId, int signalDepth) {
        return Collections.singletonList(
                new CompactEvent(eventId++, locationId++, threadId, signalDepth, EventType.EXIT_SIGNAL) {}
        );
    }

    private List<ReadonlyEventInterface> beginSignal(
            long signalNumber, long signalHandlerAddress, long threadId, int signalDepth) {
        long locationId = this.locationId++;
        return Arrays.asList(
                new CompactEvent(eventId++, locationId, threadId, signalDepth, EventType.WRITE_LOCK) {
                    @Override
                    public long getSyncObject() {
                        return Constants.SIGNAL_LOCK_C;
                    }
                },
                new CompactEvent(eventId++, locationId, threadId, signalDepth, EventType.ENTER_SIGNAL) {
                    @Override
                    public long getSignalNumber() {
                        return signalNumber;
                    }

                    @Override
                    public long getSignalHandlerAddress() {
                        return signalHandlerAddress;
                    }
                },
                new CompactEvent(eventId++, locationId, threadId, signalDepth, EventType.WRITE_UNLOCK) {
                    @Override
                    public long getSyncObject() {
                        return Constants.SIGNAL_LOCK_C;
                    }
                }
        );
    }

    private List<ReadonlyEventInterface> startThread(long threadId, int signalDepth, long startedThread) {
        return Collections.singletonList(
                new CompactEvent(eventId++, locationId++, threadId, signalDepth, EventType.START_THREAD) {
                    @Override
                    public long getSyncedThreadId() {
                        return startedThread;
                    }
                }
        );
    }

    private List<ReadonlyEventInterface> readData(long threadId, int signalDepth) {
        return Collections.singletonList(
                new CompactEvent(eventId++, locationId++, threadId, signalDepth, EventType.READ) {}
        );
    }

    private List<ReadonlyEventInterface> beginThread(long threadId) {
        return Collections.singletonList(
                new CompactEvent(eventId++, locationId++, threadId, NO_SIGNAL, EventType.BEGIN_THREAD) {}
        );
    }

    private static class ListEventReader implements IEventReader {

        private final List<ReadonlyEventInterface> events;

        private int eventIndex;
        private ReadonlyEventInterface lastReadEvent;

        private ListEventReader(List<List<ReadonlyEventInterface>> events) throws IOException {
            this.events = new ArrayList<>();
            for (List<ReadonlyEventInterface> eventList : events) {
                this.events.addAll(eventList);
            }
            eventIndex = 0;
            readEvent();
        }

        @Override
        public ReadonlyEventInterface readEvent() throws IOException {
            if (eventIndex >= events.size()) {
                lastReadEvent = null;
                return null;
            }
            lastReadEvent = events.get(eventIndex);
            eventIndex++;
            return lastReadEvent;
        }

        @Override
        public ReadonlyEventInterface lastReadEvent() {
            return lastReadEvent;
        }

        @Override
        public void close() throws IOException {

        }
    }

    private int assertEqualEvents(List<ReadonlyEventInterface> events, RawTrace rawTrace, int eventIndex) {
        for (ReadonlyEventInterface event : events) {
            Assert.assertEquals(event, rawTrace.event(eventIndex));
            eventIndex++;
        }
        return eventIndex;
    }
}
