package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.IntConstant;
import com.runtimeverification.rvpredict.smt.formula.InterruptedThreadVariable;

public class SignalInterruptsThread implements ModelRestrict {
    private final InterruptedThreadVariable interruptedThreadVariable;
    private final String interruptedThreadVariableName;
    private final int interruptedTtid;
    public SignalInterruptsThread(int interruptingSignalTtid, int interruptedTtid) {
        this.interruptedTtid = interruptedTtid;
        interruptedThreadVariable = new InterruptedThreadVariable(interruptingSignalTtid);
        interruptedThreadVariableName = interruptedThreadVariable.toString();
    }

    @Override
    public FormulaTerm createSmtFormula() {
        return FormulaTerm.INT_EQUAL(interruptedThreadVariable, new IntConstant(interruptedTtid));
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return variableSource.getValue(interruptedThreadVariableName).asInt() == interruptedTtid;
    }

    @Override
    public String toString() {
        return "(" + interruptedThreadVariableName + " = " + interruptedTtid + ")";
    }
}
