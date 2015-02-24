package com.runtimeverification.rvpredict.smt.formula;

/**
 * Created by Traian on 24.02.2015.
 */
public class BooleanConstant extends SMTLibConstant implements Formula {
    public static final BooleanConstant TRUE = new BooleanConstant(true);
    public static final BooleanConstant FALSE = new BooleanConstant(false);
    private final boolean value;

    private BooleanConstant(boolean value) {
        super(Sort.Bool);
        this.value = value;
    }

    @Override
    public String getValue() {
        return String.valueOf(value);
    }
}
