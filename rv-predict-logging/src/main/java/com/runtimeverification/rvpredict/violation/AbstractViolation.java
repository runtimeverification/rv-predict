package com.runtimeverification.rvpredict.violation;

public abstract class AbstractViolation implements Violation {

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

}
