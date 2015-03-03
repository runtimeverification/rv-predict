package com.runtimeverification.rvpredict.smt.formula;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * A formula constructed from {@link SMTFormula}s through a {@link BooleanOperation}.
 */
public class FormulaTerm extends SMTTerm<BooleanOperation,SMTFormula> implements Formula {

    public static Builder andBuilder() {
        return builder(BooleanOperation.AND);
    }

    public static Builder orBuilder() {
        return builder(BooleanOperation.OR);
    }

    public static FormulaTerm AND(Formula left, Formula right) {
        return new FormulaTerm(BooleanOperation.AND, left, right);
    }

    public static FormulaTerm OR(Formula left, Formula right) {
        return new FormulaTerm(BooleanOperation.OR, left, right);
    }

    public static FormulaTerm BOOL_EQUAL(Formula left, Formula right) {
        return new FormulaTerm(BooleanOperation.BOOL_EQUAL, left, right);
    }

    public static FormulaTerm INT_EQUAL(OrderVariable left, OrderVariable right) {
        return new FormulaTerm(BooleanOperation.INT_EQUAL, left, right);
    }

    public static FormulaTerm LESS_THAN(OrderVariable left, OrderVariable right) {
        return new FormulaTerm(BooleanOperation.LESS_THAN, left, right);
    }

    private FormulaTerm(BooleanOperation operation, ImmutableList<SMTFormula> formulas) {
        super(operation, formulas);
    }

    private FormulaTerm(BooleanOperation operation, SMTFormula... terms) {
        super(operation, terms);
    }

    private static Builder builder(BooleanOperation operation) {
        return new Builder(operation);
    }
    
   public static final class Builder extends SMTTerm.Builder<BooleanOperation,SMTFormula> {
        public Builder(BooleanOperation operation) {
            super(operation);
        }

        public FormulaTerm build() {
            return new FormulaTerm(operation, builder.build());
        }
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
