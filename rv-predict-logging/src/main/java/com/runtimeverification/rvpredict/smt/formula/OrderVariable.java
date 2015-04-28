package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.log.EventItem;
import com.runtimeverification.rvpredict.smt.visitors.Visitor;

public class OrderVariable extends SMTVariable implements SMTFormula {
    /**
     * Prefix for naming variables belonging to this class.
     */
    public static final String O = "o";

    public OrderVariable(EventItem event) {
        super(event);
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getNamePrefix() {
        return O;
    }

    @Override
    public Sort getSort() {
        return Sort.Int;
    }
}
