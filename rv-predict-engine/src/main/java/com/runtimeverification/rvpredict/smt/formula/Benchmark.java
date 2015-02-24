package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * Representation of a top-level assertion.
 */
public class Benchmark extends SMTASTNode {
    private final String name;
    private final Formula assertion;

    public Benchmark(String name, Formula assertion) {
        this.name = name;
        this.assertion = assertion;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public String getName() {
        return name;
    }

    public Formula getAssertion() {
        return assertion;
    }
}
