package com.runtimeverification.rvpredict.smt.formula;

/**
 * Common functionality for Boolean variables.
 */
public abstract class BooleanVariable extends SMTVariable implements BoolFormula {
    public BooleanVariable(long id) {
        super(id);
    }

}
