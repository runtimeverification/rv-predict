package com.runtimeverification.rvpredict.smt.constraints;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

public class And implements ModelConstraint {
    private final ImmutableList<ModelConstraint> constraints;

    public And(ImmutableList<ModelConstraint> constraints) {
        this.constraints = constraints;
    }

    public And(ModelConstraint... constraints) {
        this.constraints = ImmutableList.copyOf(constraints);
    }

    @Override
    public FormulaTerm createSmtFormula() {
        FormulaTerm.Builder result = FormulaTerm.andBuilder();
        constraints.forEach(constraint -> result.add(constraint.createSmtFormula()));
        return result.build();
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        for (ModelConstraint constraint : constraints) {
            if (!constraint.evaluate(variableSource)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(and");
        for (ModelConstraint constraint : constraints) {
            sb.append(" ");
            sb.append(constraint.toString());
        }
        sb.append(")");
        return sb.toString();
    }
}
