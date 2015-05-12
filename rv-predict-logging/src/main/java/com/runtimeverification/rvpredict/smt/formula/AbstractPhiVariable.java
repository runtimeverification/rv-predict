package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.smt.visitors.Visitor;

public final class AbstractPhiVariable extends BooleanVariable {
    /**
     * Prefix for naming variables belonging to this class.
     */
    private static final String PHI_A = "phi_a";

    public AbstractPhiVariable(Event event) {
        super(event);
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getNamePrefix() {
        return PHI_A;
    }
}
