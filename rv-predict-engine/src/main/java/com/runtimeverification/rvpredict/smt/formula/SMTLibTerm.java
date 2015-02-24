package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Traian on 23.02.2015.
 */
public class SMTLibTerm extends SMTASTNode implements SMTFormula {
    private final SMTOperation operation;
    private final List<SMTFormula> terms;

    protected SMTLibTerm(SMTOperation operation, SMTFormula... terms) {
        this.operation = operation;
        this.terms = new ArrayList<>(Arrays.asList(terms));
    }
    
    protected void addTerm(SMTFormula term) {
        terms.add(term);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public SMTOperation getOperation() {
        return operation;
    }

    public List<SMTFormula> getTerms() {
        return terms;
    }
}
