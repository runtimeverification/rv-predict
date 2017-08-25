package com.runtimeverification.rvpredict.signals;

import com.runtimeverification.rvpredict.log.EventType;
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

    public static Optional<SignalMask> changedSignalMaskAfterEvent(
            ReadonlyEventInterface event, SignalMask threadMask) {
        if (event.getType() == EventType.UNBLOCK_SIGNALS) {
            return Optional.of(threadMask.unblock(event.getPartialSignalMask()));
        } else if (event.getType() == EventType.BLOCK_SIGNALS) {
            return Optional.of(threadMask.block(event.getPartialSignalMask()));
        } else if (event.getType() == EventType.WRITE_SIGNAL_MASK) {
            return Optional.of(threadMask.setMask(event.getFullWriteSignalMask()));
        } else if (event.getType() == EventType.READ_SIGNAL_MASK) {
            threadMask.assertSameBits(event.getFullReadSignalMask());
            // Since the signal mask is per-thread and we have read-write consistency
            // for these, it's safe to overwrite the unknown bits in the mask with
            // the read values.
            return Optional.of(threadMask.setMask(event.getFullReadSignalMask()));
        } else if (event.getType() == EventType.READ_WRITE_SIGNAL_MASK) {
            threadMask.assertSameBits(event.getFullReadSignalMask());
            return Optional.of(threadMask.setMask(event.getFullWriteSignalMask()));
        }
        return Optional.empty();
    }

    private static boolean signalInMask(long signalNumber, long mask) {
        return (mask & (1L << signalNumber)) != 0;
    }

}
