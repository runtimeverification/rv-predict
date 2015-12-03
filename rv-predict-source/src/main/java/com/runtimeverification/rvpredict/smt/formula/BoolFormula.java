package com.runtimeverification.rvpredict.smt.formula;

/**
 * Interface implemented by all boolean formulas
 */
public interface BoolFormula extends SMTFormula {

    /**
     * @return {@link Sort#Bool}
     */
    @Override
    default Sort getSort() {
        return Sort.Bool;
    }

}
