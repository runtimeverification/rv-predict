package com.runtimeverification.rvpredict.trace.producers.base;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerModule;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.OptionalInt;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.assertException;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isAbsent;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isPresentWithValue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TtidToStartAndJoinEventsForWindowTest {
    private static final int NO_SIGNAL = 0;
    private static final long THREAD_1 = 301L;
    private static final long THREAD_2 = 302L;
    private static final long PC_BASE = 401L;
    private static final long BASE_ID = 501L;
    private static final int TTID_1 = 601;
    private static final int TTID_2 = 602;

    @Mock private InterThreadSyncEvents mockInterThreadSyncEvents;
    @Mock private OtidToMainTtid mockOtidToMainTtid;

    @Mock private Context mockContext;

    private final TestProducerModule module = new TestProducerModule();
    private int nextIdDelta = 0;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
    }

    @Test
    public void noEventsWithEmptyInput() {
        ComputingProducerWrapper<TtidToStartAndJoinEventsForWindow> producer =
                initProducer(module, mockInterThreadSyncEvents, mockOtidToMainTtid);

        when(mockInterThreadSyncEvents.getSyncEvents()).thenReturn(Collections.emptyList());
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        module.reset();

        Assert.assertThat(producer.getComputed().getStartEvent(TTID_1), isAbsent());
        Assert.assertThat(producer.getComputed().getJoinEvent(TTID_1), isAbsent());
        Assert.assertThat(producer.getComputed().getStartEvent(TTID_2), isAbsent());
        Assert.assertThat(producer.getComputed().getJoinEvent(TTID_2), isAbsent());
    }

    @Test
    public void indexesEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        ComputingProducerWrapper<TtidToStartAndJoinEventsForWindow> producer =
                initProducer(module, mockInterThreadSyncEvents, mockOtidToMainTtid);

        ReadonlyEventInterface start = TraceUtils.extractSingleEvent(tu.threadStart(THREAD_2));
        ReadonlyEventInterface join = TraceUtils.extractSingleEvent(tu.threadJoin(THREAD_2));

        when(mockInterThreadSyncEvents.getSyncEvents()).thenReturn(ImmutableList.of(start, join));
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockOtidToMainTtid.getTtid(THREAD_2)).thenReturn(OptionalInt.of(TTID_2));
        module.reset();

        Assert.assertThat(producer.getComputed().getStartEvent(TTID_1), isAbsent());
        Assert.assertThat(producer.getComputed().getJoinEvent(TTID_1), isAbsent());
        Assert.assertThat(producer.getComputed().getStartEvent(TTID_2), isPresentWithValue(start));
        Assert.assertThat(producer.getComputed().getJoinEvent(TTID_2), isPresentWithValue(join));
    }

    @Test
    public void exceptionWhenItCantFindTtid() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        ComputingProducerWrapper<TtidToStartAndJoinEventsForWindow> producer =
                initProducer(module, mockInterThreadSyncEvents, mockOtidToMainTtid);

        ReadonlyEventInterface start = TraceUtils.extractSingleEvent(tu.threadStart(THREAD_2));
        ReadonlyEventInterface join = TraceUtils.extractSingleEvent(tu.threadJoin(THREAD_2));

        when(mockInterThreadSyncEvents.getSyncEvents()).thenReturn(ImmutableList.of(start, join));
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        module.reset();

        assertException(IllegalStateException.class, producer::getComputed);
    }

    @Test
    public void resets() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        ComputingProducerWrapper<TtidToStartAndJoinEventsForWindow> producer =
                initProducer(module, mockInterThreadSyncEvents, mockOtidToMainTtid);

        ReadonlyEventInterface start = TraceUtils.extractSingleEvent(tu.threadStart(THREAD_2));
        ReadonlyEventInterface join = TraceUtils.extractSingleEvent(tu.threadJoin(THREAD_2));

        when(mockInterThreadSyncEvents.getSyncEvents()).thenReturn(ImmutableList.of(start, join));
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        when(mockOtidToMainTtid.getTtid(THREAD_2)).thenReturn(OptionalInt.of(TTID_2));
        module.reset();

        Assert.assertThat(producer.getComputed().getStartEvent(TTID_1), isAbsent());
        Assert.assertThat(producer.getComputed().getJoinEvent(TTID_1), isAbsent());
        Assert.assertThat(producer.getComputed().getStartEvent(TTID_2), isPresentWithValue(start));
        Assert.assertThat(producer.getComputed().getJoinEvent(TTID_2), isPresentWithValue(join));

        when(mockInterThreadSyncEvents.getSyncEvents()).thenReturn(Collections.emptyList());
        when(mockOtidToMainTtid.getTtid(anyLong())).thenReturn(OptionalInt.empty());
        module.reset();

        Assert.assertThat(producer.getComputed().getStartEvent(TTID_1), isAbsent());
        Assert.assertThat(producer.getComputed().getJoinEvent(TTID_1), isAbsent());
        Assert.assertThat(producer.getComputed().getStartEvent(TTID_2), isAbsent());
        Assert.assertThat(producer.getComputed().getJoinEvent(TTID_2), isAbsent());
    }

    private static ComputingProducerWrapper<TtidToStartAndJoinEventsForWindow> initProducer(
            ProducerModule module,
            InterThreadSyncEvents interThreadSyncEvents,
            OtidToMainTtid otidToMainTtid) {
        return new ComputingProducerWrapper<>(
                new TtidToStartAndJoinEventsForWindow(
                        new ComputingProducerWrapper<>(interThreadSyncEvents, module),
                        new ComputingProducerWrapper<>(otidToMainTtid, module)),
                module);
    }
}
