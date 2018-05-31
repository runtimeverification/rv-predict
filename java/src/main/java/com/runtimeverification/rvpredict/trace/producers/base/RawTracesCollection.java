package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.trace.RawTrace;

import java.util.List;

public interface RawTracesCollection {
    List<RawTrace> getTraces();
}
