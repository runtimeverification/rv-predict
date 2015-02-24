package com.runtimeverification.rvpredict.smt.formula;

public class BooleanConstant extends SMTConstant implements Formula {
    public static final BooleanConstant TRUE = new BooleanConstant(true);
    public static final BooleanConstant FALSE = new BooleanConstant(false);
    private final boolean value;

    private BooleanConstant(boolean value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return String.valueOf(value);
    }

    @Override
    public Sort getSort() {
        return Sort.Bool;
    }
}
