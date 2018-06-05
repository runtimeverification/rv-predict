package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

public class InterruptionDepthVariable extends SMTVariable implements IntFormula {
    /**
     * Prefix for naming variables belonging to this class.
     */
    private static final String CIDV = "cidv";

    public InterruptionDepthVariable(int signalTid) {
        super(signalTid);
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getNamePrefix() {
        return CIDV;
    }
}
