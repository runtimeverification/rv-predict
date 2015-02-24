package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * Created by Traian on 23.02.2015.
 */
public class Relation extends SMTOperation {
    public static final Relation LESS_THAN = new Relation("<");
    public static final Relation EQUAL = new Relation("=");
    private Relation(String name) {
        super(name);
    }
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
