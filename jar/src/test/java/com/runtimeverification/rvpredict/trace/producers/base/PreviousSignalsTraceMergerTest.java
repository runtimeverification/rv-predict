package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.producers.signals.ThreadsWhereSignalIsEnabled;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasSize;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmpty;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PreviousSignalsTraceMergerTest {
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final long SIGNAL_NUMBER_1 = 1L;
    private static final long SIGNAL_NUMBER_2 = 2L;
    private static final int TTID_1 = 101;
    private static final long THREAD_1 = 301L;
    private static final long PC_BASE = 401L;
    private static final long BASE_ID = 501L;
    private static final long ADDRESS_1 = 601L;
    private static final long VALUE_1 = 701L;
    private static final long SIGNAL_HANDLER_1 = 801;
    private static final long GENERATION_1 = 901;

    @Mock private RawTraces mockCurrentWindowRawTraces;
    @Mock private RawTraces mockPreviousRawTraces;
    @Mock private ThreadsWhereSignalIsEnabled mockThreadsWhereSignalIsEnabled;
    @Mock private DesiredThreadCountForSignal mockDesiredThreadCountForSignal;

    @Mock private Context mockContext;

    private final TestProducerModule module = new TestProducerModule();

    private int nextIdDelta = 0;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
    }

    @Test
    public void emptyOutputForEmptyInput() {
        ComputingProducerWrapper<PreviousSignalsTraceMerger> producer =
                initProducer(
                        module,
                        mockCurrentWindowRawTraces,
                        mockPreviousRawTraces,
                        mockThreadsWhereSignalIsEnabled,
                        mockDesiredThreadCountForSignal);

        when(mockCurrentWindowRawTraces.getTraces()).thenReturn(Collections.emptyList());
        when(mockPreviousRawTraces.getTraces()).thenReturn(Collections.emptyList());
        when(mockThreadsWhereSignalIsEnabled.threadsForSignal(anyLong())).thenReturn(Collections.emptyList());
        when(mockDesiredThreadCountForSignal.getCount()).thenReturn(0);

        module.reset();

        Assert.assertThat(
                producer.getComputed().getTraces(),
                isEmpty());
    }

    @Test
    public void addingOneTrace() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<PreviousSignalsTraceMerger> producer =
                initProducer(
                        module,
                        mockCurrentWindowRawTraces,
                        mockPreviousRawTraces,
                        mockThreadsWhereSignalIsEnabled,
                        mockDesiredThreadCountForSignal);

        RawTrace currentRawTrace = tu.createRawTrace(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        RawTrace signalRawTrace = tu.createRawTrace(
                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        when(mockCurrentWindowRawTraces.getTraces()).thenReturn(Collections.singletonList(currentRawTrace));
        when(mockPreviousRawTraces.getTraces()).thenReturn(Collections.singletonList(signalRawTrace));
        when(mockThreadsWhereSignalIsEnabled.threadsForSignal(anyLong())).thenReturn(Collections.emptyList());
        when(mockThreadsWhereSignalIsEnabled.threadsForSignal(SIGNAL_NUMBER_1))
                .thenReturn(Collections.singletonList(TTID_1));


        when(mockDesiredThreadCountForSignal.getCount()).thenReturn(1);
        module.reset();
        Assert.assertThat(
                producer.getComputed().getTraces(),
                hasSize(2));

        when(mockDesiredThreadCountForSignal.getCount()).thenReturn(0);
        module.reset();
        Assert.assertThat(
                producer.getComputed().getTraces(),
                hasSize(1));
    }

    @Test
    public void addingOneTraceOverExistingOne() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<PreviousSignalsTraceMerger> producer =
                initProducer(
                        module,
                        mockCurrentWindowRawTraces,
                        mockPreviousRawTraces,
                        mockThreadsWhereSignalIsEnabled,
                        mockDesiredThreadCountForSignal);

        RawTrace currentRawTrace = tu.createRawTrace(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        RawTrace currentSignalRawTrace = tu.createRawTrace(
                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        RawTrace formerSignalRawTrace = tu.createRawTrace(
                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        when(mockCurrentWindowRawTraces.getTraces()).thenReturn(Arrays.asList(currentRawTrace, currentSignalRawTrace));
        when(mockPreviousRawTraces.getTraces()).thenReturn(Collections.singletonList(formerSignalRawTrace));
        when(mockThreadsWhereSignalIsEnabled.threadsForSignal(anyLong())).thenReturn(Collections.emptyList());
        when(mockThreadsWhereSignalIsEnabled.threadsForSignal(SIGNAL_NUMBER_1))
                .thenReturn(Collections.singletonList(TTID_1));

        // Adds a signal.
        when(mockDesiredThreadCountForSignal.getCount()).thenReturn(2);
        module.reset();
        Assert.assertThat(
                producer.getComputed().getTraces(),
                hasSize(3));

        // Does not add signals when over the limit.
        when(mockDesiredThreadCountForSignal.getCount()).thenReturn(1);
        module.reset();
        Assert.assertThat(
                producer.getComputed().getTraces(),
                hasSize(2));

        // Does not remove existing signals.
        when(mockDesiredThreadCountForSignal.getCount()).thenReturn(0);
        module.reset();
        Assert.assertThat(
                producer.getComputed().getTraces(),
                hasSize(2));
    }

    @Test
    public void addingOneTraceInParallelExistingOne() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<PreviousSignalsTraceMerger> producer =
                initProducer(
                        module,
                        mockCurrentWindowRawTraces,
                        mockPreviousRawTraces,
                        mockThreadsWhereSignalIsEnabled,
                        mockDesiredThreadCountForSignal);

        RawTrace currentRawTrace = tu.createRawTrace(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        RawTrace currentSignalRawTrace = tu.createRawTrace(
                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        RawTrace formerSignalRawTrace = tu.createRawTrace(
                tu.switchThread(THREAD_1, ONE_SIGNAL),
                tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_1, GENERATION_1),
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        when(mockCurrentWindowRawTraces.getTraces()).thenReturn(Arrays.asList(currentRawTrace, currentSignalRawTrace));
        when(mockPreviousRawTraces.getTraces()).thenReturn(Collections.singletonList(formerSignalRawTrace));
        when(mockThreadsWhereSignalIsEnabled.threadsForSignal(anyLong())).thenReturn(Collections.emptyList());
        when(mockThreadsWhereSignalIsEnabled.threadsForSignal(SIGNAL_NUMBER_1))
                .thenReturn(Collections.singletonList(TTID_1));
        when(mockThreadsWhereSignalIsEnabled.threadsForSignal(SIGNAL_NUMBER_2))
                .thenReturn(Collections.singletonList(TTID_1));

        // Adds a signal.
        when(mockDesiredThreadCountForSignal.getCount()).thenReturn(1);
        module.reset();
        Assert.assertThat(
                producer.getComputed().getTraces(),
                hasSize(3));

        // Does not increase the signal count when over the limit.
        when(mockDesiredThreadCountForSignal.getCount()).thenReturn(0);
        module.reset();
        Assert.assertThat(
                producer.getComputed().getTraces(),
                hasSize(2));

        // Does not add signals when not enabled.
        when(mockDesiredThreadCountForSignal.getCount()).thenReturn(1);
        when(mockThreadsWhereSignalIsEnabled.threadsForSignal(SIGNAL_NUMBER_2))
                .thenReturn(Collections.emptyList());
        module.reset();
        Assert.assertThat(
                producer.getComputed().getTraces(),
                hasSize(2));
    }

    private static ComputingProducerWrapper<PreviousSignalsTraceMerger> initProducer(
            TestProducerModule module,
            RawTraces currentWindowRawTrace,
            RawTraces previousRawTrace,
            ThreadsWhereSignalIsEnabled threadsWhereSignalIsEnabled,
            DesiredThreadCountForSignal desiredThreadCountForSignal) {
        return new ComputingProducerWrapper<>(
                new PreviousSignalsTraceMerger(
                        new ComputingProducerWrapper<>(currentWindowRawTrace, module),
                        new ComputingProducerWrapper<>(previousRawTrace, module),
                        new ComputingProducerWrapper<>(threadsWhereSignalIsEnabled, module),
                        new ComputingProducerWrapper<>(desiredThreadCountForSignal, module)),
                module);
    }
}
