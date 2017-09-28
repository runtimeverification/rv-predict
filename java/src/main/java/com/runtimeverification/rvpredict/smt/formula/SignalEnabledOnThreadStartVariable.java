package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;
import com.runtimeverification.rvpredict.util.Constants;

public class SignalEnabledOnThreadStartVariable extends SMTVariable implements IntFormula {
    /**
     * Prefix for naming variables belonging to this class.
     */
    private static final String SIOTV = "sm";

    private final int ttid;
    private final long signalNumber;

    public SignalEnabledOnThreadStartVariable(int ttid, long signalNumber) {
        super(ttid * Constants.SIGNAL_NUMBER_COUNT + signalNumber);
        this.ttid = ttid;
        this.signalNumber = signalNumber;
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getNamePrefix() {
        return SIOTV;
    }

    @Override
    public String toString() {
        return SIOTV + "_" + ttid + "_" + signalNumber;
    }
}
