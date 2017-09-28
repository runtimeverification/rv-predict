package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.InterruptedThreadVariable;

public class SignalsInterruptDifferentThreads implements ModelConstraint {
    private final ModelConstraint alternateConstraint;

    public SignalsInterruptDifferentThreads(Integer ttid1, Integer ttid2) {
        alternateConstraint =
                new Or(new LessThan(new InterruptedThreadVariable(ttid1), new InterruptedThreadVariable(ttid2)),
                        new LessThan(new InterruptedThreadVariable(ttid2), new InterruptedThreadVariable(ttid1)));
    }

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
        return "(signals-interrupt-different-threads -> " + alternateConstraint.toString() + ")";
    }
}
