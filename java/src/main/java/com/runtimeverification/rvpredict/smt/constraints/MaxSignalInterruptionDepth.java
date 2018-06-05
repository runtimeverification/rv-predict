package com.runtimeverification.rvpredict.smt.constraints;

import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.IntConstant;
import com.runtimeverification.rvpredict.smt.formula.InterruptionDepthVariable;

public class MaxSignalInterruptionDepth implements ModelConstraint {
    private final InterruptionDepthVariable depthVariable;
    private final String depthVariableName;
    private final int maxDepth;

    public MaxSignalInterruptionDepth (int ttid, int maxDepth) {
        depthVariable = new InterruptionDepthVariable(ttid);
        depthVariableName = depthVariable.toString();
        this.maxDepth = maxDepth;
    }

    @Override
    public FormulaTerm createSmtFormula() {
        return FormulaTerm.LESS_THAN(depthVariable, new IntConstant(maxDepth + 1));
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return variableSource.getValue(depthVariableName).asInt() <= maxDepth;
    }

    @Override
    public String toString() {
        return "(" + depthVariableName + " <= " + maxDepth + ")";
    }
}
