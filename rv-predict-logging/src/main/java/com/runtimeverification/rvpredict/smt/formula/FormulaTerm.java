package com.runtimeverification.rvpredict.smt.formula;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.visitors.Visitor;

/**
 * A formula constructed from {@link SMTFormula}s through a {@link BooleanOperation}.
 */
public class FormulaTerm extends SMTTerm<BooleanOperation,SMTFormula> implements BoolFormula {

    public static Builder andBuilder() {
        return AndFormula.builder();
    }

    public static Builder orBuilder() {
        return OrFormula.builder();
    }

    public static AndFormula AND(BoolFormula left, BoolFormula right) {
        return new AndFormula(left, right);
    }

    public static OrFormula OR(BoolFormula left, BoolFormula right) {
        return new OrFormula(left, right);
    }

    public static Equal BOOL_EQUAL(BoolFormula left, BoolFormula right) {
        return new Equal(left, right);
    }

    public static Equal INT_EQUAL(IntFormula left, IntFormula right) {
        return new Equal(left, right);
    }

    public static LessThan LESS_THAN(IntFormula left, IntFormula right) {
        return new LessThan(left, right);
    }

    protected FormulaTerm(BooleanOperation operation, ImmutableList<SMTFormula> formulas) {
        super(operation, formulas);
    }

    protected FormulaTerm(BooleanOperation operation, SMTFormula... terms) {
        super(operation, terms);
    }

    protected static Builder builder(BooleanOperation operation) {
        return new Builder(operation);
    }

    public static final class Builder extends SMTTerm.Builder<BooleanOperation,SMTFormula> {
        private Builder(BooleanOperation operation) {
            super(operation);
        }

        @Override
        public FormulaTerm build() {
            return FormulaTerm.of(operation, builder.build());
        }
    }

    private static FormulaTerm of(BooleanOperation operation, ImmutableList<SMTFormula> build) {
        if (operation == BooleanOperation.AND) return new AndFormula(build);
        if (operation == BooleanOperation.OR) return new OrFormula(build);
        return new FormulaTerm(operation, build);
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }
}
