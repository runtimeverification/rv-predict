package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

public class IntConstant extends SMTConstant implements IntFormula {

    private final long value;

    public IntConstant(long value) {
        this.value = value;
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getValue() {
        return String.valueOf(value);
    }

}
