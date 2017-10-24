package com.runtimeverification.rvpredict.engine.main;

import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.util.Constants;

import java.util.List;

public interface RaceDetector extends Constants {
    List<String> getRaceReports();

    void run(Trace trace);
}
