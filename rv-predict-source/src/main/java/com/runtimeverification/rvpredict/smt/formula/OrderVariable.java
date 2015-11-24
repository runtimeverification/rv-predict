package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.smt.visitors.Visitor;

public class OrderVariable extends SMTVariable implements IntFormula {
    /**
     * Prefix for naming variables belonging to this class.
     */
    private static final String O = "o";

    public static OrderVariable get(Event event) {
        return new OrderVariable(event);
    }

    private OrderVariable(Event event) {
        super(event.getGID());
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getNamePrefix() {
        return O;
    }

}
