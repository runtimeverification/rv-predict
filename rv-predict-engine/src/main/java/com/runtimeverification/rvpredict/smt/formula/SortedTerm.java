package com.runtimeverification.rvpredict.smt.formula;

/**
 * Created by Traian on 23.02.2015.
 */
public class SortedTerm extends SMTLibTerm implements Term {
    public SortedTerm(Relation operation, Term... terms) {
        super(operation, terms);
    }

}
