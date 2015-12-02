package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * An operation symbol whose return sort is {@link Sort#Bool}.
 * @see SMTOperation
 */
public class BooleanOperation extends SMTOperation {
    /**
     * {@code and} takes arbitrarily many arguments of sort {@link Sort#Bool}
     * and has unit {@link BooleanConstant#TRUE}
     */
    public static final BooleanOperation AND  =
            new BooleanOperation("and", true, Sort.Bool, Sort.Bool) {
                @Override
                public SMTConstant getUnit() {
                    return BooleanConstant.TRUE;
                }
            };
    /**
     * {@code or} takes arbitrarily many arguments of sort {@link Sort#Bool}
     * and has unit {@link BooleanConstant#FALSE}
     */
    public static final BooleanOperation OR =
            new BooleanOperation("or", true, Sort.Bool, Sort.Bool) {
                @Override
                public SMTConstant getUnit() {
                    return BooleanConstant.FALSE;
                }
            };
    /**
     * {@code not} takes one argument of sort {@link Sort#Bool}
     */
    public static final BooleanOperation NOT = 
            new BooleanOperation("not", false, Sort.Bool);
    /**
     * {@code <} takes two arguments of sort {@link Sort#Int}
     */
    public static final BooleanOperation LESS_THAN = 
            new BooleanOperation("<", false, Sort.Int, Sort.Int);
    /**
     * {@code =} on integers takes two arguments of sort {@link Sort#Int}
     */
    public static final BooleanOperation INT_EQUAL =
            new BooleanOperation("=", false, Sort.Int, Sort.Int);
    /**
     * {@code =} on booleans takes two arguments of sort {@link Sort#Bool}
     */
    public static final BooleanOperation BOOL_EQUAL =
            new BooleanOperation("=", false, Sort.Bool, Sort.Bool);

    private BooleanOperation(String name, boolean variableArity, Sort... arity) {
        super(name, variableArity, Sort.Bool, arity);
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }
}
