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
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ILoggingEngine;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.LLVMTraceCache;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.trace.TraceCache;
import com.runtimeverification.rvpredict.util.Logger;
import com.runtimeverification.rvpredict.util.juc.Executors;
import com.runtimeverification.rvpredict.violation.Violation;

/**
 * Class for predicting violations from a logged execution.
 *
 * Splits the log in segments of length {@link Configuration#windowSize},
 * each of them being executed as a {@link RaceDetectorTask} task.
 */
public class RVPredict {

    private final Set<Violation> violations = Sets.newConcurrentHashSet();
    private final Configuration config;
    private final TraceCache traceCache;
    private final Metadata metadata;

    public RVPredict(Configuration config) {
        this.config = config;
        if (config.isLLVMPrediction()) {
            metadata = Metadata.singleton();
            traceCache = new LLVMTraceCache(config, metadata);
        } else {
            this.metadata = Metadata.readFrom(config.getMetadataPath());
            traceCache = new TraceCache(config, metadata);
        }
    }

    public void start() {
        try {
            ExecutorService raceDetectorExecutor = Executors.newFixedThreadPool(
                    4,
                    new ThreadFactory() {
                        int id = 0;
                        final UncaughtExceptionHandler eh = (t, e) -> {
                            System.err.println("Uncaught exception in " + t + ":");
                            e.printStackTrace();
                            System.exit(1);
                        };

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "Race Detector " + ++id);
                            t.setDaemon(true);
                            t.setUncaughtExceptionHandler(eh);
                            return t;
                        }
                    });

            long fromIndex = 0;
            // process the trace window by window
            Trace trace;
            do {
                trace = traceCache.getTrace(fromIndex, fromIndex += config.windowSize);
                if (trace.hasSharedMemAddr()) {
                    raceDetectorExecutor.execute(new RaceDetectorTask(config, metadata, trace,
                            violations));
                }
            } while (trace.getSize() == config.windowSize);

            shutdownAndAwaitTermination(raceDetectorExecutor);
            if (violations.isEmpty()) {
                config.logger.report("No races found.", Logger.MSGTYPE.INFO);
            }
        } catch (IOException e) {
            System.err.println("Error: I/O error during prediction.");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(config.timeout, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(config.timeout, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public static Thread getPredictionThread(Configuration config, ILoggingEngine loggingEngine) {
        return new Thread("Cleanup Thread") {
            @Override
            public void run() {
                try {
                    loggingEngine.finishLogging();
                } catch (IOException e) {
                    System.err.println("Warning: I/O Error while logging the execution. The log might be unreadable.");
                    System.err.println(e.getMessage());
                }

                if (config.isOfflinePrediction()) {
                    if (config.isLogging()) {
                        config.logger.reportPhase(Configuration.LOGGING_PHASE_COMPLETED);
                    }

                    Process process = null;
                    try {
                        process = startPredictionProcess(config);
                        StreamRedirector.redirect(process);
                        process.waitFor();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        if (process != null) {
                            process.destroy();
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
        /* replace option --dir with --predict */
        int idx = appArgs.indexOf(Configuration.opt_outdir);
        if (idx != -1) {
            appArgs.set(idx, Configuration.opt_only_predict);
        } else {
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
