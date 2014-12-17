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
import java.util.ListIterator;
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
import smt.SMTConstraintBuilder;
import violation.Race;
import violation.Violation;

public class RVPredict {

    private final HashSet<Violation> violations = new HashSet<Violation>();
    private final HashSet<Violation> potentialviolations = new HashSet<Violation>();
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

        // load all the metadata in the application
        Set<Integer> volatileFieldIds = new HashSet<>();
        Map<Integer, String> locIdToStmtSig = new HashMap<>();
        dbEngine.getMetadata(volatileFieldIds, locIdToStmtSig);

        // the total number of events in the trace
        totalTraceLength = dbEngine.getTraceSize();

        traceInfo = new TraceInfo(volatileFieldIds, locIdToStmtSig);

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
     * Detects data races from a given trace.
     *
     * <p>
     * We analyze memory access events on each shared memory address in the
     * trace separately. For each shared memory address, enumerate all memory
     * access pairs on this address and build the data-abstract feasibility for
     * each of them. Then for each memory access pair, send to the SMT solver
     * its data-abstract feasibility together with the already built must
     * happen-before (MHB) constraints and locking constraints. The pair is
     * reported as a real data race if the solver returns sat.
     *
     * <p>
     * To reduce the expensive calls to the SMT solver, we apply three
     * optimizations:
     * <li>Use Lockset + Weak HB algorithm to filter out those memory access
     * pairs that are obviously not data races.
     * <li>Group "equivalent" memory access events to a block and consider them
     * as a single memory access. In short, such block has the property that all
     * memory access events in it have the same causal HB relation with the
     * outside events. Therefore, it is sufficient to consider only one event
     * from each block.
     *
     * @param cnstrBuilder
     * @param trace
     *            the trace to analyze
     */
    private void detectRace(SMTConstraintBuilder cnstrBuilder, Trace trace) {
        /* enumerate each shared memory address in the trace */
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

            /* group equivalent reads and writes into memory access blocks */
            Map<MemoryAccessEvent, List<MemoryAccessEvent>> equivAccBlk = new LinkedHashMap<>();
            for (Entry<Long, List<MemoryAccessEvent>> entry : trace
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

                            /* ends the block if there is sync/branch event in between */
                            boolean memAccOnly = true;
                            for (Event e : crntThrdEvents.subList(prevMemAccIdx + 1, crntMemAccIdx)) {
                                memAccOnly = memAccOnly && (e instanceof MemoryAccessEvent);
                            }
                            if (!memAccOnly) {
                                iter.previous();
                                break;
                            }

                            equivAccBlk.get(memAcc).add(crntMemAcc);
                            if (!config.branch) {
                                /* YilongL: without logging branch events, we
                                 * have to be conservative and end the block
                                 * when a read event is encountered */
                                if (crntMemAcc instanceof ReadEvent) {
                                    break;
                                }
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
                            if (e1 instanceof WriteEvent || e2 instanceof WriteEvent) {
                                potentialRaces.add(new Race(e1, e2, trace.getLocIdToStmtSigMap()));
                            }
                        }
                    }
                    if (violations.containsAll(potentialRaces)) {
                        /* YilongL: note that this could lead to miss of data
                         * races if their signatures are the same */
                        continue;
                    }

                    /* not a race if the two events hold a common lock */
                    if (cnstrBuilder.hasCommonLock(fst, snd)) {
                        continue;
                    }

                    /* not a race if one event happens-before the other */
                    if (fst.getGID() < snd.getGID()
                            && cnstrBuilder.happensBefore(fst, snd)
                            || fst.getGID() > snd.getGID()
                            && cnstrBuilder.happensBefore(snd, fst)) {
                        continue;
                    }

                    /* start building constraints for MCM */
                    StringBuilder sb = cnstrBuilder.addReadWriteConsistencyConstraints(fst);
                    sb.append(cnstrBuilder.addReadWriteConsistencyConstraints(snd));

                    if (cnstrBuilder.isRace(fst, snd, sb)) {
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

    public void run() {
        Map<String, Long> initValues = new HashMap<>();

        // process the trace window by window
        for (int n = 0; n * config.windowSize < totalTraceLength; n++) {
            long fromIndex = n * config.windowSize + 1;
            long toIndex = fromIndex + config.windowSize;

            Trace trace = dbEngine.getTrace(fromIndex, toIndex, initValues, traceInfo);

            if (trace.hasSharedMemAddr()) {
                SMTConstraintBuilder cnstrBuilder = new SMTConstraintBuilder(config, trace);

                cnstrBuilder.declareVariables();
                if (config.rmm_pso) {
                    cnstrBuilder.addPSOIntraThreadConstraints();
                } else {
                    cnstrBuilder.addIntraThreadConstraints();
                }
                cnstrBuilder.addMHBConstraints();
                cnstrBuilder.addLockingConstraints();

                detectRace(cnstrBuilder, trace);
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
