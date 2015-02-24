package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * Created by Traian on 23.02.2015.
 */
public class BooleanOperation extends SMTOperation {
    public static final BooleanOperation AND = new BooleanOperation("and") {
        @Override
        public SMTLibConstant getUnit() {
            return BooleanConstant.TRUE;
        }
    };
    public static final BooleanOperation OR = new BooleanOperation("or") {
        @Override
        public SMTLibConstant getUnit() {
            return BooleanConstant.FALSE;
        }
    };
    public static final BooleanOperation NOT = new BooleanOperation("not");
    private BooleanOperation(String name) {
        super(name);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
