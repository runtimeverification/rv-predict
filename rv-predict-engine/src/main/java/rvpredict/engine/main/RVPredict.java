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
package rvpredict.engine.main;

import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

import rvpredict.config.Configuration;
import rvpredict.db.DBEngine;
import rvpredict.trace.Trace;
import rvpredict.trace.TraceInfo;
import rvpredict.util.Logger;
import violation.Violation;

public class RVPredict {

    private final ConcurrentMap<Violation,Boolean> violations = new ConcurrentHashMap<>();
    private final HashSet<Violation> potentialviolations = new HashSet<>();
    private final Configuration config;
    private final Logger logger;
    private final long totalTraceLength;
    private final DBEngine dbEngine;
    private final TraceInfo traceInfo;

    public RVPredict(Configuration config) {
        this.config = config;
        logger = config.logger;

        long startTime = System.currentTimeMillis();

        dbEngine = new DBEngine(config.outdir);

        // the total number of events in the trace
        totalTraceLength = dbEngine.getTraceLength();
        traceInfo = new TraceInfo(dbEngine.getVolatileFieldIds(),
                dbEngine.getVarIdToVarSig(),
                dbEngine.getLocIdToStmtSig());

        addHooks(startTime);
    }

    private void addHooks(long startTime) {
        // register a shutdown hook to store runtime statistics
        Runtime.getRuntime().addShutdownHook(
                new ExecutionInfoTask(startTime, traceInfo, totalTraceLength));

        // set a timer to timeout in a configured period
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.report("\n******* Timeout " + config.timeout + " seconds ******",
                        Logger.MSGTYPE.REAL);// report it
                System.exit(0);
            }
        }, config.timeout * 1000);
    }

    public void run() {
        ExecutorService raceDetectorExecutor = Executors.newFixedThreadPool(8);
        Trace.State initState = new Trace.State();

        // process the trace window by window
        for (int n = 0; n * config.windowSize < totalTraceLength; n++) {
            long fromIndex = n * config.windowSize + 1;
            long toIndex = fromIndex + config.windowSize;

            Trace trace = dbEngine.getTrace(fromIndex, toIndex, initState, traceInfo);

            if (trace.hasSharedMemAddr()) {

                final RaceDetectorThread raceDetectorThread = new RaceDetectorThread(this, trace);
                raceDetectorExecutor.execute(raceDetectorThread);
            }

            initState = trace.getFinalState();
        }
        shutdownAndAwaitTermination(raceDetectorExecutor);
        System.exit(0);
    }


    void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public ConcurrentMap<Violation, Boolean> getViolations() {
        return violations;
    }

    public Configuration getConfig() {
        return config;
    }

    public Logger getLogger() {
        return logger;
    }

    class ExecutionInfoTask extends Thread {
        TraceInfo info;
        long start_time;
        long TOTAL_TRACE_LENGTH;

        ExecutionInfoTask(long st, TraceInfo info, long size) {
            this.info = info;
            this.start_time = st;
            this.TOTAL_TRACE_LENGTH = size;
        }

        @Override
        public void run() {

            // Report statistics about the trace and race detection

            // TODO: query the following information from DB may be expensive

            int TOTAL_THREAD_NUMBER = info.getTraceThreadNumber();
            int TOTAL_SHAREDVARIABLE_NUMBER = info.getTraceSharedVariableNumber();
            int TOTAL_BRANCH_NUMBER = info.getTraceBranchNumber();
            int TOTAL_SHAREDREADWRITE_NUMBER = info.getTraceSharedReadWriteNumber();
            int TOTAL_LOCALREADWRITE_NUMBER = info.getTraceLocalReadWriteNumber();
            int TOTAL_INITWRITE_NUMBER = info.getTraceInitWriteNumber();

            int TOTAL_SYNC_NUMBER = info.getTraceSyncNumber();
            int TOTAL_PROPERTY_NUMBER = info.getTracePropertyNumber();

            if (violations.size() == 0)
                logger.report("No races found.", Logger.MSGTYPE.INFO);
            else {
                logger.report("Trace Size: " + TOTAL_TRACE_LENGTH, Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Threads: " + TOTAL_THREAD_NUMBER, Logger.MSGTYPE.STATISTICS);
                logger.report("Total #SharedVariables: " + TOTAL_SHAREDVARIABLE_NUMBER,
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Shared Read-Writes: " + TOTAL_SHAREDREADWRITE_NUMBER,
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Local Read-Writes: " + TOTAL_LOCALREADWRITE_NUMBER,
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Initial Writes: " + TOTAL_INITWRITE_NUMBER,
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Synchronizations: " + TOTAL_SYNC_NUMBER,
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Branches: " + TOTAL_BRANCH_NUMBER, Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Property Events: " + TOTAL_PROPERTY_NUMBER,
                        Logger.MSGTYPE.STATISTICS);

                logger.report("Total #Potential Violations: "
                        + (potentialviolations.size() + violations.size()),
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total #Real Violations: " + violations.size(),
                        Logger.MSGTYPE.STATISTICS);
                logger.report("Total Time: " + (System.currentTimeMillis() - start_time) + "ms",
                        Logger.MSGTYPE.STATISTICS);
            }

            logger.closePrinter();

        }

    }

}
