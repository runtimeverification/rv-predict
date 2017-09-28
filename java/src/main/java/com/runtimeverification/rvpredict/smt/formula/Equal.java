package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * @author TraianSF
 */
public class Equal extends FormulaTerm {
    protected Equal(IntFormula left, IntFormula right) {
        super(BooleanOperation.INT_EQUAL, left, right);
    }

    protected Equal(BoolFormula left, BoolFormula right) {
        super(BooleanOperation.BOOL_EQUAL, left, right);
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }
}
