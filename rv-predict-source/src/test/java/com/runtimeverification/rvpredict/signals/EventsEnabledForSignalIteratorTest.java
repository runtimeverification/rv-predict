package com.runtimeverification.rvpredict.signals;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static com.runtimeverification.rvpredict.testutils.TestUtils.fromOptional;
import static org.mockito.Mockito.when;

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
    private static final long VALUE_5 = 404L;
    private static final long CANONICAL_FRAME_ADDRESS = 501;
    private static final long BASE_ID = 600;

    @Mock private Context mockContext;

    private long nextIdDelta;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
    }

    /*
     * Tests for the case when we can't race with the main thread, both in strict and fast detection mode.
     */

    @Test
    public void iteratesThroughEventsWhenNotDisabled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> events = tu.flatten(
                e1 = tu.nonAtomicStore(ADDRESS, VALUE_1),
                e2 = tu.nonAtomicStore(ADDRESS, VALUE_2),
                e3 = tu.nonAtomicStore(ADDRESS, VALUE_3)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e1)),
                stop(first(e1), first(e2)),
                stop(first(e2), first(e3)),
                stop(first(e3), Optional.empty()));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e1)),
                stop(first(e1), first(e2)),
                stop(first(e2), first(e3)),
                stop(first(e3), Optional.empty()));
    }

    @Test
    public void skipsAllEventsWhenDisabledAtStartAndNotEnabled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ));
    }

    @Test
    public void iteratesThroughEventsAfterEnabling() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                e1 = tu.enableSignal(SIGNAL_NUMBER),
                e2 = tu.nonAtomicStore(ADDRESS, VALUE_2),
                e3 = tu.nonAtomicStore(ADDRESS, VALUE_3)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e2)),
                stop(first(e2), first(e3)),
                stop(first(e3), Optional.empty()));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e2)),
                stop(first(e2), first(e3)),
                stop(first(e3), Optional.empty()));
    }

    @Test
    public void doesNotIterateThroughEventsAfterDisabling() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                e1 = tu.enableSignal(SIGNAL_NUMBER),
                e2 = tu.nonAtomicStore(ADDRESS, VALUE_2),
                e3 = tu.disableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e2)),
                stop(first(e2), first(e3)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e2)),
                stop(first(e2), first(e3)));
    }

    @Test
    public void doesNotCareAboutDifferentSignalsBeingEnabledOrDisabled() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> e4;
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.enableSignal(SIGNAL_NUMBER + 1),
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                e1 = tu.enableSignal(SIGNAL_NUMBER),
                e2 = tu.disableSignal(SIGNAL_NUMBER + 1),
                e3 = tu.nonAtomicStore(ADDRESS, VALUE_2),
                e4 = tu.disableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e2)),
                stop(first(e2), first(e3)),
                stop(first(e3), first(e4)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e2)),
                stop(first(e2), first(e3)),
                stop(first(e3), first(e4)));
    }

    @Test
    public void iteratesThroughEventsEvenIfNoOtherEventBetweenEnablingAndDisabling() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                e1 = tu.enableSignal(SIGNAL_NUMBER),
                e2 = tu.disableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_2),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e2)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e2)));
    }

    /*
     * Tests for the case when we can race with the main thread, both in fast and strict detection mode.
     */

    @Test
    public void iterationWhenDetectingRacesWithSameThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> events = tu.flatten(
                e1 = tu.nonAtomicStore(ADDRESS, VALUE_1),
                e2 = tu.nonAtomicStore(ADDRESS, VALUE_2),
                e3 = tu.nonAtomicStore(ADDRESS, VALUE_3)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastUnsoundMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), Optional.empty()));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e2)),
                stop(first(e1), first(e3)),
                stop(first(e2), Optional.empty()));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e2)),
                stop(first(e1), first(e3)),
                stop(first(e2), Optional.empty()));
    }

    @Test
    public void iterationUntilDisablingWhenDetectingRacesWithSameThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> events = tu.flatten(
                e1 = tu.nonAtomicStore(ADDRESS, VALUE_1),
                e2 = tu.nonAtomicStore(ADDRESS, VALUE_2),
                e3 = tu.disableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_3)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastUnsoundMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e3)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e2)),
                stop(first(e1), first(e3)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e2)),
                stop(first(e1), first(e3)));
    }

    @Test
    public void iterationAfterEnablingWhenDetectingRacesWithSameThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> events = tu.flatten(
                tu.nonAtomicStore(ADDRESS, VALUE_1),
                e1 = tu.enableSignal(SIGNAL_NUMBER),
                e2 = tu.nonAtomicStore(ADDRESS, VALUE_2),
                e3 = tu.nonAtomicStore(ADDRESS, VALUE_3)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastUnsoundMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), Optional.empty()));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e3)),
                stop(first(e2), Optional.empty()));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e3)),
                stop(first(e2), Optional.empty()));
    }

    @Test
    public void iterationBetweenEnablingAndDisablingWhenDetectingRacesWithSameThread()
            throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> e4;
        List<ReadonlyEventInterface> e5;
        List<ReadonlyEventInterface> e6;
        List<ReadonlyEventInterface> e7;
        List<ReadonlyEventInterface> e8;
        List<ReadonlyEventInterface> events = tu.flatten(
                e1 = tu.enableSignal(SIGNAL_NUMBER),
                e2 = tu.nonAtomicStore(ADDRESS, VALUE_1),
                e3 = tu.nonAtomicStore(ADDRESS, VALUE_2),
                e4 = tu.disableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_3),
                e5 = tu.enableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_4),
                e6 = tu.disableSignal(SIGNAL_NUMBER),
                e7 = tu.enableSignal(SIGNAL_NUMBER),
                e8 = tu.disableSignal(SIGNAL_NUMBER)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastUnsoundMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e4)),
                stop(first(e5), first(e6)),
                stop(last(e7), first(e8)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e3)),
                stop(first(e2), first(e4)),
                stop(first(e5), first(e6)),
                stop(last(e7), first(e8)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e3)),
                stop(first(e2), first(e4)),
                stop(first(e5), first(e6)),
                stop(last(e7), first(e8)));
    }

    @Test
    public void fastIterationStopsAtLockBorders()
            throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> e4;
        List<ReadonlyEventInterface> e5;
        List<ReadonlyEventInterface> e6;
        List<ReadonlyEventInterface> events = tu.flatten(
                e1 = tu.enableSignal(SIGNAL_NUMBER),
                e2 = tu.nonAtomicStore(ADDRESS, VALUE_1),
                e3 = tu.nonAtomicStore(ADDRESS, VALUE_2),
                e4 = tu.atomicStore(ADDRESS, VALUE_3),
                e5 = tu.nonAtomicStore(ADDRESS, VALUE_4),
                e6 = tu.disableSignal(SIGNAL_NUMBER)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastUnsoundMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e4)),
                stop(first(e4), first(e6)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e3)),
                stop(first(e2), first(e4)),
                stop(first(e4), last(e4)),
                stop(last(e4), first(e6)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        false,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(first(e1), first(e3)),
                stop(first(e2), first(e4)),
                stop(first(e4), first(e5)),
                stop(last(e4), first(e6)));
    }

    @Test
    public void fastIterationStopsAtFirstEnableEventWhenRequested()
            throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> e4;
        List<ReadonlyEventInterface> e5;
        List<ReadonlyEventInterface> events = tu.flatten(
                e1 = tu.nonAtomicStore(ADDRESS, VALUE_1),
                e2 = tu.nonAtomicStore(ADDRESS, VALUE_2),
                e3 = tu.atomicStore(ADDRESS, VALUE_3),
                e4 = tu.nonAtomicStore(ADDRESS, VALUE_4),
                e5 = tu.enableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_5)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastUnsoundMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        true  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e3)),
                stop(first(e3), first(e5)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        true  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e2)),
                stop(first(e1), first(e3)),
                stop(first(e3), last(e3)),
                stop(last(e3), first(e5)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        true  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e2)),
                stop(first(e1), first(e3)),
                stop(first(e3), first(e4)),
                stop(last(e3), first(e5)));
    }

    @Test
    public void fastIterationStopsAtFirstDisableEventWhenRequested()
            throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> e4;
        List<ReadonlyEventInterface> e5;
        List<ReadonlyEventInterface> events = tu.flatten(
                e1 = tu.nonAtomicStore(ADDRESS, VALUE_1),
                e2 = tu.nonAtomicStore(ADDRESS, VALUE_2),
                e3 = tu.atomicStore(ADDRESS, VALUE_3),
                e4 = tu.nonAtomicStore(ADDRESS, VALUE_4),
                e5 = tu.disableSignal(SIGNAL_NUMBER),
                tu.nonAtomicStore(ADDRESS, VALUE_5)
        );

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastUnsoundMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        true  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e3)),
                stop(first(e3), first(e5)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        true  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e2)),
                stop(first(e1), first(e3)),
                stop(first(e3), last(e3)),
                stop(last(e3), first(e5)));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        true  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e2)),
                stop(first(e1), first(e3)),
                stop(first(e3), first(e4)),
                stop(last(e3), first(e5)));
    }

    @Test
    public void fastModeSkipsOverUnimportantEvents() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_ID, NO_SIGNAL, PC_BASE);
        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;
        List<ReadonlyEventInterface> e3;
        List<ReadonlyEventInterface> e4;
        List<ReadonlyEventInterface> events = tu.flatten(
                e1 = tu.nonAtomicStore(ADDRESS, VALUE_1),
                e2 = tu.enterFunction(CANONICAL_FRAME_ADDRESS, OptionalLong.empty()),
                e3 = tu.nonAtomicStore(ADDRESS, VALUE_2),
                e4 = tu.nonAtomicStore(ADDRESS, VALUE_3));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e1)),
                stop(first(e1), first(e2)),
                stop(first(e2), first(e3)),
                stop(first(e3), first(e4)),
                stop(first(e4), Optional.empty()));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithNoInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e1)),
                stop(first(e1), first(e3)),
                stop(first(e3), first(e4)),
                stop(first(e4), Optional.empty()));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastUnsoundMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), Optional.empty()));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionStrictMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e2)),
                stop(first(e2), first(e4)),
                stop(first(e3), Optional.empty()));

        assertIteratorStops(
                EventsEnabledForSignalIterator.createWithInterruptedThreadRaceDetectionFastMode(
                        events, SIGNAL_NUMBER,
                        true,  // enabledAtStart
                        false  // stopAtFirstMaskChangeEvent
                ),
                stop(Optional.empty(), first(e3)),
                stop(first(e2), first(e4)),
                stop(first(e3), Optional.empty()));
    }

    private void assertIteratorStops(EventsEnabledForSignalIterator iterator, Stop ... stops) {
        for (Stop stop : stops) {
            Assert.assertTrue(iterator.advance());
            Optional<ReadonlyEventInterface> previous =
                    iterator.getPreviousEventWithDefault(Optional.empty());
            if (stop.first.isPresent()) {
                Assert.assertEquals("First stop event: ", stop.first.get(), fromOptional(previous));
            } else {
                Assert.assertFalse(previous.isPresent());
            }
            Optional<ReadonlyEventInterface> current =
                    iterator.getCurrentEventWithDefault(Optional.empty());
            if (stop.second.isPresent()) {
                Assert.assertTrue("Expected: " + stop.second.get() + " but got no event.", current.isPresent());
                Assert.assertEquals("Second stop event: ", stop.second.get(), current.get());
            } else {
                Assert.assertFalse(current.isPresent());
            }
        }
        Assert.assertFalse(iterator.advance());
    }

    class Stop {
        private final Optional<ReadonlyEventInterface> first;
        private final Optional<ReadonlyEventInterface> second;

        Stop(Optional<ReadonlyEventInterface> first, Optional<ReadonlyEventInterface> second) {
            this.first = first;
            this.second = second;
        }
    }

    private Stop stop(Optional<ReadonlyEventInterface> first, Optional<ReadonlyEventInterface> second) {
        return new Stop(first, second);
    }

    private Optional<ReadonlyEventInterface> first(List<ReadonlyEventInterface> events) {
        assert !events.isEmpty();
        return Optional.of(events.get(0));
    }

    private Optional<ReadonlyEventInterface> last(List<ReadonlyEventInterface> events) {
        assert !events.isEmpty();
        return Optional.of(events.get(events.size() - 1));
    }
}