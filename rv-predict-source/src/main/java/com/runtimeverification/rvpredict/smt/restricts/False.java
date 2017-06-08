package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

public class False implements ModelRestrict {
    // TODO(virgil): remove this hack.
    private final ModelRestrict alternateFormula = new Or();

    @Override
    public FormulaTerm createSmtFormula() {
        return alternateFormula.createSmtFormula();
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return alternateFormula.evaluate(variableSource);
    }

    @Override
    public String toString() {
        return "false";
    }
}
