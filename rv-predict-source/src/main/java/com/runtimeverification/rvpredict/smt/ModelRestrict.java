package com.runtimeverification.rvpredict.smt;

import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

// TODO(virgil): Maybe merge this with the SmtFormula.
public interface ModelRestrict {
    FormulaTerm createSmtFormula();
    boolean evaluate(VariableSource variableSource);
}
