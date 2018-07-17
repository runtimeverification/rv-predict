package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.IntConstant;
import com.runtimeverification.rvpredict.smt.formula.InterruptionDepthVariable;

public class SingleNonSignalHasDepth0 implements ModelConstraint {
    private final InterruptionDepthVariable depthVariable;
    private final String depthVariableName;

    public SingleNonSignalHasDepth0(int ttid) {
        depthVariable = new InterruptionDepthVariable(ttid);
        depthVariableName = depthVariable.toString();
    }

    @Override
    public FormulaTerm createSmtFormula() {
        return FormulaTerm.INT_EQUAL(depthVariable, new IntConstant(0));
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return variableSource.getValue(depthVariableName).asInt() == 0;
    }

    @Override
    public String toString() {
        return "(" + depthVariableName + " = 0)";
    }
}
