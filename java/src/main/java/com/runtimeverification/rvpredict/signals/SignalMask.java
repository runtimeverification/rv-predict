package com.runtimeverification.rvpredict.signals;

import com.runtimeverification.rvpredict.util.Constants;

import java.util.Arrays;

public class SignalMask {
    public enum SignalMaskBit {
        ENABLED,
        DISABLED,
        UNKNOWN
    }
    public static final SignalMask UNKNOWN_MASK = new SignalMask();

    private final SignalMaskBit[] maskBits;
    private final long[] eventForSignal;

    private SignalMask(SignalMask signalMask) {
        maskBits = Arrays.copyOf(signalMask.maskBits, signalMask.maskBits.length);
        eventForSignal = Arrays.copyOf(signalMask.eventForSignal, signalMask.eventForSignal.length);
    }

    private SignalMask() {
        maskBits = new SignalMaskBit[Math.toIntExact(Constants.SIGNAL_NUMBER_COUNT)];
        Arrays.fill(maskBits, SignalMaskBit.UNKNOWN);
        eventForSignal = new long[maskBits.length];
        Arrays.fill(eventForSignal, Constants.INVALID_EVENT_ID);
    }

    SignalMask unblock(long signalMask, long eventId) {
        SignalMask unblocked = new SignalMask(this);
        for (int i = 0; i < Constants.SIGNAL_NUMBER_COUNT; i++) {
            if (Signals.signalInMask(i, signalMask)) {
                unblocked.maskBits[i] = SignalMaskBit.ENABLED;
                unblocked.eventForSignal[i] = eventId;
            }
        }
        return unblocked;
    }

    SignalMask block(long signalMask, long eventId) {
        SignalMask blocked = new SignalMask(this);
        for (int i = 0; i < Constants.SIGNAL_NUMBER_COUNT; i++) {
            if (Signals.signalInMask(i, signalMask)) {
                blocked.maskBits[i] = SignalMaskBit.DISABLED;
                blocked.eventForSignal[i] = eventId;
            }
        }
        return blocked;
    }

    public SignalMask enable(long signalNumber, long eventId) {
        return setSignalMaskBit(Math.toIntExact(signalNumber), SignalMaskBit.ENABLED, eventId);
    }

    public SignalMask disable(long signalNumber, long eventId) {
        return setSignalMaskBit(Math.toIntExact(signalNumber), SignalMaskBit.DISABLED, eventId);
    }

    public SignalMask setMask(long mask, long eventId) {
        SignalMask retv = new SignalMask(this);
        for (int i = 0; i < Constants.SIGNAL_NUMBER_COUNT; i++) {
            retv.maskBits[i] = Signals.signalIsEnabled(i, mask) ? SignalMaskBit.ENABLED : SignalMaskBit.DISABLED;
            retv.eventForSignal[i] = eventId;
        }
        return retv;
    }

    public SignalMask enabledToUnknown() {
        SignalMask retv = new SignalMask(this);
        for (int i = 0; i < retv.maskBits.length; i++) {
            if (retv.maskBits[i] == SignalMaskBit.ENABLED) {
                retv.maskBits[i] = SignalMaskBit.UNKNOWN;
                retv.eventForSignal[i] = Constants.INVALID_EVENT_ID;
            }
        }
        return retv;
    }

    public SignalMaskBit getMaskBit(long signalNumber) {
        return maskBits[Math.toIntExact(signalNumber)];
    }

    public long getOriginalEventIdForChange(long signalNumber) {
        return eventForSignal[Math.toIntExact(signalNumber)];
    }

    void assertSameBits(long mask, long originalEventId) {
        for (int i = 0; i < Constants.SIGNAL_NUMBER_COUNT; i++) {
            switch (maskBits[i]) {
                case ENABLED:
                    assert Signals.signalIsEnabled(i, mask)
                            : SignalMismatchError.errorMessage(getOriginalEventIdForChange(i), originalEventId, i);
                    break;
                case DISABLED:
                    assert !Signals.signalIsEnabled(i, mask)
                            : SignalMismatchError.errorMessage(getOriginalEventIdForChange(i), originalEventId, i);
                    break;
                case UNKNOWN:
                    break;
                default:
                    throw new IllegalStateException("Unexpected signal enable bit type: " + maskBits[i]);
            }
        }
    }

    private SignalMask setSignalMaskBit(int signalNumber, SignalMaskBit value, long eventId) {
        if (maskBits[signalNumber] == value) {
            return this;
        }
        SignalMask enabled = new SignalMask(this);
        enabled.maskBits[signalNumber] = value;
        enabled.eventForSignal[signalNumber] = eventId;
        return enabled;
    }

    private String signalMaskBitToShortString(SignalMaskBit bit) {
        switch (bit) {
            case UNKNOWN:
                return "u";
            case ENABLED:
                return "e";
            case DISABLED:
                return "d";
            default:
                throw new IllegalArgumentException("Unknown bit: " + bit);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SignalMask)) {
            return false;
        }
        SignalMask other = (SignalMask) obj;
        return Arrays.equals(maskBits, other.maskBits);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        SignalMaskBit previousBit = maskBits[0];
        int previousBitCount = 0;
        boolean hadOneOutput = false;
        for (SignalMaskBit bit : maskBits) {
            if (bit == previousBit) {
                previousBitCount++;
                continue;
            }
            if (hadOneOutput) {
                sb.append(",");
            }
            if (previousBitCount > 1) {
                sb.append(previousBitCount);
                sb.append("*");
            }
            sb.append(signalMaskBitToShortString(previousBit));
            hadOneOutput = true;
            previousBit = bit;
            previousBitCount = 1;
        }
        if (previousBitCount > 0) {
            if (hadOneOutput) {
                sb.append(",");
            }
            sb.append(previousBitCount);
            sb.append("*");
            sb.append(signalMaskBitToShortString(previousBit));
        }
        sb.append("]");
        return sb.toString();
    }
}
