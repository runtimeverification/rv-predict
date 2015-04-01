package com.runtimeverification.rvpredict.smt.formula;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * @author TraianSF
 */
public class AndFormula extends FormulaTerm {
    protected AndFormula(ImmutableList<SMTFormula> formulas) {
        super(BooleanOperation.AND, formulas);
    }

    protected AndFormula(SMTFormula... terms) {
        super(BooleanOperation.AND, terms);
    }

    public static Builder builder() {
        return builder(BooleanOperation.AND);
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }
}
