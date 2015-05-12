package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.log.Event;

/**
 * Type of variables. Common functionality for both Boolean and sorted variables.
 * A variable is uniquely identified by its {@link #id} and by its class type
 * which is reflected by {@link #getNamePrefix()}.
 */
public abstract class SMTVariable extends SMTASTNode implements SMTFormula {

    private final long id;

    protected SMTVariable(Event event) {
        this.id = event.getGID();
    }

    /**
     * @return A prefix for variable names common to all variables of the same class.
     */
    public abstract String getNamePrefix();

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SMTVariable variable = (SMTVariable) o;

        if (id != variable.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }


    @Override
    public String toString() {
        return getNamePrefix() + id;
    }
}
