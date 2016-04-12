package com.runtimeverification.rvpredict.engine.main;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.ForkPoint;
import com.runtimeverification.rvpredict.trace.LLVMTraceCache;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.trace.TraceCache;
import com.runtimeverification.rvpredict.util.Logger;
import com.sun.xml.internal.bind.annotation.OverrideAnnotationOf;

import java.io.IOException;
import java.util.List;

/**
 * Modification of the RVPredict class
 * tailored to LLVM's fork scheme
 * @author EricPtS
 */
public class RVPredictLLVM extends RVPredict {

    @Override
    Metadata initMetadata() {
        return Metadata.singleton();
    }

    @Override
    TraceCache initTraceCache() {
        return new LLVMTraceCache(config, metadata, "", null);
    }

    RVPredictLLVM(Configuration config) {
        super(config);
    }

    private void run(String pid, ForkPoint forkPoint) {
        TraceCache forkTraceCache = new LLVMTraceCache(config, metadata, pid, forkPoint);
        long fromIndex = forkPoint.getFromIndex();
        try {
            forkTraceCache.setup();
            Trace trace;
            while (true) {
                if ((trace = forkTraceCache.getTrace(fromIndex)) != null) {
                    fromIndex += config.windowSize;
                    detector.run(trace);
                } else {
                    break;
                }
            }
            metadata.setFork();
            forkTraceCache.getForks().forEach((k, v) -> run(k + "-", v));
        } catch (IOException e) {
            System.err.println("Error: I/O error during prediction.");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        forkTraceCache.getCrntState().getLockGraph().runDeadlockDetection();
    }

    @Override
    public void start() {
        run("", new ForkPoint(traceCache.getCrntState(), 0));
        List<String> reports = detector.getRaceReports();

        if (reports.isEmpty()) {
            config.logger().report("No races found.", Logger.MSGTYPE.INFO);
        } else {
            reports.forEach(r -> config.logger().report(r, Logger.MSGTYPE.REAL));
        }
    }
}
