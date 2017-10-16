package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.trace.producers.signals.SignalMaskForEvents;

import java.util.Collections;
import java.util.Map;

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
        ttidToEventIdToSignalMaskBefore.forEach((ttid, eventIdToSignalMask) ->
                eventIdToSignalMask.forEach((eventId, signalMask) ->
                        when(mockSignalMaskForEvents.getSignalMaskBeforeEvent(ttid, eventId)).thenReturn(signalMask)));
    }
}
