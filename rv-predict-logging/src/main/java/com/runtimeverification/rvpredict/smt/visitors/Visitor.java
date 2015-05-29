package com.runtimeverification.rvpredict.smt.visitors;
// Copyright (c) 2012-2015 K Team. All Rights Reserved.

import com.runtimeverification.rvpredict.smt.formula.*;

public interface Visitor<TResult> {
    TResult getResult();
    void visit(SMTOperation node) throws Exception;
    void visit(BooleanOperation node) throws Exception;
    void visit(SMTTerm<SMTOperation,SMTFormula> node) throws Exception;
    void visit(AndFormula node) throws Exception;
    void visit(OrFormula node) throws Exception;
    void visit(Not node) throws Exception;
    void visit(LessThan node) throws Exception;
    void visit(Equal node) throws Exception;
    void visit(FormulaTerm node) throws Exception;
    void visit(SMTConstant node) throws Exception;
    void visit(BooleanConstant node) throws Exception;
    void visit(IntConstant node) throws Exception;
    void visit(SMTVariable node) throws Exception;
    void visit(BooleanVariable node) throws Exception;
    void visit(ConcretePhiVariable node) throws Exception;
    void visit(OrderVariable node) throws Exception;
    void visit(SMTASTNode node) throws Exception;
}
