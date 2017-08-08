package com.runtimeverification.rvpredict.trace.producers.signals;

import com.google.common.collect.ImmutableSet;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.ThreadInfo;
import com.runtimeverification.rvpredict.trace.producers.base.MinEventIdForWindow;
import com.runtimeverification.rvpredict.trace.producers.base.RawTracesByTtid;
import com.runtimeverification.rvpredict.trace.producers.base.ThreadInfosComponent;
import com.runtimeverification.rvpredict.trace.producers.base.TtidSetDifference;
import com.runtimeverification.rvpredict.trace.producers.base.TtidsForCurrentWindow;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import static com.runtimeverification.rvpredict.testutils.ThreadInfosComponentUtils.fillMockThreadInfosComponentFromTraces;
import static com.runtimeverification.rvpredict.testutils.TtidSetDifferenceUtils.fillMockTtidSetDifference;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InterruptedEventsTest {
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final int TWO_SIGNALS = 2;
    private static final long SIGNAL_NUMBER_1 = 2L;
    private static final long THREAD_1 = 101L;
    private static final long PC_BASE = 201L;
    private static final long BASE_ID = 301L;
    private static final long ADDRESS_1 = 401L;
    private static final long VALUE_1 = 501L;
    private static final long SIGNAL_HANDLER_1 = 601L;
    private static final long GENERATION_1 = 701;

    @Mock private RawTracesByTtid mockRawTracesByTtid;
    @Mock private TtidsForCurrentWindow mockTtidsForCurrentWindow;
    @Mock private ThreadInfosComponent mockThreadInfosComponent;
    @Mock private TtidSetDifference mockThreadStartsInTheCurrentWindow;
    @Mock private TtidSetDifference mockThreadEndsInTheCurrentWindow;
    @Mock private MinEventIdForWindow mockMinEventIdForWindow;
    @Mock private Context mockContext;

    private TestProducerModule module;
    private int nextIdDelta = 0;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
        module = new TestProducerModule();
    }

    @Test
    public void computesEmptyMappingForEmptyInput() {
        ComputingProducerWrapper<InterruptedEvents> producer =
                createAndRegister(mockThreadStartsInTheCurrentWindow, mockThreadEndsInTheCurrentWindow);

        fillMockTtidSetDifference(mockThreadStartsInTheCurrentWindow);
        fillMockTtidSetDifference(mockThreadEndsInTheCurrentWindow);

        module.reset();

        Assert.assertTrue(producer.getComputed().getSignalNumberToTtidToNextMinInterruptedEventId().isEmpty());
    }

    @Test
    public void emptyMappingWithoutSignals() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        ComputingProducerWrapper<InterruptedEvents> producer = createAndRegister(
                mockThreadStartsInTheCurrentWindow, mockThreadEndsInTheCurrentWindow,
                tu.createRawTrace(tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        fillMockTtidSetDifference(mockThreadStartsInTheCurrentWindow);
        fillMockTtidSetDifference(mockThreadEndsInTheCurrentWindow);

        module.reset();

        Assert.assertTrue(producer.getComputed().getSignalNumberToTtidToNextMinInterruptedEventId().isEmpty());
    }

    @Test
    public void signalInterruptingAtEnd() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> e1;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.switchThread(THREAD_1, NO_SIGNAL),
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));

        RawTrace mainThread = tu.extractRawTrace(events, THREAD_1, NO_SIGNAL);
        RawTrace signal = tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL);

        ComputingProducerWrapper<InterruptedEvents> producer = createAndRegister(
                mockThreadStartsInTheCurrentWindow, mockThreadEndsInTheCurrentWindow,
                mainThread, signal);

        fillThreads(mockThreadStartsInTheCurrentWindow, signal);
        fillThreads(mockThreadEndsInTheCurrentWindow, signal);

        module.reset();

        Map<Long, Map<Integer, Long>> result = producer.getComputed().getSignalNumberToTtidToNextMinInterruptedEventId();
        Assert.assertEquals(1, result.size());
        Map<Integer, Long> ttidToId = result.get(SIGNAL_NUMBER_1);
        Assert.assertNotNull(ttidToId);
        Assert.assertEquals(1, ttidToId.size());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId() + 1,
                (long)ttidToId.get(mainThread.getThreadInfo().getId()));

        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal.getThreadInfo().getId()));
        OptionalLong interruptedEventId = producer.getComputed().getInterruptedEventId(signal.getThreadInfo().getId());
        Assert.assertTrue(interruptedEventId.isPresent());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId(),
                interruptedEventId.getAsLong());
    }

    @Test
    public void signalInterruptingInTheMiddleOfTheThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> e1;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.switchThread(THREAD_1, NO_SIGNAL),
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        RawTrace mainThread = tu.extractRawTrace(events, THREAD_1, NO_SIGNAL);
        RawTrace signal = tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL);

        ComputingProducerWrapper<InterruptedEvents> producer = createAndRegister(
                mockThreadStartsInTheCurrentWindow, mockThreadEndsInTheCurrentWindow,
                mainThread, signal);

        fillThreads(mockThreadStartsInTheCurrentWindow, signal);
        fillThreads(mockThreadEndsInTheCurrentWindow, signal);

        module.reset();

        Map<Long, Map<Integer, Long>> result = producer.getComputed().getSignalNumberToTtidToNextMinInterruptedEventId();
        Assert.assertEquals(1, result.size());
        Map<Integer, Long> ttidToId = result.get(SIGNAL_NUMBER_1);
        Assert.assertNotNull(ttidToId);
        Assert.assertEquals(1, ttidToId.size());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId() + 1,
                (long)ttidToId.get(mainThread.getThreadInfo().getId()));

        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal.getThreadInfo().getId()));
        OptionalLong interruptedEventId = producer.getComputed().getInterruptedEventId(signal.getThreadInfo().getId());
        Assert.assertTrue(interruptedEventId.isPresent());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId(),
                interruptedEventId.getAsLong());
    }

    @Test
    public void signalInterruptingBeforeTheFirstEventOfTheThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> e1;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        RawTrace mainThread = tu.extractRawTrace(events, THREAD_1, NO_SIGNAL);
        RawTrace signal = tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL);

        ComputingProducerWrapper<InterruptedEvents> producer = createAndRegister(
                mockThreadStartsInTheCurrentWindow, mockThreadEndsInTheCurrentWindow,
                mainThread, signal);

        fillThreads(mockThreadStartsInTheCurrentWindow, signal);
        fillThreads(mockThreadEndsInTheCurrentWindow, signal);

        module.reset();

        Map<Long, Map<Integer, Long>> result = producer.getComputed().getSignalNumberToTtidToNextMinInterruptedEventId();
        Assert.assertEquals(1, result.size());
        Map<Integer, Long> ttidToId = result.get(SIGNAL_NUMBER_1);
        Assert.assertNotNull(ttidToId);
        Assert.assertEquals(1, ttidToId.size());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId(), (long)ttidToId.get(mainThread.getThreadInfo().getId()));

        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal.getThreadInfo().getId()));
        OptionalLong interruptedEventId = producer.getComputed().getInterruptedEventId(signal.getThreadInfo().getId());
        Assert.assertFalse(interruptedEventId.isPresent());
    }

    @Test
    public void multipleSignalInterruptions() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> start1;
        List<ReadonlyEventInterface> end1;
        List<ReadonlyEventInterface> start2;
        List<ReadonlyEventInterface> end2;
        List<ReadonlyEventInterface> start3;
        List<ReadonlyEventInterface> end3;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.switchThread(THREAD_1, NO_SIGNAL),
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                start1 = tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                end1 = tu.exitSignal(),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                start2 = tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                end2 = tu.exitSignal(),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                e3 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                start3 = tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                end3 = tu.exitSignal(),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        RawTrace mainThread = tu.extractRawTrace(events, THREAD_1, NO_SIGNAL);
        RawTrace signal1 = tu.extractRawTrace(
                events, THREAD_1, ONE_SIGNAL,
                TraceUtils.extractFirstEvent(start1).getEventId(), TraceUtils.extractLastEvent(end1).getEventId());
        RawTrace signal2 = tu.extractRawTrace(
                events, THREAD_1, ONE_SIGNAL,
                TraceUtils.extractFirstEvent(start2).getEventId(), TraceUtils.extractLastEvent(end2).getEventId());
        RawTrace signal3 = tu.extractRawTrace(
                events, THREAD_1, ONE_SIGNAL,
                TraceUtils.extractFirstEvent(start3).getEventId(), TraceUtils.extractLastEvent(end3).getEventId());

        ComputingProducerWrapper<InterruptedEvents> producer = createAndRegister(
                mockThreadStartsInTheCurrentWindow, mockThreadEndsInTheCurrentWindow,
                mainThread, signal1, signal2, signal3);

        fillThreads(mockThreadStartsInTheCurrentWindow, signal1, signal2, signal3);
        fillThreads(mockThreadEndsInTheCurrentWindow, signal1, signal2, signal3);

        module.reset();

        Map<Long, Map<Integer, Long>> result = producer.getComputed().getSignalNumberToTtidToNextMinInterruptedEventId();
        Assert.assertEquals(1, result.size());
        Map<Integer, Long> ttidToId = result.get(SIGNAL_NUMBER_1);
        Assert.assertNotNull(ttidToId);
        Assert.assertEquals(1, ttidToId.size());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId() + 1,
                (long)ttidToId.get(mainThread.getThreadInfo().getId()));

        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal1.getThreadInfo().getId()));
        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal2.getThreadInfo().getId()));
        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal3.getThreadInfo().getId()));

        OptionalLong interruptedEventId = producer.getComputed().getInterruptedEventId(signal1.getThreadInfo().getId());
        Assert.assertTrue(interruptedEventId.isPresent());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId(),
                interruptedEventId.getAsLong());
        interruptedEventId = producer.getComputed().getInterruptedEventId(signal2.getThreadInfo().getId());
        Assert.assertTrue(interruptedEventId.isPresent());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e2).getEventId(),
                interruptedEventId.getAsLong());
        interruptedEventId = producer.getComputed().getInterruptedEventId(signal3.getThreadInfo().getId());
        Assert.assertTrue(interruptedEventId.isPresent());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e3).getEventId(),
                interruptedEventId.getAsLong());
    }

    @Test
    public void multipleSignalInterruptionsBeforeFirstEvent() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> start1;
        List<ReadonlyEventInterface> end1;
        List<ReadonlyEventInterface> start2;
        List<ReadonlyEventInterface> end2;
        List<ReadonlyEventInterface> start3;
        List<ReadonlyEventInterface> end3;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.switchThread(THREAD_1, ONE_SIGNAL),
                start1 = tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                end1 = tu.exitSignal(),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                start2 = tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                end2 = tu.exitSignal(),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                start3 = tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                end3 = tu.exitSignal(),


                tu.switchThread(THREAD_1, NO_SIGNAL),
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        RawTrace mainThread = tu.extractRawTrace(events, THREAD_1, NO_SIGNAL);
        RawTrace signal1 = tu.extractRawTrace(
                events, THREAD_1, ONE_SIGNAL,
                TraceUtils.extractFirstEvent(start1).getEventId(), TraceUtils.extractLastEvent(end1).getEventId());
        RawTrace signal2 = tu.extractRawTrace(
                events, THREAD_1, ONE_SIGNAL,
                TraceUtils.extractFirstEvent(start2).getEventId(), TraceUtils.extractLastEvent(end2).getEventId());
        RawTrace signal3 = tu.extractRawTrace(
                events, THREAD_1, ONE_SIGNAL,
                TraceUtils.extractFirstEvent(start3).getEventId(), TraceUtils.extractLastEvent(end3).getEventId());

        ComputingProducerWrapper<InterruptedEvents> producer = createAndRegister(
                mockThreadStartsInTheCurrentWindow, mockThreadEndsInTheCurrentWindow,
                mainThread, signal1, signal2, signal3);

        fillThreads(mockThreadStartsInTheCurrentWindow, signal1, signal2, signal3);
        fillThreads(mockThreadEndsInTheCurrentWindow, signal1, signal2, signal3);

        module.reset();

        Map<Long, Map<Integer, Long>> result = producer.getComputed().getSignalNumberToTtidToNextMinInterruptedEventId();
        Assert.assertEquals(1, result.size());
        Map<Integer, Long> ttidToId = result.get(SIGNAL_NUMBER_1);
        Assert.assertNotNull(ttidToId);
        Assert.assertEquals(1, ttidToId.size());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId(),
                (long)ttidToId.get(mainThread.getThreadInfo().getId()));

        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal1.getThreadInfo().getId()));
        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal2.getThreadInfo().getId()));
        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal3.getThreadInfo().getId()));

        OptionalLong interruptedEventId = producer.getComputed().getInterruptedEventId(signal1.getThreadInfo().getId());
        Assert.assertFalse(interruptedEventId.isPresent());
        interruptedEventId = producer.getComputed().getInterruptedEventId(signal2.getThreadInfo().getId());
        Assert.assertFalse(interruptedEventId.isPresent());
        interruptedEventId = producer.getComputed().getInterruptedEventId(signal3.getThreadInfo().getId());
        Assert.assertFalse(interruptedEventId.isPresent());
    }

    @Test
    public void signalInterruptsTheRightSignal() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> start1;
        List<ReadonlyEventInterface> end1;
        List<ReadonlyEventInterface> start2;
        List<ReadonlyEventInterface> end2;
        List<ReadonlyEventInterface> start3;
        List<ReadonlyEventInterface> end3;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.switchThread(THREAD_1, NO_SIGNAL),
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                start1 = tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                end1 = tu.exitSignal(),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                start2 = tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, TWO_SIGNALS),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal(),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                end2 = tu.exitSignal(),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                start3 = tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                end3 = tu.exitSignal(),

                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1)
        );

        RawTrace mainThread = tu.extractRawTrace(events, THREAD_1, NO_SIGNAL);
        RawTrace signal1 = tu.extractRawTrace(
                events, THREAD_1, ONE_SIGNAL,
                TraceUtils.extractFirstEvent(start1).getEventId(), TraceUtils.extractLastEvent(end1).getEventId());
        RawTrace signal2 = tu.extractRawTrace(
                events, THREAD_1, ONE_SIGNAL,
                TraceUtils.extractFirstEvent(start2).getEventId(), TraceUtils.extractLastEvent(end2).getEventId());
        RawTrace signal3 = tu.extractRawTrace(
                events, THREAD_1, ONE_SIGNAL,
                TraceUtils.extractFirstEvent(start3).getEventId(), TraceUtils.extractLastEvent(end3).getEventId());
        RawTrace signal = tu.extractRawTrace(events, THREAD_1, TWO_SIGNALS);

        ComputingProducerWrapper<InterruptedEvents> producer = createAndRegister(
                mockThreadStartsInTheCurrentWindow, mockThreadEndsInTheCurrentWindow,
                mainThread, signal1, signal2, signal3, signal);

        fillThreads(mockThreadStartsInTheCurrentWindow, signal1, signal2, signal3, signal);
        fillThreads(mockThreadEndsInTheCurrentWindow, signal1, signal2, signal3, signal);

        module.reset();

        Map<Long, Map<Integer, Long>> result = producer.getComputed().getSignalNumberToTtidToNextMinInterruptedEventId();
        Assert.assertEquals(1, result.size());
        Map<Integer, Long> ttidToId = result.get(SIGNAL_NUMBER_1);
        Assert.assertNotNull(ttidToId);
        Assert.assertEquals(2, ttidToId.size());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId() + 1,
                (long)ttidToId.get(mainThread.getThreadInfo().getId()));
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e2).getEventId() + 1,
                (long)ttidToId.get(signal2.getThreadInfo().getId()));

        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal1.getThreadInfo().getId()));
        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal2.getThreadInfo().getId()));
        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal3.getThreadInfo().getId()));
        Assert.assertEquals(
                signal2.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal.getThreadInfo().getId()));

        OptionalLong interruptedEventId = producer.getComputed().getInterruptedEventId(signal1.getThreadInfo().getId());
        Assert.assertTrue(interruptedEventId.isPresent());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId(),
                interruptedEventId.getAsLong());
        interruptedEventId = producer.getComputed().getInterruptedEventId(signal2.getThreadInfo().getId());
        Assert.assertTrue(interruptedEventId.isPresent());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId(),
                interruptedEventId.getAsLong());
        interruptedEventId = producer.getComputed().getInterruptedEventId(signal3.getThreadInfo().getId());
        Assert.assertTrue(interruptedEventId.isPresent());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId(),
                interruptedEventId.getAsLong());
        interruptedEventId = producer.getComputed().getInterruptedEventId(signal.getThreadInfo().getId());
        Assert.assertTrue(interruptedEventId.isPresent());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e2).getEventId(),
                interruptedEventId.getAsLong());
    }

    @Test
    public void signalInterruptsSignalThatStartsInPreviousWindow() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.switchThread(THREAD_1, TWO_SIGNALS),
                e1 = tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal(),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal()
        );

        RawTrace mainThreadPreviousWindow = tu.createRawTrace(
                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        RawTrace signal1PreviousWindow = tu.createRawTrace(
                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1));

        RawTrace signal1 = tu.extractRawTrace(false, events, THREAD_1, ONE_SIGNAL);
        RawTrace signal = tu.extractRawTrace(events, THREAD_1, TWO_SIGNALS);

        ComputingProducerWrapper<InterruptedEvents> producer = createAndRegister(
                mockTtidsForCurrentWindow,
                mockThreadStartsInTheCurrentWindow, mockThreadEndsInTheCurrentWindow,
                signal1, signal);

        fillMockThreadInfosComponentFromTraces(
                mockThreadInfosComponent, mainThreadPreviousWindow, signal1PreviousWindow, signal1, signal);
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(ImmutableSet.of(
                mainThreadPreviousWindow.getThreadInfo().getId(),
                signal.getThreadInfo().getId(),
                signal1.getThreadInfo().getId()));
        fillThreads(mockThreadStartsInTheCurrentWindow, signal);
        fillThreads(mockThreadEndsInTheCurrentWindow, signal1, signal);

        module.reset();

        Map<Long, Map<Integer, Long>> result = producer.getComputed().getSignalNumberToTtidToNextMinInterruptedEventId();
        Assert.assertEquals(1, result.size());
        Map<Integer, Long> ttidToId = result.get(SIGNAL_NUMBER_1);
        Assert.assertNotNull(ttidToId);
        Assert.assertEquals(2, ttidToId.size());
        Assert.assertEquals(
                TraceUtils.extractFirstEvent(e1).getEventId(),
                (long)ttidToId.get(mainThreadPreviousWindow.getThreadInfo().getId()));
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e2).getEventId(),
                (long)ttidToId.get(signal1.getThreadInfo().getId()));

        Assert.assertEquals(
                mainThreadPreviousWindow.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal1.getThreadInfo().getId()));
        Assert.assertEquals(
                signal1.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal.getThreadInfo().getId()));

        OptionalLong interruptedEventId = producer.getComputed().getInterruptedEventId(signal1.getThreadInfo().getId());
        Assert.assertFalse(interruptedEventId.isPresent());
        interruptedEventId = producer.getComputed().getInterruptedEventId(signal.getThreadInfo().getId());
        Assert.assertFalse(interruptedEventId.isPresent());
    }

    @Test
    public void signalInterruptsSignalThatEndsInNextWindow() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.switchThread(THREAD_1, NO_SIGNAL),
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                e2 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),

                tu.switchThread(THREAD_1, TWO_SIGNALS),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal()
        );

        RawTrace mainThread = tu.extractRawTrace(events, THREAD_1, NO_SIGNAL);
        RawTrace signal1 = tu.extractRawTrace(events, THREAD_1, ONE_SIGNAL);
        RawTrace signal = tu.extractRawTrace(events, THREAD_1, TWO_SIGNALS);

        ComputingProducerWrapper<InterruptedEvents> producer = createAndRegister(
                mockThreadStartsInTheCurrentWindow, mockThreadEndsInTheCurrentWindow,
                mainThread, signal1, signal);

        fillThreads(mockThreadStartsInTheCurrentWindow, signal1, signal);
        fillThreads(mockThreadEndsInTheCurrentWindow, signal);

        module.reset();

        Map<Long, Map<Integer, Long>> result = producer.getComputed().getSignalNumberToTtidToNextMinInterruptedEventId();
        Assert.assertEquals(1, result.size());
        Map<Integer, Long> ttidToId = result.get(SIGNAL_NUMBER_1);
        Assert.assertNotNull(ttidToId);
        Assert.assertEquals(2, ttidToId.size());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId() + 1,
                (long)ttidToId.get(mainThread.getThreadInfo().getId()));
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e2).getEventId() + 1,
                (long)ttidToId.get(signal1.getThreadInfo().getId()));

        Assert.assertEquals(
                mainThread.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal1.getThreadInfo().getId()));
        Assert.assertEquals(
                signal1.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal.getThreadInfo().getId()));

        OptionalLong interruptedEventId = producer.getComputed().getInterruptedEventId(signal1.getThreadInfo().getId());
        Assert.assertTrue(interruptedEventId.isPresent());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e1).getEventId(),
                interruptedEventId.getAsLong());
        interruptedEventId = producer.getComputed().getInterruptedEventId(signal.getThreadInfo().getId());
        Assert.assertTrue(interruptedEventId.isPresent());
        Assert.assertEquals(
                TraceUtils.extractSingleEvent(e2).getEventId(),
                interruptedEventId.getAsLong());
    }

    @Test
    public void signalInterruptsSignalWithoutEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> e1;

        List<List<ReadonlyEventInterface>> events = Arrays.asList(
                tu.switchThread(THREAD_1, TWO_SIGNALS),
                e1 = tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.exitSignal());

        RawTrace mainThreadPreviousWindow = tu.createRawTrace(
                tu.switchThread(THREAD_1, NO_SIGNAL),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1));
        RawTrace signal1PreviousWindow = tu.createRawTrace(
                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1));

        RawTrace signal = tu.extractRawTrace(events, THREAD_1, TWO_SIGNALS);

        ComputingProducerWrapper<InterruptedEvents> producer = createAndRegister(
                mockTtidsForCurrentWindow,
                mockThreadStartsInTheCurrentWindow, mockThreadEndsInTheCurrentWindow,
                signal);

        fillMockThreadInfosComponentFromTraces(
                mockThreadInfosComponent, mainThreadPreviousWindow, signal1PreviousWindow, signal);
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(ImmutableSet.of(
                mainThreadPreviousWindow.getThreadInfo().getId(),
                signal1PreviousWindow.getThreadInfo().getId(),
                signal.getThreadInfo().getId()));
        fillThreads(mockThreadStartsInTheCurrentWindow, signal);
        fillThreads(mockThreadEndsInTheCurrentWindow, signal);

        module.reset();

        Map<Long, Map<Integer, Long>> result = producer.getComputed().getSignalNumberToTtidToNextMinInterruptedEventId();
        Assert.assertEquals(1, result.size());
        Map<Integer, Long> ttidToId = result.get(SIGNAL_NUMBER_1);
        Assert.assertNotNull(ttidToId);
        Assert.assertEquals(2, ttidToId.size());
        Assert.assertEquals(
                TraceUtils.extractFirstEvent(e1).getEventId(),
                (long)ttidToId.get(mainThreadPreviousWindow.getThreadInfo().getId()));
        Assert.assertEquals(
                TraceUtils.extractFirstEvent(e1).getEventId(),
                (long)ttidToId.get(signal1PreviousWindow.getThreadInfo().getId()));

        Assert.assertEquals(
                mainThreadPreviousWindow.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal1PreviousWindow.getThreadInfo().getId()));
        Assert.assertEquals(
                signal1PreviousWindow.getThreadInfo().getId(),
                producer.getComputed().getInterruptedTtid(signal.getThreadInfo().getId()));

        OptionalLong interruptedEventId =
                producer.getComputed().getInterruptedEventId(signal1PreviousWindow.getThreadInfo().getId());
        Assert.assertFalse(interruptedEventId.isPresent());
        interruptedEventId = producer.getComputed().getInterruptedEventId(signal.getThreadInfo().getId());
        Assert.assertFalse(interruptedEventId.isPresent());
    }

    // more tests needed.

    private void fillThreads(TtidSetDifference mockThreadSetDifference, RawTrace... threads) {
        fillMockTtidSetDifference(
                mockThreadSetDifference,
                Arrays.stream(threads).map(thread -> thread.getThreadInfo().getId()).mapToInt(id -> id).toArray());
    }

    private void fillTtidsForCurrentWindowFromTraces(
            TtidsForCurrentWindow mockTtidsForCurrentWindow, RawTrace... rawTraces) {
        when(mockTtidsForCurrentWindow.getTtids()).thenReturn(
                Arrays.stream(rawTraces).map(trace -> trace.getThreadInfo().getId()).collect(Collectors.toList()));
    }

    private ComputingProducerWrapper<InterruptedEvents> createAndRegister(
            TtidSetDifference threadStartsInTheCurrentWindow,
            TtidSetDifference threadEndsInTheCurrentWindow,
            RawTrace... rawTraces) {
        fillTtidsForCurrentWindowFromTraces(mockTtidsForCurrentWindow, rawTraces);
        fillMockThreadInfosComponentFromTraces(mockThreadInfosComponent, rawTraces);
        return createAndRegister(
                mockTtidsForCurrentWindow,
                threadStartsInTheCurrentWindow, threadEndsInTheCurrentWindow,
                rawTraces);
    }

    private ComputingProducerWrapper<InterruptedEvents> createAndRegister(
            TtidsForCurrentWindow ttidsForCurrentWindow,
            TtidSetDifference threadStartsInTheCurrentWindow,
            TtidSetDifference threadEndsInTheCurrentWindow,
            RawTrace... rawTraces) {
        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        fillMockThreadInfosComponentFromTraces(mockThreadInfosComponent, rawTraces);
        for (RawTrace rawTrace : rawTraces) {
            ThreadInfo threadInfo = rawTrace.getThreadInfo();
            when(mockRawTracesByTtid.getRawTrace(threadInfo.getId())).thenReturn(Optional.of(rawTrace));
        }
        when(mockMinEventIdForWindow.getId()).thenReturn(
                Arrays.stream(rawTraces).mapToLong(RawTrace::getMinGID).min());
        return createAndRegister(
                mockRawTracesByTtid, ttidsForCurrentWindow, mockThreadInfosComponent,
                threadStartsInTheCurrentWindow, threadEndsInTheCurrentWindow,
                mockMinEventIdForWindow);
    }

    private ComputingProducerWrapper<InterruptedEvents> createAndRegister(
            RawTracesByTtid rawTracesByTtid,
            TtidsForCurrentWindow ttidsForCurrentWindow,
            ThreadInfosComponent threadInfosComponent,
            TtidSetDifference threadStartsInTheCurrentWindow,
            TtidSetDifference threadEndsInTheCurrentWindow,
            MinEventIdForWindow minEventIdForWindow) {
        ComputingProducerWrapper<RawTracesByTtid> rawTracesByTtidWrapper =
                new ComputingProducerWrapper<>(rawTracesByTtid, module);
        ComputingProducerWrapper<TtidsForCurrentWindow> ttidsForCurrentWindowWrapper =
                new ComputingProducerWrapper<>(ttidsForCurrentWindow, module);
        ComputingProducerWrapper<ThreadInfosComponent> threadInfosComponentWrapper =
                new ComputingProducerWrapper<>(threadInfosComponent, module);
        ComputingProducerWrapper<TtidSetDifference> threadStartsInTheCurrentWindowWrapper =
                new ComputingProducerWrapper<>(threadStartsInTheCurrentWindow, module);
        ComputingProducerWrapper<TtidSetDifference> threadEndsInTheCurrentWindowWrapper =
                new ComputingProducerWrapper<>(threadEndsInTheCurrentWindow, module);
        ComputingProducerWrapper<MinEventIdForWindow> minEventIdForWindowWrapper =
                new ComputingProducerWrapper<>(minEventIdForWindow, module);
        return new ComputingProducerWrapper<>(new InterruptedEvents(
                rawTracesByTtidWrapper, ttidsForCurrentWindowWrapper, threadInfosComponentWrapper,
                threadStartsInTheCurrentWindowWrapper, threadEndsInTheCurrentWindowWrapper,
                minEventIdForWindowWrapper), module);
    }
}
