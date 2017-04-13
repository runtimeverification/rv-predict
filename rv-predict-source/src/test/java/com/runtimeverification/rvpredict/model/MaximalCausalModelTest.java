package com.runtimeverification.rvpredict.model;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.violation.Race;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MaximalCausalModelTest {
    private static final long BASE_GID = 1000;
    private static final int BASE_ID = 2000;
    private static final long BASE_TID = 3000;
    private static final long BASE_ADDR = 4000;
    private static final long BASE_VALUE = 5000;

    @Mock private Configuration mockConfiguration;
    @Mock private Trace mockTrace;
    @Mock private EventStepper mockEventStepper;
    @Mock private ModelTrace mockModelTrace;

    private final Set<Integer> allEventIdDeltas = new HashSet<>();

    @Test
    public void racesBetweenUndefinedReadsAndDefinedWrites() {
        Map<Long, List<Event>> events = new HashMap<>();

        write(events, 1, 1, 1, 1);
        start(events, 1, 2, 2);
        start(events, 1, 3, 3);
        join(events, 1, 4, 2);
        join(events, 1, 5, 3);

        read(events, 2, 6, 1, 3);

        write(events, 3, 7, 1, 2);
        write(events, 3, 8, 1, 3);

        when(mockTrace.eventsByThreadID()).thenReturn(events);
        when(mockTrace.getEvents(BASE_TID + 1)).thenReturn(events.get(BASE_TID + 1));
        when(mockTrace.getEvents(BASE_TID + 2)).thenReturn(events.get(BASE_TID + 2));
        when(mockTrace.getEvents(BASE_TID + 3)).thenReturn(events.get(BASE_TID + 3));
        when(mockTrace.getHeldLocksAt(any(Event.class))).thenReturn(Collections.emptyList());
        MaximalCausalModel maximalCausalModel =
                MaximalCausalModel.create(mockTrace, mockConfiguration, mockEventStepper, mockModelTrace);
        /*
        Map<String, Race> races = maximalCausalModel.findRaces();
        Assert.assertEquals(2, races.size());
        assertContains(races, "Race(0," + (BASE_ID + 6) + "," + (BASE_ID + 7) + ")");
        assertContains(races, "Race(0," + (BASE_ID + 6) + "," + (BASE_ID + 8) + ")");
        */
    }

    private <T> void assertContains(Set<T> set, T object) {
        Assert.assertTrue(
                "Expected that the set " + set + " contains '" + object + "' but it doesn't.",
                set.contains(object));
    }
    private <T, S> void assertContains(Map<T, S> map, T object) {
        Assert.assertTrue(
                "Expected that the map " + map + " contains '" + object + "' but it doesn't.",
                map.containsKey(object));
    }
    private <T> void assertNotContains(Set<T> set, T object) {
        Assert.assertFalse(
                "Expected that the set " + set + " does not contain '" + object + "' but it does.",
                set.contains(object));
    }
    private <T, S> void assertNotContains(Map<T, S> map, T object) {
        Assert.assertFalse(
                "Expected that the map " + map + " does not contain '" + object + "' but it does.",
                map.containsKey(object));
    }

    private void start(Map<Long, List<Event>> events, long tidDelta, int idDelta, long otherThreadIdDelta) {
        addEvent(events, tidDelta, idDelta, BASE_TID + otherThreadIdDelta, 0, EventType.START);
    }
    private void join(Map<Long, List<Event>> events, long tidDelta, int idDelta, long otherThreadIdDelta) {
        addEvent(events, tidDelta, idDelta, BASE_TID + otherThreadIdDelta, 0, EventType.JOIN);
    }

    private void write(Map<Long, List<Event>> events,
            long tidDelta, int idDelta, long addressDelta, long valueDelta) {
        addEvent(events, tidDelta, idDelta, BASE_ADDR + addressDelta, valueDelta, EventType.WRITE);
    }
    private void read(Map<Long, List<Event>> events,
            long tidDelta, int idDelta, long addressDelta, long valueDelta) {
        addEvent(events, tidDelta, idDelta, BASE_ADDR + addressDelta, valueDelta, EventType.READ);
    }

    private void addEvent(Map<Long, List<Event>> events,
            long tidDelta, int idDelta, long address, long valueDelta, EventType type) {
        Assert.assertFalse("Reused event ID delta: " + idDelta + ".", allEventIdDeltas.contains(idDelta));
        allEventIdDeltas.add(idDelta);
        events.computeIfAbsent(BASE_TID + tidDelta, tid -> new ArrayList<>())
                .add(new Event(BASE_GID + idDelta, BASE_TID + tidDelta, BASE_ID + idDelta,
                address, BASE_VALUE + valueDelta, type));
    }
}
