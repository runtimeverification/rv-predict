package com.runtimeverification.rvpredict.smt.visitors;
// Copyright (c) 2012-2015 K Team. All Rights Reserved.

import com.runtimeverification.rvpredict.smt.formula.BooleanConstant;
import com.runtimeverification.rvpredict.smt.formula.*;

public interface Visitor {
    public void visit(Benchmark node);
    public void visit(SMTOperation node);
    public void visit(BooleanOperation node);
    public void visit(SMTTerm node);
    public void visit(FormulaTerm node);
    public void visit(SMTConstant node);
    public void visit(BooleanConstant node);
    public void visit(SMTVariable node);
    public void visit(BooleanVariable node);
    public void visit(AbstractPhiVariable node);
    public void visit(ConcretePhiVariable node);
    public void visit(OrderVariable node);
    public void visit(SMTASTNode node);
}

