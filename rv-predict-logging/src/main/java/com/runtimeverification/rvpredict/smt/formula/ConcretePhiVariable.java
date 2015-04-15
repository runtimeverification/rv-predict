package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;
import com.runtimeverification.rvpredict.trace.Event;

public class ConcretePhiVariable extends BooleanVariable {
    /**
     * Prefix for naming variables belonging to this class.
     */
    private static final String PHI_C = "phi_c";

    public ConcretePhiVariable(Event event) {
       super(event);
    }
    
    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getNamePrefix() {
        return PHI_C;
    }
}
