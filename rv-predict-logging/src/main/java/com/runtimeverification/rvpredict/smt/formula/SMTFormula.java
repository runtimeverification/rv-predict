package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * Interface implemented by all SMT formulas
 */
public interface SMTFormula {
    void accept(Visitor visitor);

    /**
     * @return the sort of this {@link SMTFormula}.
     */
    Sort getSort();
}
