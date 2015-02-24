package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;
import com.runtimeverification.rvpredict.trace.Event;

/**
 * Created by Traian on 23.02.2015.
 */
public abstract class BooleanVariable extends Variable implements Formula {
    public BooleanVariable(Event event) {
        super(Sort.Bool, event);
    }
}
