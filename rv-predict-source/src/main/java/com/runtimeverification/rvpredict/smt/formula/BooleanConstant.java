package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

public class BooleanConstant extends SMTConstant implements BoolFormula {
    public static final BooleanConstant TRUE = new BooleanConstant(true);
    public static final BooleanConstant FALSE = new BooleanConstant(false);
    private final boolean value;

    private BooleanConstant(boolean value) {
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
