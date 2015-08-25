package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * @author TraianSF
 */
public class LessThan extends FormulaTerm {
    public LessThan(IntFormula left, IntFormula right) {
        super(BooleanOperation.LESS_THAN, left, right);
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }
}
