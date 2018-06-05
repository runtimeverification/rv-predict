package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.IntConstant;
import com.runtimeverification.rvpredict.smt.formula.InterruptedThreadVariable;
import com.runtimeverification.rvpredict.smt.formula.InterruptionDepthVariable;

// TODO: Maybe split this into multiple ModelConstraints.
public class SignalInterruptsThread implements ModelConstraint {
    private final InterruptedThreadVariable interruptedThreadVariable;
    private final InterruptionDepthVariable interruptingDepthVariable;
    private final InterruptionDepthVariable interruptedDepthVariable;
    private final String interruptedThreadVariableName;
    private final String interruptingDepthVariableName;
    private final String interruptedDepthVariableName;
    private final int interruptedTtid;

    public SignalInterruptsThread(int interruptingSignalTtid, int interruptedTtid) {
        this.interruptedTtid = interruptedTtid;
        interruptedThreadVariable = new InterruptedThreadVariable(interruptingSignalTtid);
        interruptedThreadVariableName = interruptedThreadVariable.toString();
        interruptingDepthVariable = new InterruptionDepthVariable(interruptingSignalTtid);
        interruptingDepthVariableName = interruptingDepthVariable.toString();
        interruptedDepthVariable = new InterruptionDepthVariable(interruptedTtid);
        interruptedDepthVariableName = interruptedDepthVariable.toString();
    }

    @Override
    public FormulaTerm createSmtFormula() {
        FormulaTerm.Builder builder = FormulaTerm.andBuilder();
        builder.add(
                FormulaTerm.INT_EQUAL(interruptedThreadVariable, new IntConstant(interruptedTtid)),
                FormulaTerm.LESS_THAN(interruptedDepthVariable, interruptingDepthVariable));
        return builder.build();
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return variableSource.getValue(interruptedThreadVariableName).asInt() == interruptedTtid
                && variableSource.getValue(interruptingDepthVariableName).asInt() >
                        variableSource.getValue(interruptedDepthVariableName).asInt();
    }

    @Override
    public String toString() {
        return "(and ("
                + interruptedThreadVariableName + " = " + interruptedTtid + ") ("
                + interruptingDepthVariableName + " > " + interruptedDepthVariableName + "))";
    }
}
