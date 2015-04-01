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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.LoggingEngine;
import com.runtimeverification.rvpredict.log.LoggingFactory;
import com.runtimeverification.rvpredict.log.LoggingTask;
import com.runtimeverification.rvpredict.log.OfflineLoggingFactory;
import com.runtimeverification.rvpredict.smt.SMTConstraintBuilder;
import com.runtimeverification.rvpredict.smt.formula.Formula;
import com.runtimeverification.rvpredict.trace.Event;
import com.runtimeverification.rvpredict.trace.MemoryAccessEvent;
import com.runtimeverification.rvpredict.trace.MemoryAddr;
import com.runtimeverification.rvpredict.trace.ReadEvent;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.trace.TraceCache;
import com.runtimeverification.rvpredict.trace.WriteEvent;
import com.runtimeverification.rvpredict.util.Logger;
import com.runtimeverification.rvpredict.util.juc.Executors;
import com.runtimeverification.rvpredict.violation.Race;
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
    private final TraceCache traceCache;
    private final LoggingFactory loggingFactory;
    private Thread owner;

    public RVPredict(Configuration config, LoggingFactory loggingFactory) {
        this.config = config;
        this.loggingFactory = loggingFactory;
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
                    raceDetectorExecutor.execute(new RaceDetectorTask(trace));
                }
            } while (trace.getSize() == config.windowSize);

            shutdownAndAwaitTermination(raceDetectorExecutor);
            if (violations.size() == 0) {
                config.logger.report("No races found.", Logger.MSGTYPE.INFO);
            }
        } catch (InterruptedException e) {
            System.err.println("Error: prediction interrupted.");
            System.err.println(e.getMessage());
            System.exit(1);
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

    @Override
    public void finishLogging() throws InterruptedException {
        loggingFactory.finishLogging();
        owner.join();
    }

    @Override
    public void setOwner(Thread owner) {
        this.owner = owner;
    }

    public static Thread getPredictionThread(Configuration config, LoggingEngine loggingEngine) {
        return new Thread("Cleanup Thread") {
            @Override
            public void run() {
                try {
                    loggingEngine.finishLogging();
                } catch (IOException e) {
                    System.err.println("Warning: I/O Error while logging the execution. The log might be unreadable.");
                    System.err.println(e.getMessage());
                } catch (InterruptedException e) {
                    System.err.println("Warning: Execution is being forcefully ended. Log data might be lost.");
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
        new RVPredict(config, new OfflineLoggingFactory(config)).run();
    }

    /**
     * Detects data races from a given {@link Trace} object
     * <p/>
     * <p/>
     * We analyze memory access events on each shared memory address in the
     * trace separately. For each shared memory address, enumerate all memory
     * access pairs on this address and build the data-abstract feasibility for
     * each of them. Then for each memory access pair, send to the SMT solver
     * its data-abstract feasibility together with the already built must
     * happen-before (MHB) constraints and locking constraints. The pair is
     * reported as a real data race if the solver returns sat.
     * <p/>
     * <p/>
     * To reduce the expensive calls to the SMT solver, we apply three
     * optimizations:
     * <li>Use Lockset + Weak HB algorithm to filter out those memory access
     * pairs that are obviously not data races.
     * <li>Group "equivalent" memory access events to a block and consider them
     * as a single memory access. In short, such block has the property that all
     * memory access events in it have the same causal HB relation with the
     * outside events. Therefore, it is sufficient to consider only one event
     * from each block.
     */
    private class RaceDetectorTask implements Runnable {

        private final Trace trace;

        RaceDetectorTask(Trace trace) {
            this.trace = trace;
        }

        @Override
        public void run() {
            SMTConstraintBuilder cnstrBuilder = new SMTConstraintBuilder(config, trace);

            cnstrBuilder.addIntraThreadConstraints();
            cnstrBuilder.addThreadStartJoinConstraints();
            cnstrBuilder.addLockingConstraints();
            cnstrBuilder.finish();
            /* enumerate each shared memory address in the trace */
            for (MemoryAddr addr : trace.getMemAccessEventsTable().rowKeySet()) {
                /* exclude unsafe address */
                if (trace.isUnsafeAddress(addr)) {
                    continue;
                }

                /* exclude volatile variable */
                if (!config.checkVolatile && trace.isVolatileField(addr)) {
                    continue;
                }

                /* skip if there is no write event */
                if (trace.getWriteEventsOn(addr).isEmpty()) {
                    continue;
                }

                /* skip if there is only one thread */
                if (trace.getMemAccessEventsTable().row(addr).size() == 1) {
                    continue;
                }

                /* group equivalent reads and writes into memory access blocks */
                Map<MemoryAccessEvent, List<MemoryAccessEvent>> equivAccBlk = new LinkedHashMap<MemoryAccessEvent, List<MemoryAccessEvent>>();
                for (Map.Entry<Long, List<MemoryAccessEvent>> entry : trace
                        .getMemAccessEventsTable().row(addr).entrySet()) {
                    // TODO(YilongL): the extensive use of List#indexOf could be a performance problem later

                    long crntTID = entry.getKey();
                    List<MemoryAccessEvent> memAccEvents = entry.getValue();

                    List<Event> crntThrdEvents = trace.getThreadEvents(crntTID);

                    ListIterator<MemoryAccessEvent> iter = memAccEvents.listIterator();
                    while (iter.hasNext()) {
                        MemoryAccessEvent memAcc = iter.next();
                        equivAccBlk.put(memAcc, Lists.newArrayList(memAcc));
                        if (memAcc instanceof WriteEvent) {
                            int prevMemAccIdx = crntThrdEvents.indexOf(memAcc);

                            while (iter.hasNext()) {
                                MemoryAccessEvent crntMemAcc = iter.next();
                                int crntMemAccIdx = crntThrdEvents.indexOf(crntMemAcc);

                                /* ends the block if there is sync/branch-event in between */
                                boolean memAccOnly = true;
                                for (Event e : crntThrdEvents.subList(prevMemAccIdx + 1, crntMemAccIdx)) {
                                    memAccOnly = memAccOnly && (e instanceof MemoryAccessEvent);
                                }
                                if (!memAccOnly) {
                                    iter.previous();
                                    break;
                                }

                                equivAccBlk.get(memAcc).add(crntMemAcc);
                                /* YilongL: without logging branch events, we
                                 * have to be conservative and end the block
                                 * when a read event is encountered */
                                if (crntMemAcc instanceof ReadEvent) {
                                    break;
                                }

                                prevMemAccIdx = crntMemAccIdx;
                            }
                        }
                    }
                }

                /* check memory access pairs */
                for (MemoryAccessEvent fst : equivAccBlk.keySet()) {
                    for (MemoryAccessEvent snd : equivAccBlk.keySet()) {
                        if (fst.getTID() >= snd.getTID()) {
                            continue;
                        }

                        /* skip if all potential data races are already known */
                        Set<Race> potentialRaces = Sets.newHashSet();
                        for (MemoryAccessEvent e1 : equivAccBlk.get(fst)) {
                            for (MemoryAccessEvent e2 : equivAccBlk.get(snd)) {
                                if ((e1 instanceof WriteEvent || e2 instanceof WriteEvent)
                                        && !trace.isClinitMemoryAccess(e1)
                                        && !trace.isClinitMemoryAccess(e2)) {
                                    potentialRaces.add(new Race(e1, e2, trace, loggingFactory));
                                }
                            }
                        }
                        boolean hasFreshRace = false;
                        for (Race potentialRace : potentialRaces) {
                            hasFreshRace = !violations.contains(potentialRace);
                            if (hasFreshRace) break;
                        }
                        if (!hasFreshRace) {
                            /* YilongL: note that this could lead to miss of data
                             * races if their signatures are the same */
                            continue;
                        }

                        /* not a race if the two events hold a common lock */
                        if (cnstrBuilder.hasCommonLock(fst, snd)) {
                            continue;
                        }

                        /* not a race if one event happens-before the other */
                        if (cnstrBuilder.happensBefore(fst, snd)
                                || cnstrBuilder.happensBefore(snd, fst)) {
                            continue;
                        }

                        /* start building constraints for MCM */
                        Formula[] causalConstraints = new Formula[]{
                                cnstrBuilder.getAbstractFeasibilityConstraint(fst),
                                cnstrBuilder.getAbstractFeasibilityConstraint(snd)
                        };

                        if (cnstrBuilder.isRace(fst, snd, causalConstraints)) {
                            for (Race race : potentialRaces) {
                                if (violations.add(race)) {
                                    String report = config.simple_report ?
                                            race.toString() : race.generateRaceReport();
                                    config.logger.report(report, Logger.MSGTYPE.REAL);
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
