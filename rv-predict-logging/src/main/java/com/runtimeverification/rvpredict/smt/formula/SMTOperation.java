package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * Class for representing an operation symbol.
 * 
 * Although {@link #name} would be sufficient for generating the output,
 * {@link #variableArity}, {@link #resultSort}, and {@link #arity} are useful
 * for checking terms are constructed and used consistently.
 */
public class SMTOperation extends SMTASTNode {
    private final String name;
    private final boolean variableArity;
    private final Sort resultSort;
    private final Sort[] arity;

    protected SMTOperation(String name, boolean variableArity, Sort resultSort, Sort... arity) {
        this.name = name;
        this.variableArity = variableArity;
        this.resultSort = resultSort;
        this.arity = arity;
        assert arity.length > 0 : "Operations cannot have arity 0 (see constants).";
    }

    public String getName() {
        return name;
    }
    
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    /**
     * @return  The unit of this operation, or {@code null} if it is not applicable.
     */
    public SMTConstant getUnit() {
        return null;
    }

    /**
     * @return  whether this operation has variable arity.
     */
    public boolean isVariableArity() {
        return variableArity;
    }

    /**
     * @return  the result sort of the operation.
     */
    public Sort getResultSort() {
        return resultSort;
    }

    /**
     * @return  the arity of the operation as an array of {@link Sort}
     */
    public Sort[] getArity() {
        return arity;
    }
    
    public String toString() { return name;}
}
