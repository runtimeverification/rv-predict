package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.InterruptedThreadVariable;

public class SignalsInterruptDifferentThreads implements ModelRestrict {
    private final ModelRestrict alternateRestrict;

    public SignalsInterruptDifferentThreads(Integer ttid1, Integer ttid2) {
        alternateRestrict =
                new Or(new LessThan(new InterruptedThreadVariable(ttid1), new InterruptedThreadVariable(ttid2)),
                        new LessThan(new InterruptedThreadVariable(ttid2), new InterruptedThreadVariable(ttid1)));
    }

    @Override
    public FormulaTerm createSmtFormula() {
        return alternateRestrict.createSmtFormula();
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return alternateRestrict.evaluate(variableSource);
    }

    @Override
    public String toString() {
        return "(signals-interrupt-different-threads -> " + alternateRestrict.toString() + ")";
    }
}
