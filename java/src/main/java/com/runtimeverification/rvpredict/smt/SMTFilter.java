package com.runtimeverification.rvpredict.smt;

import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

/**
 * Interface common to all SMT Filters, preparing a formula to be sent to the SMT Solver
 */
public interface SMTFilter {
    public String getSMTQuery(FormulaTerm node) throws Exception;
}
