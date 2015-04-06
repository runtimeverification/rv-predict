package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;
import com.runtimeverification.rvpredict.trace.Event;

public final class AbstractPhiVariable extends BooleanVariable {
    /**
     * Prefix for naming variables belonging to this class.
     */
    private static final String PHI_A = "phi_a";

    public AbstractPhiVariable(Event event) {
        super(event);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String getNamePrefix() {
        return PHI_A;
    }
}
