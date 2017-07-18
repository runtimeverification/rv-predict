/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.runtimeverification.rvpredict.engine.main;

import static com.runtimeverification.rvpredict.config.Configuration.JAVA_EXECUTABLE;
import static com.runtimeverification.rvpredict.config.Configuration.RV_PREDICT_JAR;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ILoggingEngine;
import com.runtimeverification.rvpredict.metadata.CompactMetadata;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.progressindicator.ConsoleOneLineProgressIndicatorUI;
import com.runtimeverification.rvpredict.progressindicator.ProgressIndicator;
import com.runtimeverification.rvpredict.progressindicator.ProgressTimerThread;
import com.runtimeverification.rvpredict.trace.JavaTraceCache;
import com.runtimeverification.rvpredict.trace.LLVMCompactTraceCache;
import com.runtimeverification.rvpredict.trace.LLVMTraceCache;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.trace.TraceCache;
import com.runtimeverification.rvpredict.util.Logger;

/**
 * Class for predicting violations from a logged execution.
 *
 * Splits the log in segments of length {@link Configuration#windowSize},
 * each of them being executed as a {@link RaceDetector} task.
 */
public class RVPredict {

    private final Configuration config;
    private final TraceCache traceCache;
    private final RaceDetector detector;

    public RVPredict(Configuration config) {
        this.config = config;
        if (config.isLLVMPrediction()) {
            if (config.isCompactTrace()) {
                traceCache = new LLVMCompactTraceCache(config, new CompactMetadata());
            } else {
                traceCache = new LLVMTraceCache(config, Metadata.singleton());
            }
        } else {
            traceCache = new JavaTraceCache(
                    config, Metadata.readFrom(config.getMetadataPath(), config.isCompactTrace()));
        }
        this.detector = new RaceDetector(config);
    }

    public void start() {
        try {
            traceCache.setup();
            ProgressIndicator progressIndicator = new ProgressIndicator(
                    traceCache.getFileSize(),
                    config.solver_timeout,
                    new ConsoleOneLineProgressIndicatorUI(),
                    Clock.systemDefaultZone());
            try (ProgressTimerThread ignored = new ProgressTimerThread(progressIndicator)) {
                // process the trace window by window
                Trace trace;
                while (true) {
                    if ((trace = traceCache.getTraceWindow()) != null) {
                        detector.run(trace, progressIndicator);
                        progressIndicator.endWindow(traceCache.getTotalRead());
                    } else {
                        break;
                    }
                }
                progressIndicator.end();
                System.out.println();

                List<String> reports = detector.getRaceReports();
                if (reports.isEmpty()) {
                    config.logger().report("No races found.", Logger.MSGTYPE.INFO);
                } else {
                    reports.forEach(r -> config.logger().report(r, Logger.MSGTYPE.REAL));
                }
                traceCache.getLockGraph().runDeadlockDetection();
            }
        } catch (IOException e) {
            System.err.println("Error: I/O error during prediction.");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Thread getPredictionThread(Configuration config, ILoggingEngine loggingEngine) {
        return new Thread("Cleanup Thread") {
            @Override
            public void run() {
                if (loggingEngine != null) {
                    try {
                        loggingEngine.finishLogging();
                    } catch (IOException e) {
                        System.err.println("Warning: I/O Error while logging the execution. The log might be unreadable.");
                        System.err.println(e.getMessage());
                    }
                }

                if (config.isOfflinePrediction()) {
                    if (config.isLogging()) {
                        config.logger().reportPhase(Configuration.LOGGING_PHASE_COMPLETED);
                    }

                    Process proc = null;
                    try {
                        proc = startPredictionProcess(config);
                        StreamGobbler errorGobbler = StreamGobbler.spawn(proc.getErrorStream(), System.err);
                        StreamGobbler outputGobbler = StreamGobbler.spawn(proc.getInputStream(), System.out);

                        proc.waitFor();

                        // the join() here is necessary even if the gobbler
                        // threads are non-daemon because we are already in the
                        // shutdown hook
                        errorGobbler.join();
                        outputGobbler.join();
                    } catch (IOException | InterruptedException e) {
                        if (proc != null) {
                            proc.destroy();
                        }
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    /**
     * Starts a prediction-only RV-Predict instance in a subprocess.
     */
    private static Process startPredictionProcess(Configuration config) throws IOException {
        List<String> appArgs = new ArrayList<>();
        appArgs.add(JAVA_EXECUTABLE);
        appArgs.add("-ea");
        appArgs.add("-cp");
        appArgs.add(RV_PREDICT_JAR);
        appArgs.add(RVPredict.class.getName());
        int startOfRVArgs = appArgs.size();
        Collections.addAll(appArgs, config.getArgs());

        assert config.isOfflinePrediction();
        if (!appArgs.contains(Configuration.opt_only_predict)) {
            appArgs.add(startOfRVArgs, Configuration.opt_only_predict);
            appArgs.add(startOfRVArgs + 1, config.getLogDir());
        }
        return new ProcessBuilder(appArgs).start();
    }

    /**
     * The entry point of prediction-only RV-Predict started as a subprocess by
     * {@link RVPredict#startPredictionProcess(Configuration)}.
     */
    public static void main(String[] args) {
        Configuration config = Configuration.instance(args);
        new RVPredict(config).start();
    }

}
