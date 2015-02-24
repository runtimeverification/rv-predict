package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * Base class for all SMT-related classes.  Used for visiting purposes.
 * 
 * @see Visitor
 */
public abstract class SMTASTNode {
   public abstract void accept(Visitor visitor);
}
