package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.trace.Event;

/**
 * Common functionality for Boolean variables.
 */
public abstract class BooleanVariable extends SMTVariable implements Formula {
    public BooleanVariable(Event event) {
        super(event);
    }

    @Override
    public Sort getSort() {
        return Sort.Bool;
    }
}
