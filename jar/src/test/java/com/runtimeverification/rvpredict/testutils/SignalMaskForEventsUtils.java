package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.trace.producers.signals.SignalMaskForEvents;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

public class SignalMaskForEventsUtils {
    public static void clearMockSignalMaskForEvents(SignalMaskForEvents mockSignalMaskForEvents) {
        when(mockSignalMaskForEvents.extractTtidToLastEventMap()).thenReturn(Collections.emptyMap());
        when(mockSignalMaskForEvents.getSignalMaskBeforeEvent(anyInt(), anyLong())).thenReturn(SignalMask.UNKNOWN_MASK);
    }

    public static void fillMockSignalMaskForEvents(
            SignalMaskForEvents mockSignalMaskForEvents,
            Map<Integer, SignalMask> ttidToSignalMaskAfterLastEvent,
            Map<Integer, Map<Long, SignalMask>> ttidToEventIdToSignalMaskBefore) {
        when(mockSignalMaskForEvents.extractTtidToLastEventMap()).thenReturn(ttidToSignalMaskAfterLastEvent);
        when(mockSignalMaskForEvents.getSignalMaskBeforeEvent(anyInt(), anyLong())).thenReturn(SignalMask.UNKNOWN_MASK);
        when(mockSignalMaskForEvents.getSignalMaskAfterEvent(anyInt(), anyLong())).thenReturn(SignalMask.UNKNOWN_MASK);
        ttidToEventIdToSignalMaskBefore.forEach((ttid, eventIdToSignalMask) -> {
            Optional<Long> previousEventId = Optional.empty();
            for (Map.Entry<Long, SignalMask> eventIdAndMask : eventIdToSignalMask.entrySet()) {
                when(mockSignalMaskForEvents.getSignalMaskBeforeEvent(ttid, eventIdAndMask.getKey()))
                        .thenReturn(eventIdAndMask.getValue());
                previousEventId.ifPresent(
                        eventId -> when(mockSignalMaskForEvents.getSignalMaskAfterEvent(ttid, eventId))
                                .thenReturn(eventIdAndMask.getValue()));
                previousEventId = Optional.of(eventIdAndMask.getKey());
            }
            previousEventId.ifPresent(
                    eventId -> when(mockSignalMaskForEvents.getSignalMaskAfterEvent(ttid, eventId))
                            .thenReturn(ttidToSignalMaskAfterLastEvent.get(ttid)));
        });
    }
}
