package com.runtimeverification.rvpredict.smt.constraints;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

public class Or implements ModelConstraint {
    private final ImmutableList<ModelConstraint> constraints;

    Or(ModelConstraint... constraints) {
        this(ImmutableList.copyOf(constraints));
    }

    public Or(ImmutableList<ModelConstraint> constraints) {
        this.constraints = constraints;
    }

    @Override
    public FormulaTerm createSmtFormula() {
        FormulaTerm.Builder result = FormulaTerm.orBuilder();
        constraints.forEach(constraint -> result.add(constraint.createSmtFormula()));
        return result.build();
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        for (ModelConstraint constraint : constraints) {
            if (constraint.evaluate(variableSource)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(or");
        for (ModelConstraint constraint : constraints) {
            sb.append(" ");
            sb.append(constraint.toString());
        }
        sb.append(")");
        return sb.toString();
    }
}
