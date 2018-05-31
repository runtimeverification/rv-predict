package com.runtimeverification.rvpredict.trace.producers.base;

import java.util.Collection;

public interface TtidSet {
    Collection<Integer> getTtids();
    boolean contains(int ttid);
}
