package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * @author TraianSF
 */
public class Equal extends FormulaTerm {
    protected Equal(OrderVariable left, OrderVariable right) {
        super(BooleanOperation.INT_EQUAL, left, right);
    }

    protected Equal(Formula left, Formula right) {
        super(BooleanOperation.BOOL_EQUAL, left, right);
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }
}
