package com.runtimeverification.rvpredict.signals;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.util.Constants;

public class SignalMismatchError {
    public static String errorMessage(ReadonlyEventInterface event, SignalMask computedMask, long signalNumber) {
        return errorMessage(event.getOriginalId(),computedMask.getOriginalEventIdForChange(signalNumber), signalNumber);
    }

    private static String eventString(long originalEventId) {
        if (originalEventId == Constants.INTERRUPTION_MARKER_EVENT_ID) {
            return "<some interruption by this signal>";
        }
        return Long.toString(originalEventId);
    }

    public static String errorMessage(long firstOriginalEventId, long secondOriginalEventId, long signalNumber) {
        return "Enabling mismatch for signal #" + signalNumber + " at events "
                + eventString(firstOriginalEventId) + " and "
                + eventString(secondOriginalEventId);
    }
}
