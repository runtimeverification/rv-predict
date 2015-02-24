package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.trace.Event;

/**
 * Created by Traian on 23.02.2015.
 */
public abstract class Variable extends SMTASTNode implements SMTFormula {

    private final Sort sort;
    private final long id;

    protected Variable(Sort sort, Event event) {
        this.sort = sort;
        this.id = event.getGID();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Variable variable = (Variable) o;

        if (id != variable.id) return false;
        if (sort != variable.sort) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sort.hashCode();
        result = 31 * result + (int) (id ^ (id >>> 32));
        return result;
    }

    public abstract String getName();
    
    protected long getId() {
        return id;
    }

    public String getSort() {
        return sort.toString();
    }
}
