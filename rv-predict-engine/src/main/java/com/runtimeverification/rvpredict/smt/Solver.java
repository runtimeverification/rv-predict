package com.runtimeverification.rvpredict.smt;

import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

public interface Solver {

    boolean isSat(FormulaTerm query);

}
