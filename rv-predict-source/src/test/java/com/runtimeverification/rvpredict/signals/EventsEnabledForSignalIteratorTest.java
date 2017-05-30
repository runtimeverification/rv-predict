package com.runtimeverification.rvpredict.signals;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static com.runtimeverification.rvpredict.testutils.TraceUtils.extractSingleEvent;

@RunWith(MockitoJUnitRunner.class)
public class EventsEnabledForSignalIteratorTest {
    private static final long SIGNAL_NUMBER = 3;
    private static final int NO_SIGNAL = 0;
    private static final long PC_BASE = 100L;
    private static final long THREAD_ID = 200L;
    private static final long ADDRESS = 300L;
    private static final long VALUE_1 = 400L;
    private static final long VALUE_2 = 401L;
    private static final long VALUE_3 = 402L;
    private static final long VALUE_4 = 403L;
    private static final long VALUE_5 = 403L;

    @Mock private Context mockContext;

    @Test
    public void iteratesThroughEventsWhenNotDisabled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );
        ReadonlyEventInterface defaultEvent = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_4));
        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        events, false, SIGNAL_NUMBER, true);

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(VALUE_4, iterator.getPreviousEventWithDefault(defaultEvent).getDataValue());
        Assert.assertEquals(VALUE_1, iterator.getCurrentEventWithDefault(defaultEvent).getDataValue());

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(VALUE_1, iterator.getPreviousEventWithDefault(defaultEvent).getDataValue());
        Assert.assertEquals(VALUE_2, iterator.getCurrentEventWithDefault(defaultEvent).getDataValue());

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(VALUE_2, iterator.getPreviousEventWithDefault(defaultEvent).getDataValue());
        Assert.assertEquals(VALUE_3, iterator.getCurrentEventWithDefault(defaultEvent).getDataValue());

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(VALUE_3, iterator.getPreviousEventWithDefault(defaultEvent).getDataValue());
        Assert.assertEquals(VALUE_4, iterator.getCurrentEventWithDefault(defaultEvent).getDataValue());

        Assert.assertFalse(iterator.advance());
    }

    @Test
    public void skipsAllEventsWhenDisabledAtStartAndNotEnabled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );
        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        events, false, SIGNAL_NUMBER, false);

        Assert.assertFalse(iterator.advance());
    }

    @Test
    public void iteratesThroughEventsAfterEnabling() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                tu.enableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );
        ReadonlyEventInterface defaultEvent = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_4));
        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        events, false, SIGNAL_NUMBER, false);

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(EventType.UNBLOCK_SIGNALS, iterator.getPreviousEventWithDefault(defaultEvent).getType());
        Assert.assertEquals(VALUE_2, iterator.getCurrentEventWithDefault(defaultEvent).getDataValue());

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(VALUE_2, iterator.getPreviousEventWithDefault(defaultEvent).getDataValue());
        Assert.assertEquals(VALUE_3, iterator.getCurrentEventWithDefault(defaultEvent).getDataValue());

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(VALUE_3, iterator.getPreviousEventWithDefault(defaultEvent).getDataValue());
        Assert.assertEquals(VALUE_4, iterator.getCurrentEventWithDefault(defaultEvent).getDataValue());

        Assert.assertFalse(iterator.advance());
    }

    @Test
    public void doesNotIteratesThroughEventsAfterDisabling() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                tu.enableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.disableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );
        ReadonlyEventInterface defaultEvent = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_4));
        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        events, false, SIGNAL_NUMBER, false);

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(EventType.UNBLOCK_SIGNALS, iterator.getPreviousEventWithDefault(defaultEvent).getType());
        Assert.assertEquals(VALUE_2, iterator.getCurrentEventWithDefault(defaultEvent).getDataValue());

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(VALUE_2, iterator.getPreviousEventWithDefault(defaultEvent).getDataValue());
        Assert.assertEquals(EventType.BLOCK_SIGNALS, iterator.getCurrentEventWithDefault(defaultEvent).getType());

        Assert.assertFalse(iterator.advance());
    }

    @Test
    public void doesNotCareAboutDifferentSignalsBeingEnabledOrDisabled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.enableSignal(SIGNAL_NUMBER + 1),
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                tu.enableSignal(SIGNAL_NUMBER),
                tu.disableSignal(SIGNAL_NUMBER + 1),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.disableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );
        ReadonlyEventInterface defaultEvent = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_4));
        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        events, false, SIGNAL_NUMBER, false);

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(EventType.UNBLOCK_SIGNALS, iterator.getPreviousEventWithDefault(defaultEvent).getType());
        Assert.assertEquals(EventType.BLOCK_SIGNALS, iterator.getCurrentEventWithDefault(defaultEvent).getType());

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(EventType.BLOCK_SIGNALS, iterator.getPreviousEventWithDefault(defaultEvent).getType());
        Assert.assertEquals(VALUE_2, iterator.getCurrentEventWithDefault(defaultEvent).getDataValue());

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(VALUE_2, iterator.getPreviousEventWithDefault(defaultEvent).getDataValue());
        Assert.assertEquals(EventType.BLOCK_SIGNALS, iterator.getCurrentEventWithDefault(defaultEvent).getType());

        Assert.assertFalse(iterator.advance());
    }

    @Test
    public void iteratesThroughEventsEvenIfNoOtherEventBetweenEnablingAndDisabling() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                tu.enableSignal(SIGNAL_NUMBER),
                tu.disableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );
        ReadonlyEventInterface defaultEvent = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_4));
        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        events, false, SIGNAL_NUMBER, false);

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(EventType.UNBLOCK_SIGNALS, iterator.getPreviousEventWithDefault(defaultEvent).getType());
        Assert.assertEquals(EventType.BLOCK_SIGNALS, iterator.getCurrentEventWithDefault(defaultEvent).getType());

        Assert.assertFalse(iterator.advance());
    }

    @Test
    public void fastIterationWhenDetectingRacesWithSameThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );
        ReadonlyEventInterface defaultEvent1 = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_4));
        ReadonlyEventInterface defaultEvent2 = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_5));
        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        events, true, SIGNAL_NUMBER, true);

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(VALUE_4, iterator.getPreviousEventWithDefault(defaultEvent1).getDataValue());
        Assert.assertEquals(VALUE_5, iterator.getCurrentEventWithDefault(defaultEvent2).getDataValue());

        Assert.assertFalse(iterator.advance());
    }

    @Test
    public void fastIterationUntilDisablingWhenDetectingRacesWithSameThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.disableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );
        ReadonlyEventInterface defaultEvent1 = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_4));
        ReadonlyEventInterface defaultEvent2 = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_5));
        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        events, true, SIGNAL_NUMBER, true);

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(VALUE_4, iterator.getPreviousEventWithDefault(defaultEvent1).getDataValue());
        Assert.assertEquals(EventType.BLOCK_SIGNALS, iterator.getCurrentEventWithDefault(defaultEvent2).getType());

        Assert.assertFalse(iterator.advance());
    }

    @Test
    public void fastIterationAfterEnablingWhenDetectingRacesWithSameThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                tu.enableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );
        ReadonlyEventInterface defaultEvent1 = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_4));
        ReadonlyEventInterface defaultEvent2 = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_5));
        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        events, true, SIGNAL_NUMBER, false);

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(EventType.UNBLOCK_SIGNALS, iterator.getPreviousEventWithDefault(defaultEvent2).getType());
        Assert.assertEquals(VALUE_4, iterator.getCurrentEventWithDefault(defaultEvent1).getDataValue());

        Assert.assertFalse(iterator.advance());
    }

    @Test
    public void fastIterationBetweenEnablingAndDisablingWhenDetectingRacesWithSameThread()
            throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.enableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.disableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_3),
                tu.enableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_4),
                tu.disableSignal(SIGNAL_NUMBER),
                tu.enableSignal(SIGNAL_NUMBER),
                tu.disableSignal(SIGNAL_NUMBER)
        );
        ReadonlyEventInterface defaultEvent = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_4));
        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        events, true, SIGNAL_NUMBER, false);

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(EventType.UNBLOCK_SIGNALS, iterator.getPreviousEventWithDefault(defaultEvent).getType());
        Assert.assertEquals(EventType.BLOCK_SIGNALS, iterator.getCurrentEventWithDefault(defaultEvent).getType());

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(EventType.UNBLOCK_SIGNALS, iterator.getPreviousEventWithDefault(defaultEvent).getType());
        Assert.assertEquals(EventType.BLOCK_SIGNALS, iterator.getCurrentEventWithDefault(defaultEvent).getType());

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(EventType.UNBLOCK_SIGNALS, iterator.getPreviousEventWithDefault(defaultEvent).getType());
        Assert.assertEquals(EventType.BLOCK_SIGNALS, iterator.getCurrentEventWithDefault(defaultEvent).getType());

        Assert.assertFalse(iterator.advance());
    }

    @Test
    public void fastIterationStopsAtLockBorders()
            throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.enableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.atomicStore(ADDRESS, VALUE_3),
                tu.nonAtomicStore(ADDRESS, VALUE_4),
                tu.disableSignal(SIGNAL_NUMBER)
        );
        ReadonlyEventInterface defaultEvent = extractSingleEvent(tu.nonAtomicStore(ADDRESS, VALUE_4));
        EventsEnabledForSignalIterator iterator =
                new EventsEnabledForSignalIterator(
                        events, true, SIGNAL_NUMBER, false);

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(EventType.UNBLOCK_SIGNALS, iterator.getPreviousEventWithDefault(defaultEvent).getType());
        Assert.assertEquals(EventType.WRITE_LOCK, iterator.getCurrentEventWithDefault(defaultEvent).getType());

        Assert.assertTrue(iterator.advance());
        Assert.assertEquals(EventType.WRITE_LOCK, iterator.getPreviousEventWithDefault(defaultEvent).getType());
        Assert.assertEquals(ADDRESS, iterator.getPreviousEventWithDefault(defaultEvent).getLockId());
        Assert.assertEquals(EventType.BLOCK_SIGNALS, iterator.getCurrentEventWithDefault(defaultEvent).getType());

        Assert.assertFalse(iterator.advance());
    }
}