package com.runtimeverification.rvpredict.smt.formula;

/**
 * Interface implemented by all integer formulas
 */
public interface IntFormula extends SMTFormula {

    /**
     * @return {@link Sort#Int}
     */
    @Override
    default Sort getSort() {
        return Sort.Int;
    }
}
