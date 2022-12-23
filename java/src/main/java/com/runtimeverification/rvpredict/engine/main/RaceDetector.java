package com.runtimeverification.rvpredict.engine.main;

import com.runtimeverification.rvpredict.performance.AnalysisLimit;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.util.Constants;

import java.util.List;

/**
 * Objects having this interface should be closed.
 */
public interface RaceDetector extends Constants, AutoCloseable {
    List<String> getRaceReports();

    void run(Trace trace, AnalysisLimit analysisLimit);
    void finish();
}
