package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

import java.util.OptionalInt;

public class InterruptedThreadVariable extends SMTVariable implements IntFormula {
    /**
     * Prefix for naming variables belonging to this class.
     */
    private static final String CITV = "citv";

    public InterruptedThreadVariable(int signalTid) {
        super(signalTid);
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getNamePrefix() {
        return CITV;
    }

    public static OptionalInt extractSignalTtidIfPossible(String name) {
        if (!name.startsWith(CITV)) {
            return OptionalInt.empty();
        }
        int signalTid = Integer.parseInt(name.substring(CITV.length()));
        return OptionalInt.of(signalTid);
    }
}
