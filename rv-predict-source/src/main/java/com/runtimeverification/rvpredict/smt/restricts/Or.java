package com.runtimeverification.rvpredict.smt.restricts;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

public class Or implements ModelRestrict {
    private final ImmutableList<ModelRestrict> restricts;

    Or(ModelRestrict... restricts) {
        this(ImmutableList.copyOf(restricts));
    }

    public Or(ImmutableList<ModelRestrict> restricts) {
        this.restricts = restricts;
    }

    @Override
    public FormulaTerm createSmtFormula() {
        FormulaTerm.Builder result = FormulaTerm.orBuilder();
        restricts.forEach(restrict -> result.add(restrict.createSmtFormula()));
        return result.build();
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        for (ModelRestrict restrict : restricts) {
            if (restrict.evaluate(variableSource)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(or");
        for (ModelRestrict restrict : restricts) {
            sb.append(" ");
            sb.append(restrict.toString());
        }
        sb.append(")");
        return sb.toString();
    }
}
