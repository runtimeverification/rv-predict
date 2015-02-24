package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * Created by Traian on 23.02.2015.
 */
public abstract class SMTASTNode {
   public abstract void accept(Visitor visitor);
}
