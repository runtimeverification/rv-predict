package com.runtimeverification.rvpredict.smt.formula;

/**
 * Created by Traian on 24.02.2015.
 */
public class BooleanEquality extends FormulaTerm {
    public BooleanEquality(Formula term1, Formula term2) {
        super(Relation.EQUAL, term1, term2);
    }
}
