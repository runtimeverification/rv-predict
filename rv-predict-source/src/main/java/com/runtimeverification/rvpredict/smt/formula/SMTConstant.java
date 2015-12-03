package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * Type of constants. Common functionality for both Boolean and sorted constants.
 */
public abstract class SMTConstant extends SMTASTNode implements SMTFormula {
    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }

    public abstract String getValue();

    @Override
    public String toString() {
        return getValue();
    }
}
