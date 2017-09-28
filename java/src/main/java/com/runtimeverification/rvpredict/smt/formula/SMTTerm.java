package com.runtimeverification.rvpredict.smt.formula;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.visitors.Visitor;

import java.util.List;

/**
 * Base class for SMT terms.  Currently only extended by {@link FormulaTerm}.
 */
public class SMTTerm<Operation extends SMTOperation, Formula extends SMTFormula> extends SMTASTNode implements SMTFormula {
    private final Operation operation;
    private final ImmutableList<Formula> terms;

    /**
     * Builds a new term given an {@code operation} and a list of {@code terms} as arguments.
     *
     * Checks that the given terms match the {@link SMTOperation#arity} of the operation.
     * @param operation
     * @param terms
     */
    public SMTTerm(Operation operation, Formula... terms) {
        this(operation,ImmutableList.copyOf(terms));
        Sort[] arity = operation.getArity();
        assert arity.length == terms.length : "This constructor can only be used for complete terms";
        for (int i = 0; i < terms.length; i++) {
            assert terms[i].getSort() == arity[i] : "Sort not matching arity";
        }
    }

    protected SMTTerm(Operation operation, ImmutableList<Formula> terms) {
        this.operation = operation;
        this.terms = terms;
    }

    @Override
    public void accept(Visitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public Sort getSort() {
        return operation.getResultSort();
    }

    public Operation getOperation() {
        return operation;
    }

    public List<Formula> getTerms() {
        return terms;
    }

    /**
     * Abstract Builder class for SMTTerms.  Works only for operations of variable arity.
     * @param <Operation> An SMTOperation type to be the top operation of the SMTTerm
     * @param <Formula> The type of the arguments of the operation.
     */
    public static abstract class Builder<Operation extends SMTOperation,Formula extends SMTFormula> {
        protected final Operation operation;
        protected final ImmutableList.Builder<Formula> builder;
        private int size;

        protected Builder(Operation operation) {
            this.operation = operation;
            assert operation.isVariableArity() : "Only operations with variable arity can be built.";
            builder = ImmutableList.builder();
        }

        /**
         * Adds new terms to this term builder.
         *
         * Checks that the term matches the corresponding sort of the operation's arity.
         */
        public void add(Formula... formulas) {
            for (Formula formula : formulas) {
                Sort[] arity = operation.getArity();
                if (size < arity.length) {
                    assert formula.getSort() == arity[size] : "Sort not matching arity";
                } else {
                    assert formula.getSort() == arity[arity.length - 1];
                }
                builder.add(formula);
                size++;
            }
        }

        public abstract SMTTerm<Operation,Formula> build();
    }

    @Override
    public String toString() {
        return "(" + operation + ' ' +
                Joiner.on(' ').join(terms) +
                ')';
    }
}
