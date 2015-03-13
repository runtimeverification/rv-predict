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

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.LoggingFactory;
import com.runtimeverification.rvpredict.log.LoggingTask;
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
public class RVPredict implements LoggingTask {

    private final Set<Violation> violations = Sets.newConcurrentHashSet();
    private final Configuration config;
    private final Logger logger;
    private final TraceCache traceCache;
    private final LoggingFactory loggingFactory;
    private Thread owner;

    public RVPredict(Configuration config, LoggingFactory loggingFactory) {
        this.config = config;
        this.loggingFactory = loggingFactory;
        logger = config.logger;
        traceCache = new TraceCache(loggingFactory);
    }

    @Override
    public void run() {
        try {
            ExecutorService raceDetectorExecutor = Executors.newFixedThreadPool(4,
                    new ThreadFactory() {
                        int id = 0;
                        final UncaughtExceptionHandler eh = new UncaughtExceptionHandler() {
                            @Override
                            public void uncaughtException(Thread t, Throwable e) {
                                System.err.println("Uncaught exception in " + t + ":");
                                e.printStackTrace();
                                System.exit(1);
                            }
                        };

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "Race Detector " + ++id);
                            t.setDaemon(true);
                            t.setUncaughtExceptionHandler(eh);
                            return t;
                        }
                    });

            long fromIndex = 1;
            // process the trace window by window
            Trace trace;
            do {
                trace = traceCache.getTrace(fromIndex, fromIndex += config.windowSize);

                if (trace.hasSharedMemAddr()) {
                    raceDetectorExecutor.execute(new RaceDetectorTask(this, trace));
                }

            } while (trace.getSize() == config.windowSize);

            shutdownAndAwaitTermination(raceDetectorExecutor);

            if (violations.size() == 0) {
                logger.report("No races found.", Logger.MSGTYPE.INFO);
            }
            logger.closePrinter();

            if (Configuration.online) {
                return;
            } else {
                System.exit(0);
            }
        } catch (InterruptedException e) {
            System.err.println("Error: prediction interrupted.");
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println("Error: I/O error during prediction.");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        System.exit(1);
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

    public Set<Violation> getViolations() {
        return violations;
    }

    public Configuration getConfig() {
        return config;
    }

    public Logger getLogger() {
        return logger;
    }

    public LoggingFactory getLoggingFactory() {
        return loggingFactory;
    }

    @Override
    public void finishLogging() throws InterruptedException {
        loggingFactory.finishLogging();
        owner.join();
    }

    @Override
    public void setOwner(Thread owner) {
        this.owner = owner;
    }
}
