package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;
import com.runtimeverification.rvpredict.trace.Event;

/**
 * Created by Traian on 23.02.2015.
 */
public class OrderVariable extends Variable implements Term {
    public OrderVariable(Event event) {
        super(Sort.Int, event);
    }
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String getName() {
        return "o" + getId();
    }
}
