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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import rvpredict.config.Configuration;
import rvpredict.db.DBEngine;
import rvpredict.trace.Event;
import rvpredict.trace.MemoryAccessEvent;
import rvpredict.trace.ReadEvent;
import rvpredict.trace.Trace;
import rvpredict.trace.TraceInfo;
import rvpredict.trace.WriteEvent;
import rvpredict.util.Logger;
import smt.EngineSMTLIB1;
import violation.Race;
import violation.Violation;

/**
 * The NewRVPredict class implements our new race detection algorithm based on
 * constraint solving. The events in the trace are loaded and processed window
 * by window with a configurable window size.
 *
 * @author jeffhuang
 *
 */
public class NewRVPredict {

    private final HashSet<Violation> violations = new HashSet<Violation>();
    private final HashSet<Violation> potentialviolations = new HashSet<Violation>();
    private final Configuration config;
    private final Logger logger;
    private final long totalTraceLength;
    private final DBEngine dbEngine;
    private final TraceInfo traceInfo;

    public NewRVPredict(Configuration config) {
        this.config = config;
        logger = config.logger;

        long startTime = System.currentTimeMillis();

        dbEngine = new DBEngine(config.outdir);

        // load all the metadata in the application
        Set<Integer> volatileFieldIds = new HashSet<>();
        Map<Integer, String> stmtIdSigMap = new HashMap<>();
        dbEngine.getMetadata(volatileFieldIds, stmtIdSigMap);

        // the total number of events in the trace
        totalTraceLength = dbEngine.getTraceSize();

        traceInfo = new TraceInfo(volatileFieldIds, stmtIdSigMap);

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

    /**
     * Race detection method. For every pair of conflicting data accesses, the
     * corresponding race constraint is generated and solved by a solver. If the
     * solver returns a solution, we report a real data race. Otherwise, a
     * potential race is reported. We call it a potential race but not a false
     * race because it might be a real data race in another trace.
     *
     * @param engine
     * @param trace
     */
    private void detectRace(EngineSMTLIB1 engine, Trace trace) {
        // implement potentialraces to be exact match

        // sometimes we choose an un-optimized way to implement things faster,
        // easier
        // e.g., here we use check, but still enumerate read/write
        for (String addr : trace.getMemAccessEventsTable().rowKeySet()) {
            /* exclude volatile variable */
            if (config.novolatile && trace.isVolatileAddr(addr)) {
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

            // find equivalent reads and writes by the same thread
            Map<MemoryAccessEvent, List<MemoryAccessEvent>> equivMap = new LinkedHashMap<>();

            // skip non-primitive and array variables?
            // because we add branch operations before their operations
            for (Entry<Long, List<MemoryAccessEvent>> entry : trace
                    .getMemAccessEventsTable().row(addr).entrySet()) {
                // TODO(YilongL): the extensive use of List#indexOf could be a performance problem later

                long crntTID = entry.getKey();
                List<MemoryAccessEvent> memAccEvents = entry.getValue();

                List<Event> crntThrdEvents = trace.getThreadEvents(crntTID);

                // first memory access event of a block
                MemoryAccessEvent fstMemAcc = memAccEvents.get(0);
                int fstMemAccIdx = crntThrdEvents.indexOf(fstMemAcc);
                equivMap.put(fstMemAcc, Lists.newArrayList(fstMemAcc));

                int prevMemAccIdx = fstMemAccIdx;
                for (MemoryAccessEvent crntMemAcc : memAccEvents.subList(1, memAccEvents.size())) {
                    int crntMemAccIdx = crntThrdEvents.indexOf(crntMemAcc);

                    boolean memAccOnly = true;
                    for (Event e : crntThrdEvents.subList(prevMemAccIdx + 1, crntMemAccIdx)) {
                        memAccOnly = memAccOnly && (e instanceof MemoryAccessEvent);
                    }

                    if (memAccOnly) {
                        equivMap.get(fstMemAcc).add(crntMemAcc);
                    } else {
                        fstMemAcc = crntMemAcc;
                        fstMemAccIdx = crntMemAccIdx;
                        equivMap.put(fstMemAcc, Lists.newArrayList(fstMemAcc));
                    }
                    prevMemAccIdx = crntMemAccIdx;
                }
            }

            /* check conflicting pairs */
            for (MemoryAccessEvent fst : equivMap.keySet()) {
                for (MemoryAccessEvent snd : equivMap.keySet()) {
                    if (fst.getTID() < snd.getTID()) {
                        continue;
                    }

                    /* skip if all potential data races are already known */
                    Set<Race> potentialRaces = Sets.newHashSet();
                    for (MemoryAccessEvent e1 : equivMap.get(fst)) {
                        for (MemoryAccessEvent e2 : equivMap.get(snd)) {
                            if (e1 instanceof WriteEvent || e2 instanceof WriteEvent) {
                                potentialRaces.add(new Race(trace.getStmtSigIdMap().get(e1.getID()),
                                        trace.getStmtSigIdMap().get(e2.getID()), e1.getID(),
                                        e2.getID()));
                            }
                        }
                    }
                    if (violations.containsAll(potentialRaces)) {
                        /* YilongL: note that this could lead to miss of data
                         * races if their signatures are the same */
                        continue;
                    }

                    /* not a race if the two events hold a common lock */
                    if (engine.hasCommonLock(fst, snd)) {
                        continue;
                    }

                    /* not a race if one event happens-before the other */
                    if (fst.getGID() < snd.getGID()
                            && engine.canReach(fst, snd)
                            || fst.getGID() > snd.getGID()
                            && engine.canReach(snd, fst)) {
                        continue;
                    }

                    /* start building constraints for MCM */
                    List<ReadEvent> readNodes_r = trace.getDependentReadNodes(
                            fst, config.branch);
                    List<ReadEvent> readNodes_w = trace.getDependentReadNodes(
                            snd, config.branch);

                    StringBuilder sb1 = engine
                            .constructCausalReadWriteConstraintsOptimized(
                                    fst.getGID(), readNodes_r,
                                    trace);
                    StringBuilder sb2 = engine
                            .constructCausalReadWriteConstraintsOptimized(-1,
                                    readNodes_w, trace);
                    StringBuilder sb = sb1.append(sb2);


                    if (engine.isRace(fst, snd, sb)) {
                        for (Race r : potentialRaces) {
                            if (violations.add(r)) {
                                logger.report(r.toString(), Logger.MSGTYPE.REAL);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The input is the application name and the optional options
     *
     * @param args
     */
    public static void main(String[] args) {
        Configuration config = new Configuration();
        config.parseArguments(args, true);
        config.outdir = "./log";
        NewRVPredict predictor = new NewRVPredict(config);
        predictor.run();
    }

    public void run() {
        EngineSMTLIB1 engine = new EngineSMTLIB1(config);
        Map<String, Long> initValues = new HashMap<>();

        // process the trace window by window
        for (int round = 0; round * config.window_size < totalTraceLength; round++) {
            long index_start = round * config.window_size + 1;
            long index_end = (round + 1) * config.window_size;
            // if(totalTraceLength>rvpredict.config.window_size)System.out.println("***************** Round "+(round+1)+": "+index_start+"-"+index_end+"/"+totalTraceLength+" ******************\n");

            // load trace
            Trace trace = dbEngine.getTrace(index_start, index_end, initValues, traceInfo);

            // OPT: if #sv==0 or #shared rw ==0 continue
            if (trace.mayRace()) {
                // Now, construct the constraints

                // 1. declare all variables
                engine.declareVariables(trace.getFullTrace());
                // 2. intra-thread order for all nodes, excluding branches
                // and basic block transitions
                if (config.rmm_pso)// TODO: add intra order between sync
                    engine.addPSOIntraThreadConstraints(trace.getMemAccessEventsTable());
                else
                    engine.addIntraThreadConstraints(trace.getThreadIdToEventsMap());

                // 3. order for locks, signals, fork/joins
                engine.addSynchronizationConstraints(trace, trace.getSyncNodesMap(),
                        trace.getThreadFirstNodeMap(), trace.getThreadLastNodeMap());

                // 4. match read-write
                // This is only used for constructing all read-write
                // consistency constraints

                // engine.addReadWriteConstraints(trace.getIndexedReadNodes(),trace.getIndexedWriteNodes());
                // engine.addReadWriteConstraints(trace.getIndexedReadNodes(),trace.getIndexedWriteNodes());

                detectRace(engine, trace);
            }

            /* use the final values of the current window as the initial values
             * of the next window */
            initValues = trace.getFinalValues();
        }
        System.exit(0);
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
