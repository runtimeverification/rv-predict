package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.IntFormula;

public class LessThan implements ModelRestrict {
    private final IntFormula first;
    private final IntFormula second;
    private final String firstName;
    private final String secondName;

    LessThan(IntFormula first, IntFormula second) {
        this.first = first;
        this.second = second;
        this.firstName = first.toString();
        this.secondName = second.toString();
    }

    @Override
    public FormulaTerm createSmtFormula() {
        return FormulaTerm.LESS_THAN(first, second);
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return variableSource.getValue(firstName).asInt() < variableSource.getValue(secondName).asInt();
    }

    @Override
    public String toString() {
        return "(" + firstName + " < " + secondName + ")";
    }
}
