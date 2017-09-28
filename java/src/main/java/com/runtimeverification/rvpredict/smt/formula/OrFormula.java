package com.runtimeverification.rvpredict.smt.formula;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * @author TraianSF
 */
public class OrFormula extends FormulaTerm {
    protected OrFormula(ImmutableList<SMTFormula> formulas) {
        super(BooleanOperation.OR, formulas);
    }

    protected OrFormula(SMTFormula... terms) {
        super(BooleanOperation.OR, terms);
    }

    protected static Builder builder() {
        return builder(BooleanOperation.OR);
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }
}
