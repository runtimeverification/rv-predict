package com.runtimeverification.rvpredict.signals;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.Optional;

public class Signals {
    public static Optional<Boolean> signalEnableChange(ReadonlyEventInterface event, long signalNumber) {
        switch (event.getType()) {
            case WRITE_SIGNAL_MASK:
            case READ_WRITE_SIGNAL_MASK:
                return Optional.of(!signalInMask(signalNumber, event.getFullWriteSignalMask()));
            case BLOCK_SIGNALS:
                if (signalInMask(signalNumber, event.getPartialSignalMask())) {
                    return Optional.of(Boolean.FALSE);
                }
                return Optional.empty();
            case UNBLOCK_SIGNALS:
                if (signalInMask(signalNumber, event.getPartialSignalMask())) {
                    return Optional.of(Boolean.TRUE);
                }
                return Optional.empty();
        }
        return Optional.empty();
    }

    public static boolean signalIsEnabled(long signalNumber, long signalMask) {
        return !signalInMask(signalNumber, signalMask);
    }

    public static boolean signalIsDisabledInFullMask(long signalNumber, long signalMask) {
        return signalInMask(signalNumber, signalMask);
    }

    static boolean updateEnabledWithEvent(boolean eventIsEnabled, long signalNumber, ReadonlyEventInterface event) {
        return Signals.signalEnableChange(event, signalNumber).orElse(eventIsEnabled);
    }

    private static boolean signalInMask(long signalNumber, long mask) {
        return (mask & (1 << signalNumber)) != 0;
    }

}
