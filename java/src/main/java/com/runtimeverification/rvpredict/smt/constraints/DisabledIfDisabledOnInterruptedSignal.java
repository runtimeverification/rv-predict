package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

public class DisabledIfDisabledOnInterruptedSignal implements ModelConstraint {
    private final ModelConstraint alternateConstraint;

    public DisabledIfDisabledOnInterruptedSignal(int interruptingTtid, int interruptedTtid, long signalNumber) {
        this.alternateConstraint = new Or(
                new SignalEnabledOnThreadValue(interruptingTtid, signalNumber, false),
                new SignalEnabledOnThreadValue(interruptedTtid, signalNumber, true)
        );
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
        return "(disabledifdisabledonparent -> " + alternateConstraint.toString() + ")";
    }
}
