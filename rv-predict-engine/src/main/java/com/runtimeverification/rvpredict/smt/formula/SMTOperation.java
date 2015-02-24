package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * Created by Traian on 23.02.2015.
 */
public class SMTOperation extends SMTASTNode {
    private final String name;

    protected SMTOperation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public SMTLibConstant getUnit() {
        return null;
    }
}
