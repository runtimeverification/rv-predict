package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * Created by Traian on 24.02.2015.
 */
public abstract class SMTLibConstant extends SMTASTNode implements SMTFormula {
    private final Sort sort;

    protected SMTLibConstant(Sort sort) {
        this.sort = sort;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public Sort getSort() {
        return sort;
    }

    public abstract String getValue();
}
