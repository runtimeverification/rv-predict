package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.formula.Formula;
import com.runtimeverification.rvpredict.smt.formula.SMTLibConstant;
import com.runtimeverification.rvpredict.smt.formula.Sort;
import com.runtimeverification.rvpredict.smt.formula.Term;

/**
 * Created by Traian on 24.02.2015.
 */
public class IntegerConstant extends SMTLibConstant implements Term {

    private final long value;

    private IntegerConstant(long value) {
        super(Sort.Int);
        this.value = value;
    }

    @Override
    public String getValue() {
        return String.valueOf(value);
    }
}
