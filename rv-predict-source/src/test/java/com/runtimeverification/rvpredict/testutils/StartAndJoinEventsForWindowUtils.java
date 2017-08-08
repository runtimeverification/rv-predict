package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.trace.producers.base.StartAndJoinEventsForWindow;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class StartAndJoinEventsForWindowUtils {
    public static void clearMockStartAndJoinEventsForWindow(StartAndJoinEventsForWindow mockStartAndJoinEventsForWindow) {
        when(mockStartAndJoinEventsForWindow.getStartEvent(anyInt())).thenReturn(Optional.empty());
        when(mockStartAndJoinEventsForWindow.getJoinEvent(anyInt())).thenReturn(Optional.empty());
    }

    public static void fillMockStartAndJoinEventsForWindow(
            StartAndJoinEventsForWindow mockStartAndJoinEventsForWindow,
            Map<Integer, ReadonlyEventInterface> ttidToStartEvent,
            Map<Integer, ReadonlyEventInterface> ttidToJoinEvent) {
        clearMockStartAndJoinEventsForWindow(mockStartAndJoinEventsForWindow);
        ttidToStartEvent.forEach((ttid, event) ->
                when(mockStartAndJoinEventsForWindow.getStartEvent(ttid)).thenReturn(Optional.of(event)));
        ttidToJoinEvent.forEach((ttid, event) ->
                when(mockStartAndJoinEventsForWindow.getJoinEvent(ttid)).thenReturn(Optional.of(event)));
    }
}
