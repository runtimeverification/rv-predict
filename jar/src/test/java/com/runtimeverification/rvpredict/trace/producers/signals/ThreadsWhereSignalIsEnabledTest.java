package com.runtimeverification.rvpredict.trace.producers.signals;

import com.google.common.collect.ImmutableMap;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.testutils.SignalMaskForEventsUtils;
import com.runtimeverification.rvpredict.testutils.SignalMasksAtWindowStartUtils;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.producers.base.RawTracesByTtid;
import com.runtimeverification.rvpredict.trace.producers.base.TtidSetLeaf;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsExactly;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmpty;
import static com.runtimeverification.rvpredict.testutils.TtidSetLeafUtils.fillMockTtidSetLeaf;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ThreadsWhereSignalIsEnabledTest {
    private static final int NO_SIGNAL = 0;
    private static final long SIGNAL_NUMBER_1 = 1L;
    private static final int TTID_1 = 101;
    private static final long EVENT_ID_1 = 201L;
    private static final long THREAD_1 = 301L;
    private static final long PC_BASE = 401L;
    private static final long BASE_ID = 501L;
    private static final long ADDRESS_1 = 601L;
    private static final long VALUE_1 = 701L;

    @Mock private SignalMaskAtWindowStart<? extends ProducerState> mockSignalMaskAtWindowStart;
    @Mock private SignalMaskForEvents mockSignalMaskForEvents;
    @Mock private TtidSetLeaf mockAllTtids;
    @Mock private RawTracesByTtid mockRawTracesByTtid;

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
        ComputingProducerWrapper<ThreadsWhereSignalIsEnabled> producer =
                initProducer(
                        module,
                        mockSignalMaskForEvents,
                        mockAllTtids,
                        mockRawTracesByTtid,
                        mockSignalMaskAtWindowStart);

        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        fillMockTtidSetLeaf(mockAllTtids/*, 1, 2, 3*/);
        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        SignalMasksAtWindowStartUtils.clearMockSignalMasksAtWindowStart(mockSignalMaskAtWindowStart);

        module.reset();

        Assert.assertThat(
                producer.getComputed().threadsForSignal(SIGNAL_NUMBER_1),
                isEmpty());
    }

    @Test
    public void emptyOutputWhenEmptyTraceSignalNotEnabledExplicitly() {
        ComputingProducerWrapper<ThreadsWhereSignalIsEnabled> producer =
                initProducer(
                        module,
                        mockSignalMaskForEvents,
                        mockAllTtids,
                        mockRawTracesByTtid,
                        mockSignalMaskAtWindowStart);

        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        fillMockTtidSetLeaf(mockAllTtids, TTID_1);
        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());

        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK));

        module.reset();

        Assert.assertThat(
                producer.getComputed().threadsForSignal(SIGNAL_NUMBER_1),
                isEmpty());
    }

    @Test
    public void ttidIncludedWhenEmptyTraceSignalEnabled() {
        ComputingProducerWrapper<ThreadsWhereSignalIsEnabled> producer =
                initProducer(
                        module,
                        mockSignalMaskForEvents,
                        mockAllTtids,
                        mockRawTracesByTtid,
                        mockSignalMaskAtWindowStart);

        SignalMaskForEventsUtils.clearMockSignalMaskForEvents(mockSignalMaskForEvents);
        fillMockTtidSetLeaf(mockAllTtids, TTID_1);
        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());

        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_1, EVENT_ID_1)));

        module.reset();

        Assert.assertThat(
                producer.getComputed().threadsForSignal(SIGNAL_NUMBER_1),
                containsExactly(TTID_1));
    }

    @Test
    public void emptyOutputWhenSignalNotEnabledExplicitly() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<ThreadsWhereSignalIsEnabled> producer =
                initProducer(
                        module,
                        mockSignalMaskForEvents,
                        mockAllTtids,
                        mockRawTracesByTtid,
                        mockSignalMaskAtWindowStart);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        RawTrace rawTrace = tu.createRawTrace(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                e2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1));


        SignalMaskForEventsUtils.fillMockSignalMaskForEvents(
                mockSignalMaskForEvents,
                ImmutableMap.of(
                        TTID_1, SignalMask.UNKNOWN_MASK),
                ImmutableMap.of(
                        TTID_1, ImmutableMap.of(
                                TraceUtils.extractSingleEvent(e1).getEventId(), SignalMask.UNKNOWN_MASK,
                                TraceUtils.extractSingleEvent(e2).getEventId(), SignalMask.UNKNOWN_MASK)));
        fillMockTtidSetLeaf(mockAllTtids, TTID_1);

        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockRawTracesByTtid.getRawTrace(TTID_1)).thenReturn(Optional.of(rawTrace));

        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK));

        module.reset();

        Assert.assertThat(
                producer.getComputed().threadsForSignal(SIGNAL_NUMBER_1),
                isEmpty());
    }

    @Test
    public void ttidIncludedWhenSignalEnabledExplicitly() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<ThreadsWhereSignalIsEnabled> producer =
                initProducer(
                        module,
                        mockSignalMaskForEvents,
                        mockAllTtids,
                        mockRawTracesByTtid,
                        mockSignalMaskAtWindowStart);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        RawTrace rawTrace = tu.createRawTrace(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                e2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1));


        SignalMask signal1Enabled = SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_1, EVENT_ID_1);
        SignalMaskForEventsUtils.fillMockSignalMaskForEvents(
                mockSignalMaskForEvents,
                ImmutableMap.of(
                        TTID_1, signal1Enabled),
                ImmutableMap.of(
                        TTID_1, ImmutableMap.of(
                                TraceUtils.extractSingleEvent(e1).getEventId(), SignalMask.UNKNOWN_MASK,
                                TraceUtils.extractSingleEvent(e2).getEventId(), signal1Enabled)));
        fillMockTtidSetLeaf(mockAllTtids, TTID_1);

        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockRawTracesByTtid.getRawTrace(TTID_1)).thenReturn(Optional.of(rawTrace));

        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK));

        module.reset();

        Assert.assertThat(
                producer.getComputed().threadsForSignal(SIGNAL_NUMBER_1),
                containsExactly(TTID_1));
    }

    @Test
    public void ttidIncludedWhenSignalEnabledExplicitlyAtTheEnd() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<ThreadsWhereSignalIsEnabled> producer =
                initProducer(
                        module,
                        mockSignalMaskForEvents,
                        mockAllTtids,
                        mockRawTracesByTtid,
                        mockSignalMaskAtWindowStart);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        RawTrace rawTrace = tu.createRawTrace(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                e2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1));


        SignalMask signal1Enabled = SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_1, EVENT_ID_1);
        SignalMaskForEventsUtils.fillMockSignalMaskForEvents(
                mockSignalMaskForEvents,
                ImmutableMap.of(
                        TTID_1, signal1Enabled),
                ImmutableMap.of(
                        TTID_1, ImmutableMap.of(
                                TraceUtils.extractSingleEvent(e1).getEventId(), SignalMask.UNKNOWN_MASK,
                                TraceUtils.extractSingleEvent(e2).getEventId(), SignalMask.UNKNOWN_MASK)));
        fillMockTtidSetLeaf(mockAllTtids, TTID_1);

        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockRawTracesByTtid.getRawTrace(TTID_1)).thenReturn(Optional.of(rawTrace));

        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK));

        module.reset();

        Assert.assertThat(
                producer.getComputed().threadsForSignal(SIGNAL_NUMBER_1),
                containsExactly(TTID_1));
    }

    @Test
    public void ttidIncludedWhenSignalEnabledThenDisabled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<ThreadsWhereSignalIsEnabled> producer =
                initProducer(
                        module,
                        mockSignalMaskForEvents,
                        mockAllTtids,
                        mockRawTracesByTtid,
                        mockSignalMaskAtWindowStart);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        RawTrace rawTrace = tu.createRawTrace(
                e1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                e2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1));


        SignalMask signal1Enabled = SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_1, EVENT_ID_1);
        SignalMask signal1Disabled = SignalMask.UNKNOWN_MASK.disable(SIGNAL_NUMBER_1, EVENT_ID_1);
        SignalMaskForEventsUtils.fillMockSignalMaskForEvents(
                mockSignalMaskForEvents,
                ImmutableMap.of(
                        TTID_1, signal1Disabled),
                ImmutableMap.of(
                        TTID_1, ImmutableMap.of(
                                TraceUtils.extractSingleEvent(e1).getEventId(), SignalMask.UNKNOWN_MASK,
                                TraceUtils.extractSingleEvent(e2).getEventId(), signal1Enabled)));
        fillMockTtidSetLeaf(mockAllTtids, TTID_1);

        when(mockRawTracesByTtid.getRawTrace(anyInt())).thenReturn(Optional.empty());
        when(mockRawTracesByTtid.getRawTrace(TTID_1)).thenReturn(Optional.of(rawTrace));

        SignalMasksAtWindowStartUtils.fillMockSignalMasksAtWindowStart(
                mockSignalMaskAtWindowStart,
                ImmutableMap.of(TTID_1, SignalMask.UNKNOWN_MASK));

        module.reset();

        Assert.assertThat(
                producer.getComputed().threadsForSignal(SIGNAL_NUMBER_1),
                containsExactly(TTID_1));
    }

    private static ComputingProducerWrapper<ThreadsWhereSignalIsEnabled> initProducer(
            TestProducerModule module,
            SignalMaskForEvents signalMaskForEvents,
            TtidSetLeaf allTtids,
            RawTracesByTtid rawTracesByTtid,
            SignalMaskAtWindowStart<? extends ProducerState> signalMaskAtWindowStart) {
        return new ComputingProducerWrapper<>(
                new ThreadsWhereSignalIsEnabled(
                        new ComputingProducerWrapper<>(signalMaskForEvents, module),
                        new ComputingProducerWrapper<>(allTtids, module),
                        new ComputingProducerWrapper<>(rawTracesByTtid, module),
                        new ComputingProducerWrapper<>(signalMaskAtWindowStart, module)),
                module);
    }
}
