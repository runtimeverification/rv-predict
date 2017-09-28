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

    private SignalMask(SignalMask signalMask) {
        maskBits = Arrays.copyOf(signalMask.maskBits, signalMask.maskBits.length);
    }

    private SignalMask() {
        maskBits = new SignalMaskBit[Math.toIntExact(Constants.SIGNAL_NUMBER_COUNT)];
        Arrays.fill(maskBits, SignalMaskBit.UNKNOWN);
    }

    SignalMask unblock(long signalMask) {
        SignalMask unblocked = new SignalMask(this);
        for (int i = 0; i < Constants.SIGNAL_NUMBER_COUNT; i++) {
            if (!Signals.signalIsEnabled(i, signalMask)) {
                unblocked.maskBits[i] = SignalMaskBit.ENABLED;
            }
        }
        return unblocked;
    }

    SignalMask block(long signalMask) {
        SignalMask blocked = new SignalMask(this);
        for (int i = 0; i < Constants.SIGNAL_NUMBER_COUNT; i++) {
            if (!Signals.signalIsEnabled(i, signalMask)) {
                blocked.maskBits[i] = SignalMaskBit.DISABLED;
            }
        }
        return blocked;
    }

    public SignalMask enable(long signalNumber) {
        return setSignalMaskBit(Math.toIntExact(signalNumber), SignalMaskBit.ENABLED);
    }

    public SignalMask disable(long signalNumber) {
        return setSignalMaskBit(Math.toIntExact(signalNumber), SignalMaskBit.DISABLED);
    }

    public SignalMask setMask(long mask) {
        SignalMask retv = new SignalMask(this);
        for (int i = 0; i < Constants.SIGNAL_NUMBER_COUNT; i++) {
            retv.maskBits[i] = Signals.signalIsEnabled(i, mask) ? SignalMaskBit.ENABLED : SignalMaskBit.DISABLED;
        }
        return retv;
    }

    public SignalMask enabledToUnknown() {
        SignalMask retv = new SignalMask(this);
        for (int i = 0; i < retv.maskBits.length; i++) {
            retv.maskBits[i] = retv.maskBits[i] == SignalMaskBit.ENABLED ? SignalMaskBit.UNKNOWN : retv.maskBits[i];
        }
        return retv;
    }

    public SignalMaskBit getMaskBit(long signalNumber) {
        return maskBits[Math.toIntExact(signalNumber)];
    }

    void assertSameBits(long mask) {
        for (int i = 0; i < Constants.SIGNAL_NUMBER_COUNT; i++) {
            switch (maskBits[i]) {
                case ENABLED:
                    assert Signals.signalIsEnabled(i, mask);
                    break;
                case DISABLED:
                    assert !Signals.signalIsEnabled(i, mask);
                    break;
                case UNKNOWN:
                    break;
                default:
                    throw new IllegalStateException("Unexpected signal enable bit type: " + maskBits[i]);
            }
        }
    }

    private SignalMask setSignalMaskBit(int signalNumber, SignalMaskBit value) {
        if (maskBits[signalNumber] == value) {
            return this;
        }
        SignalMask enabled = new SignalMask(this);
        enabled.maskBits[signalNumber] = value;
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
