package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.OrderVariable;

public class Before implements ModelRestrict {
    private final ModelRestrict alternateFormula;

    public Before(ReadonlyEventInterface first, ReadonlyEventInterface second) {
        alternateFormula = new LessThan(OrderVariable.get(first), OrderVariable.get(second));
    }

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
        return "(before -> " + alternateFormula.toString() + ")";
    }
}
