package com.runtimeverification.rvpredict.smt.visitors;
// Copyright (c) 2012-2015 K Team. All Rights Reserved.

import com.runtimeverification.rvpredict.smt.formula.BooleanConstant;
import com.runtimeverification.rvpredict.smt.formula.IntegerConstant;
import com.runtimeverification.rvpredict.smt.formula.*;

public interface Visitor {
    public void visit(Benchmark node);
    public void visit(SMTOperation node);
    public void visit(BooleanOperation node);
    public void visit(Relation node);
    public void visit(SMTLibTerm node);
    public void visit(FormulaTerm node);
    public void visit(SortedTerm node);
    public void visit(SMTLibConstant node);
    public void visit(BooleanConstant node);
    public void visit(IntegerConstant node);
    public void visit(Variable node);
    public void visit(BooleanVariable node);
    public void visit(AbstractPhiVariable node);
    public void visit(ConcretePhiVariable node);
    public void visit(OrderVariable node);
    public void visit(SMTASTNode node);
}

