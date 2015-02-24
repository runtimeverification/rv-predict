package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for SMT terms.  Currently only extended by {@link FormulaTerm}.
 */
public class SMTTerm extends SMTASTNode implements SMTFormula {
    private final SMTOperation operation;
    private final List<SMTFormula> terms;

    /**
     * Builds a new term given an {@code operation} and a list of {@code terms} as arguments.
     * 
     * Checks that the given terms match the {@link SMTOperation#arity} of the operation.
     * @param operation
     * @param terms
     */
    protected SMTTerm(SMTOperation operation, SMTFormula... terms) {
        this.operation = operation;
        Sort[] arity = operation.getArity();
        if (terms.length != arity.length) {
            assert operation.isVariableArity() : "Too many/few terms";
            if (terms.length < arity.length) {
                assert terms.length == arity.length-1 : "Too few terms. Only the final sort can be iterated";

            }
        }
        for (int i = 0; i < terms.length; i++) {
            if (i < arity.length) {
                assert terms[i].getSort() == arity[i] : "Sort not matching arity";
            } else {
                assert terms[i].getSort() == arity[arity.length-1];
            }
        }
        this.terms = new ArrayList<>(Arrays.asList(terms));
    }

    /**
     * Adds a new subterm to this term as an argument on the final position of the operation.
     * 
     * Only enabled when the operation has variable arity.
     * Checks that the term matches the final sort of the arity.
     * @param term
     */
    public void addFormula(SMTFormula term) {
        assert operation.isVariableArity() : "One can add formulas only to variable arity ops";
        Sort[] arity = operation.getArity();
        assert term.getSort() == arity[arity.length-1];
        terms.add(term);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Sort getSort() {
        return operation.getResultSort();
    }

    public SMTOperation getOperation() {
        return operation;
    }

    public List<SMTFormula> getTerms() {
        return terms;
    }
}
