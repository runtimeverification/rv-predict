package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.log.EventItem;

/**
 * Common functionality for Boolean variables.
 */
public abstract class BooleanVariable extends SMTVariable implements Formula {
    public BooleanVariable(EventItem event) {
        super(event);
    }

    @Override
    public Sort getSort() {
        return Sort.Bool;
    }
}
