package com.runtimeverification.rvpredict.smt.visitors;

import com.runtimeverification.rvpredict.smt.formula.*;

/**
 * Created by Traian on 24.02.2015.
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
    public void visit(Relation node) {
        visit((SMTOperation) node);
    }

    @Override
    public void visit(SMTLibTerm node) {
        node.getOperation().accept(this);
        for (SMTFormula term : node.getTerms()) {
            term.accept(this);
        }
        visit((SMTASTNode) node);
    }

    @Override
    public void visit(FormulaTerm node) {
        visit((SMTLibTerm) node);
    }

    @Override
    public void visit(SortedTerm node) {
        visit((SMTLibTerm) node);
    }

    @Override
    public void visit(SMTLibConstant node) {
        visit((SMTASTNode) node);
    }

    @Override
    public void visit(BooleanConstant node) {
        visit((SMTLibConstant) node);
    }

    @Override
    public void visit(IntegerConstant node) {
        visit((SMTLibConstant) node);
    }

    @Override
    public void visit(Variable node) {
        visit((SMTASTNode) node);
    }

    @Override
    public void visit(BooleanVariable node) {
        visit((Variable) node);
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
        visit((Variable) node);
    }

    @Override
    public void visit(SMTASTNode node) {
    }
}
