package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

public class False implements ModelConstraint {
    // TODO(virgil): remove this hack.
    private final ModelConstraint alternateConstraint = new Or();

    @Override
    public FormulaTerm createSmtFormula() {
        return alternateConstraint.createSmtFormula();
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return alternateConstraint.evaluate(variableSource);
    }

    @Override
    public String toString() {
        return "false";
    }
}
