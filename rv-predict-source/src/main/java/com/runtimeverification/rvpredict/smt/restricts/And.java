package com.runtimeverification.rvpredict.smt.restricts;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

public class And implements ModelRestrict {
    private final ImmutableList<ModelRestrict> restricts;

    public And(ImmutableList<ModelRestrict> restricts) {
        this.restricts = restricts;
    }

    public And(ModelRestrict... restricts) {
        this.restricts = ImmutableList.copyOf(restricts);
    }

    @Override
    public FormulaTerm createSmtFormula() {
        FormulaTerm.Builder result = FormulaTerm.andBuilder();
        restricts.forEach(restrict -> result.add(restrict.createSmtFormula()));
        return result.build();
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        for (ModelRestrict restrict : restricts) {
            if (!restrict.evaluate(variableSource)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(and");
        for (ModelRestrict restrict : restricts) {
            sb.append(" ");
            sb.append(restrict.toString());
        }
        sb.append(")");
        return sb.toString();
    }
}
