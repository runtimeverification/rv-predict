package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.IntConstant;
import com.runtimeverification.rvpredict.smt.formula.SignalEnabledOnThreadStartVariable;

public class SignalEnabledOnThreadValue implements ModelConstraint {
    private final SignalEnabledOnThreadStartVariable signalEnabledOnThreadVariable;
    private final String signalEnabledOnThreadVariableName;
    private final int value;

    public SignalEnabledOnThreadValue(int interruptedSignalTtid, long interruptingSignalNumber, boolean b) {
        signalEnabledOnThreadVariable =
                new SignalEnabledOnThreadStartVariable(interruptedSignalTtid, interruptingSignalNumber);
        signalEnabledOnThreadVariableName = signalEnabledOnThreadVariable.toString();
        value = b ? 1 : 0;
    }

    @Override
    public FormulaTerm createSmtFormula() {
        return FormulaTerm.INT_EQUAL(signalEnabledOnThreadVariable, new IntConstant(value));
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return variableSource.getValue(signalEnabledOnThreadVariableName).asInt() == value;
    }

    @Override
    public String toString() {
        return "(" + signalEnabledOnThreadVariableName + " = " + value + ")";
    }
}
