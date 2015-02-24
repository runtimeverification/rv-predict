package com.runtimeverification.rvpredict.smt.visitors;

import com.runtimeverification.rvpredict.smt.formula.*;

/**
 * Base class for visitors.  Visits all nodes in an AST, but does nothing.
 */
public class BasicVisitor implements Visitor {
    @Override
    public void visit(Benchmark node) {
        node.getAssertion().accept(this);
        visit((SMTASTNode) node);
    }

    @Override
    public void visit(SMTOperation node) {
        visit((SMTASTNode) node);
    }

    @Override
    public void visit(BooleanOperation node) {
        visit((SMTOperation) node);
    }

    @Override
    public void visit(SMTTerm node) {
        node.getOperation().accept(this);
        for (SMTFormula term : node.getTerms()) {
            term.accept(this);
        }
        visit((SMTASTNode) node);
    }

    @Override
    public void visit(FormulaTerm node) {
        visit((SMTTerm) node);
    }

    @Override
    public void visit(SMTConstant node) {
        visit((SMTASTNode) node);
    }

    @Override
    public void visit(BooleanConstant node) {
        visit((SMTConstant) node);
    }

    @Override
    public void visit(SMTVariable node) {
        visit((SMTASTNode) node);
    }

    @Override
    public void visit(BooleanVariable node) {
        visit((SMTVariable) node);
    }

    @Override
    public void visit(AbstractPhiVariable node) {
        visit((BooleanVariable) node);
    }

    @Override
    public void visit(ConcretePhiVariable node) {
        visit((BooleanVariable) node);
    }

    @Override
    public void visit(OrderVariable node) {
        visit((SMTVariable) node);
    }

    @Override
    public void visit(SMTASTNode node) {
    }
}
