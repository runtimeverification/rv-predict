package com.runtimeverification.rvpredict.smt;

public interface RestrictSourceWithHappensBefore extends RestrictSource {
    void addToMhbClosure(TransitiveClosure.Builder mhbClosureBuilder);
}
