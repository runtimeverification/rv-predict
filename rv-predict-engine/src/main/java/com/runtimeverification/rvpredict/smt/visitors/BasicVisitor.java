package com.runtimeverification.rvpredict.smt.visitors;

import com.runtimeverification.rvpredict.smt.formula.*;

import java.util.Objects;

/**
 * Base class for visitors.  Visits all nodes in an AST, but does nothing.
 */
public class BasicVisitor<TResult> implements Visitor<TResult> {

    @Override
    public TResult getResult() {
        return null;
    }

    @Override
    public void visit(SMTOperation node) throws Exception {
        visit((SMTASTNode) node);
    }

    @Override
    public void visit(BooleanOperation node) throws Exception {
        visit((SMTOperation) node);
    }

    @Override
    public void visit(SMTTerm node) throws Exception {
        SMTTerm<SMTOperation,SMTFormula> term = (SMTTerm<SMTOperation,SMTFormula>)node;
        node.getOperation().accept(this);
        for (SMTFormula sterm : term.getTerms()) {
            sterm.accept(this);
        }
        visit((SMTASTNode) node);
    }

    @Override
    public void visit(AndFormula node) throws Exception {
        visit((FormulaTerm) node);
    }

    @Override
    public void visit(OrFormula node) throws Exception {
        visit((FormulaTerm) node);
    }

    @Override
    public void visit(Not node) throws Exception {
        visit((FormulaTerm) node);
    }

    @Override
    public void visit(LessThan node) throws Exception {
        visit((FormulaTerm) node);
    }

    @Override
    public void visit(Equal node) throws Exception {
        visit((FormulaTerm) node);
    }

    @Override
    public void visit(FormulaTerm node) throws Exception {
        visit((SMTTerm) node);
    }

    @Override
    public void visit(SMTConstant node) throws Exception {
        visit((SMTASTNode) node);
    }

    @Override
    public void visit(BooleanConstant node) throws Exception {
        visit((SMTConstant) node);
    }

    @Override
    public void visit(SMTVariable node) throws Exception {
        visit((SMTASTNode) node);
    }

    @Override
    public void visit(BooleanVariable node) throws Exception {
        visit((SMTVariable) node);
    }

    @Override
    public void visit(AbstractPhiVariable node) throws Exception {
        visit((BooleanVariable) node);
    }

    @Override
    public void visit(ConcretePhiVariable node) throws Exception {
        visit((BooleanVariable) node);
    }

    @Override
    public void visit(OrderVariable node) throws Exception {
        visit((SMTVariable) node);
    }

    @Override
    public void visit(SMTASTNode node) throws Exception {
    }
}
