package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.trace.producers.signals.InterruptedEvents;
import com.runtimeverification.rvpredict.util.Constants;

import java.util.Collections;
import java.util.Map;
import java.util.OptionalLong;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class InterruptedEventsUtils {
    public static void clearMockInterruptedEvents(InterruptedEvents mockInterruptedEvents) {
        when(mockInterruptedEvents.getInterruptedEventId(anyInt())).thenReturn(OptionalLong.empty());
        when(mockInterruptedEvents.getSignalNumberToTtidToNextMinInterruptedEventId())
                .thenReturn(Collections.emptyMap());
        when(mockInterruptedEvents.getInterruptedTtid(anyInt())).thenReturn(Constants.INVALID_TTID);
    }

    public static void fillMockInterruptedEvents(
            InterruptedEvents mockInterruptedEvents,
            Map<Integer, Integer> signalTtidToInterruptedTtid,
            Map<Integer, Long> signalTtidToInterruptedEventId,
            Map<Long, Map<Integer, Long>> signalNumberToInterruptedTtidToNextMinEventId) {
        when(mockInterruptedEvents.getSignalNumberToTtidToNextMinInterruptedEventId())
                .thenReturn(signalNumberToInterruptedTtidToNextMinEventId);

        when(mockInterruptedEvents.getInterruptedEventId(anyInt())).thenReturn(OptionalLong.empty());
        signalTtidToInterruptedEventId.forEach((interruptingTtid, interruptedEventId) ->
                when(mockInterruptedEvents.getInterruptedEventId(interruptingTtid))
                        .thenReturn(OptionalLong.of(interruptedEventId)));

        when(mockInterruptedEvents.getInterruptedTtid(anyInt())).thenReturn(Constants.INVALID_TTID);
        signalTtidToInterruptedTtid.forEach((interruptingTtid, interruptedTtid) ->
                when(mockInterruptedEvents.getInterruptedTtid(interruptingTtid)).thenReturn(interruptedTtid));
    }
}
