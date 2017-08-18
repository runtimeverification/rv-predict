package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.trace.producers.base.TtidToStartAndJoinEventsForWindow;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class StartAndJoinEventsForWindowUtils {
    public static void clearMockStartAndJoinEventsForWindow(TtidToStartAndJoinEventsForWindow mockTtidToStartAndJoinEventsForWindow) {
        when(mockTtidToStartAndJoinEventsForWindow.getStartEvent(anyInt())).thenReturn(Optional.empty());
        when(mockTtidToStartAndJoinEventsForWindow.getJoinEvent(anyInt())).thenReturn(Optional.empty());
    }

    public static void fillMockStartAndJoinEventsForWindow(
            TtidToStartAndJoinEventsForWindow mockTtidToStartAndJoinEventsForWindow,
            Map<Integer, ReadonlyEventInterface> ttidToStartEvent,
            Map<Integer, ReadonlyEventInterface> ttidToJoinEvent) {
        clearMockStartAndJoinEventsForWindow(mockTtidToStartAndJoinEventsForWindow);
        ttidToStartEvent.forEach((ttid, event) ->
                when(mockTtidToStartAndJoinEventsForWindow.getStartEvent(ttid)).thenReturn(Optional.of(event)));
        ttidToJoinEvent.forEach((ttid, event) ->
                when(mockTtidToStartAndJoinEventsForWindow.getJoinEvent(ttid)).thenReturn(Optional.of(event)));
    }
}
