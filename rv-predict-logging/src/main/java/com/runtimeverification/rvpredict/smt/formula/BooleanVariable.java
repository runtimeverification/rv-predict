package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.log.Event;

/**
 * Common functionality for Boolean variables.
 */
public abstract class BooleanVariable extends SMTVariable implements BoolFormula {
    public BooleanVariable(Event event) {
        super(event);
    }

}
