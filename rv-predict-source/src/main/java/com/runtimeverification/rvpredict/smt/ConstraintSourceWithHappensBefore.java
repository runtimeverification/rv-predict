package com.runtimeverification.rvpredict.smt;

public interface ConstraintSourceWithHappensBefore extends ConstraintSource {
    void addToMhbClosure(TransitiveClosure.Builder mhbClosureBuilder);
}
