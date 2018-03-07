package com.runtimeverification.rvpredict.signals;

import com.runtimeverification.rvpredict.util.Constants;
import org.junit.Assert;
import org.junit.Test;

public class SignalMaskTest {
    private final long ANY_SIGNAL = -1L;
    private final long SIGNAL_NUMBER_1 = 2L;
    private final long SIGNAL_NUMBER_2 = 4L;
    private final long EVENT_ID_1 = 201L;
    private final long EVENT_ID_2 = 202L;

    private class State {
        private final long signalNumber;
        private final SignalMask.SignalMaskBit maskBit;
        private final long eventId;

        private State(long signalNumber, SignalMask.SignalMaskBit maskBit, long eventId) {
            this.signalNumber = signalNumber;
            this.maskBit = maskBit;
            this.eventId = eventId;
        }

        @Override
        public String toString() {
            return "State(signal=" + signalNumber + ",bit=" + maskBit + ",event=" + eventId + ")";
        }
    }

    @Test
    public void emptyMask() {
        assertDefaultStateWithExceptions(
                SignalMask.UNKNOWN_MASK,
                defaultState(ANY_SIGNAL, SignalMask.SignalMaskBit.UNKNOWN, Constants.INVALID_EVENT_ID));
    }

    @Test
    public void enable() {
        assertDefaultStateWithExceptions(
                SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_1, EVENT_ID_1),
                defaultState(ANY_SIGNAL, SignalMask.SignalMaskBit.UNKNOWN, Constants.INVALID_EVENT_ID),
                exception(SIGNAL_NUMBER_1, SignalMask.SignalMaskBit.ENABLED, EVENT_ID_1));
        assertDefaultStateWithExceptions(
                SignalMask.UNKNOWN_MASK.enable(SIGNAL_NUMBER_1, EVENT_ID_1).enable(SIGNAL_NUMBER_2, EVENT_ID_2),
                defaultState(ANY_SIGNAL, SignalMask.SignalMaskBit.UNKNOWN, Constants.INVALID_EVENT_ID),
                exception(SIGNAL_NUMBER_1, SignalMask.SignalMaskBit.ENABLED, EVENT_ID_1),
                exception(SIGNAL_NUMBER_2, SignalMask.SignalMaskBit.ENABLED, EVENT_ID_2));
    }

    @Test
    public void disable() {
        assertDefaultStateWithExceptions(
                SignalMask.UNKNOWN_MASK.disable(SIGNAL_NUMBER_1, EVENT_ID_1),
                defaultState(ANY_SIGNAL, SignalMask.SignalMaskBit.UNKNOWN, Constants.INVALID_EVENT_ID),
                exception(SIGNAL_NUMBER_1, SignalMask.SignalMaskBit.DISABLED, EVENT_ID_1));
        assertDefaultStateWithExceptions(
                SignalMask.UNKNOWN_MASK.disable(SIGNAL_NUMBER_1, EVENT_ID_1).disable(SIGNAL_NUMBER_2, EVENT_ID_2),
                defaultState(ANY_SIGNAL, SignalMask.SignalMaskBit.UNKNOWN, Constants.INVALID_EVENT_ID),
                exception(SIGNAL_NUMBER_1, SignalMask.SignalMaskBit.DISABLED, EVENT_ID_1),
                exception(SIGNAL_NUMBER_2, SignalMask.SignalMaskBit.DISABLED, EVENT_ID_2));
    }

    @Test
    public void setMask() {
        assertDefaultStateWithExceptions(
                SignalMask.UNKNOWN_MASK.setMask(1L << SIGNAL_NUMBER_1, EVENT_ID_1),
                defaultState(ANY_SIGNAL, SignalMask.SignalMaskBit.ENABLED, EVENT_ID_1),
                exception(SIGNAL_NUMBER_1, SignalMask.SignalMaskBit.DISABLED, EVENT_ID_1));
    }

    @Test
    public void block() {
        assertDefaultStateWithExceptions(
                SignalMask.UNKNOWN_MASK.block((1L << SIGNAL_NUMBER_1) | (1L << SIGNAL_NUMBER_2), EVENT_ID_1),
                defaultState(ANY_SIGNAL, SignalMask.SignalMaskBit.UNKNOWN, Constants.INVALID_EVENT_ID),
                exception(SIGNAL_NUMBER_1, SignalMask.SignalMaskBit.DISABLED, EVENT_ID_1),
                exception(SIGNAL_NUMBER_2, SignalMask.SignalMaskBit.DISABLED, EVENT_ID_1));
    }

    @Test
    public void unblock() {
        assertDefaultStateWithExceptions(
                SignalMask.UNKNOWN_MASK.unblock((1L << SIGNAL_NUMBER_1) | (1L << SIGNAL_NUMBER_2), EVENT_ID_1),
                defaultState(ANY_SIGNAL, SignalMask.SignalMaskBit.UNKNOWN, Constants.INVALID_EVENT_ID),
                exception(SIGNAL_NUMBER_1, SignalMask.SignalMaskBit.ENABLED, EVENT_ID_1),
                exception(SIGNAL_NUMBER_2, SignalMask.SignalMaskBit.ENABLED, EVENT_ID_1));
    }

    @Test
    public void enabledToUnknown() {
        assertDefaultStateWithExceptions(
                SignalMask.UNKNOWN_MASK.setMask(~(1L << SIGNAL_NUMBER_1), EVENT_ID_1).enabledToUnknown(),
                defaultState(ANY_SIGNAL, SignalMask.SignalMaskBit.DISABLED, EVENT_ID_1),
                exception(SIGNAL_NUMBER_1, SignalMask.SignalMaskBit.UNKNOWN, Constants.INVALID_EVENT_ID));
    }

    private void assertDefaultStateWithExceptions(SignalMask mask, State defaultState, State... exceptions) {
        for (long i = 0; i < Constants.SIGNAL_NUMBER_COUNT; i++) {
            boolean isException = false;
            for (State exception : exceptions) {
                if (i == exception.signalNumber) {
                    assertState(mask, i, exception);
                    isException = true;
                    break;
                }
            }
            if (isException) {
                continue;
            }
            assertState(mask, i, defaultState);
        }
    }

    private void assertState(SignalMask mask, long signalNumber, State state) {
        Assert.assertEquals(state.toString(), state.maskBit, mask.getMaskBit(signalNumber));
        Assert.assertEquals(state.toString(), state.eventId, mask.getOriginalEventIdForChange(signalNumber));
    }

    private State defaultState(long signalNumber, SignalMask.SignalMaskBit maskBit, long eventId) {
        return new State(signalNumber, maskBit, eventId);
    }

    private State exception(long signalNumber, SignalMask.SignalMaskBit maskBit, long eventId) {
        return new State(signalNumber, maskBit, eventId);
    }
}
