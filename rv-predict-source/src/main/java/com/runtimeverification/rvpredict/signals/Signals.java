package com.runtimeverification.rvpredict.signals;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.Collection;
import java.util.Iterator;

public class Signals {
    public static Boolean signalEnableChange(ReadonlyEventInterface event, long signalNumber) {
        switch (event.getType()) {
            case WRITE_SIGNAL_MASK:
            case READ_WRITE_SIGNAL_MASK:
                return !signalInMask(signalNumber, event.getFullWriteSignalMask());
            case BLOCK_SIGNALS:
                if (signalInMask(signalNumber, event.getPartialSignalMask())) {
                    return false;
                }
                return null;
            case UNBLOCK_SIGNALS:
                if (signalInMask(signalNumber, event.getPartialSignalMask())) {
                    return true;
                }
                return null;
        }
        return null;
    }

    public static boolean signalIsEnabled(long signalNumber, long signalMask) {
        return !signalInMask(signalNumber, signalMask);
    }

    static boolean updateEnabledWithEvent(boolean eventIsEnabled, long signalNumber, ReadonlyEventInterface event) {
        Boolean change = Signals.signalEnableChange(event, signalNumber);
        if (change == null) {
            return eventIsEnabled;
        }
        return change;
    }

    private static boolean signalInMask(long signalNumber, long mask) {
        return (mask & (1 << signalNumber)) != 0;
    }

}
