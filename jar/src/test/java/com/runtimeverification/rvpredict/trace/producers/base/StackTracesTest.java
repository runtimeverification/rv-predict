package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsInOrder;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmpty;
import static com.runtimeverification.rvpredict.testutils.TraceUtils.extractSingleEvent;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StackTracesTest {
    private static final int NO_SIGNAL = 0;
    private static final int TTID_1 = 101;
    private static final long EVENT_ID_1 = 201L;
    private static final long BASE_ID = 301L;
    private static final long THREAD_ID_1 = 401L;
    private static final long PC_BASE = 501L;
    private static final long CANONICAL_FRAME_ADDRESS_1 = 601L;
    private static final long CANONICAL_FRAME_ADDRESS_2 = 602L;
    private static final long CANONICAL_FRAME_ADDRESS_3 = 603L;
    private static final OptionalLong CALL_SITE_ADDRESS_1 = OptionalLong.of(701L);
    private static final long ADDRESS_1 = 801L;
    private static final long VALUE_1 = 901L;

    @Mock private RawTraces mockRawTraces;

    @Mock private Context mockContext;

    private final TestProducerModule module = new TestProducerModule();

    private long nextIdDelta;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
    }

    @Test
    public void noStacksForEmptyTraces() {
        ComputingProducerWrapper<StackTraces> producer = initProducer(module, mockRawTraces);
        module.reset();

        when(mockRawTraces.getTraces()).thenReturn(Collections.emptyList());

        Assert.assertThat(
                producer.getComputed().getStackTraceAfterEventBuilder(TTID_1, EVENT_ID_1).build(),
                isEmpty());
    }

    @Test
    public void addsStartedFunctionsToStack() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<StackTraces> producer = initProducer(module, mockRawTraces);
        module.reset();

        List<ReadonlyEventInterface> event1List;
        List<ReadonlyEventInterface> event2List;
        List<ReadonlyEventInterface> event3List;
        List<ReadonlyEventInterface> event4List;
        List<ReadonlyEventInterface> stack1List;
        List<ReadonlyEventInterface> stack2List;
        List<ReadonlyEventInterface> stack3List;
        RawTrace trace = tu.createRawTrace(
                tu.setPc(PC_BASE),
                event1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                stack1List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_1, CALL_SITE_ADDRESS_1),
                event2List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                stack2List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_2, CALL_SITE_ADDRESS_1),
                event3List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                stack3List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_3, CALL_SITE_ADDRESS_1),
                event4List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
        );

        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(trace));

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event1List),
                isEmpty());

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event2List),
                containsInOrder(getId(stack1List)));

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event3List),
                containsInOrder(getId(stack1List), getId(stack2List)));

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event4List),
                containsInOrder(getId(stack1List), getId(stack2List), getId(stack3List)));
    }

    @Test
    public void removesExitedFunctionFromStack() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<StackTraces> producer = initProducer(module, mockRawTraces);
        module.reset();

        List<ReadonlyEventInterface> event1List;
        List<ReadonlyEventInterface> event2List;
        List<ReadonlyEventInterface> event3List;
        List<ReadonlyEventInterface> event4List;
        List<ReadonlyEventInterface> event5List;
        List<ReadonlyEventInterface> stack1List;
        List<ReadonlyEventInterface> stack2List;
        List<ReadonlyEventInterface> stack3List;
        RawTrace trace = tu.createRawTrace(
                tu.setPc(PC_BASE),
                event1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                stack1List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_1, CALL_SITE_ADDRESS_1),
                event2List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                stack2List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_2, CALL_SITE_ADDRESS_1),
                event3List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                tu.exitFunction(),
                event4List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                stack3List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_3, CALL_SITE_ADDRESS_1),
                event5List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
        );

        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(trace));

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event1List),
                isEmpty());

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event2List),
                containsInOrder(getId(stack1List)));

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event3List),
                containsInOrder(getId(stack1List), getId(stack2List)));

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event4List),
                containsInOrder(getId(stack1List)));

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event5List),
                containsInOrder(getId(stack1List), getId(stack3List)));
    }

    @Test
    public void doesNotCrashWhenExitingTooManyFunctions() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID_1, NO_SIGNAL, PC_BASE);
        ComputingProducerWrapper<StackTraces> producer = initProducer(module, mockRawTraces);
        module.reset();

        List<ReadonlyEventInterface> event1List;
        List<ReadonlyEventInterface> event2List;
        List<ReadonlyEventInterface> event3List;
        List<ReadonlyEventInterface> event4List;
        List<ReadonlyEventInterface> stack1List;
        RawTrace trace = tu.createRawTrace(
                tu.setPc(PC_BASE),
                event1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                stack1List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_1, CALL_SITE_ADDRESS_1),
                event2List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                tu.exitFunction(),
                event3List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                tu.exitFunction(),
                event4List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
        );

        when(mockRawTraces.getTraces()).thenReturn(Collections.singletonList(trace));

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event1List),
                isEmpty());

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event2List),
                containsInOrder(getId(stack1List)));

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event3List),
                isEmpty());

        Assert.assertThat(
                getStackTraceIDs(producer, trace, event4List),
                isEmpty());
    }

    private static long getId(List<ReadonlyEventInterface> event) {
        return extractSingleEvent(event).getEventId();
    }

    private static Collection<Long> getStackTraceIDs(
            ComputingProducerWrapper<StackTraces> producer,
            RawTrace trace,
            List<ReadonlyEventInterface> event) {
        return producer.getComputed().getStackTraceAfterEventBuilder(
                trace.getThreadInfo().getId(),
                getId(event)
        ).build().stream().map(ReadonlyEventInterface::getEventId).collect(Collectors.toList());

    }

    private static ComputingProducerWrapper<StackTraces> initProducer(
            TestProducerModule module,
            RawTraces rawTraces) {
        return new ComputingProducerWrapper<>(
                new StackTraces(
                        new ComputingProducerWrapper<>(rawTraces, module)),
                module);
    }
}
