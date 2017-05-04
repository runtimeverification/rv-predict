package com.runtimeverification.rvpredict.engine.deadlock;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.LLVMSignatureProcessor;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.metadata.SignatureProcessor;
import com.runtimeverification.rvpredict.util.Logger;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for representing the lock acquisition graph abstraction
 * Useful in detecting lock inversion patterns which could lead to deadlocks
 *
 * @author TraianSF
 */
public class LockGraph {
    private final Configuration config;
    private final Metadata metadata;
    private final SCCTarjan<Long> sccGraph = new SCCTarjan<>();
    private final Map<Long,ReadonlyEventInterface> lockEvents = new HashMap<>();
    private final Map<Pair<Long,Long>, Pair<ReadonlyEventInterface, ReadonlyEventInterface>> eventEdges = new HashMap<>();
    private final Map<Integer, Set<Long>> lockSet = new HashMap<>();
    private final SignatureProcessor signatureProcessor;

    public LockGraph(Configuration config, Metadata metadata) {
        this.config = config;
        this.metadata = metadata;
        if (config.isLLVMPrediction()) {
            signatureProcessor = new LLVMSignatureProcessor();
        } else {
            signatureProcessor = new SignatureProcessor();
        }
    }

    /**
     * Maintains the lock acquisition set for each thread and at each new
     * acquisition adds edges to the graph from the existing acquired locks to
     * the newly acquired one.
     *
     * @param event  a lock/unlock event
     */
    public void handle(ReadonlyEventInterface event, int ttid) {
        assert event.isPreLock() || event.isLock() || event.isUnlock();
        long lockId = event.getLockId();
        Set<Long> locks = lockSet.get(ttid);
        if (event.isUnlock()) {
            if (locks == null) {
                reportUnlockOfUnlockedMutex(event);
                return;
            }
            locks.remove(lockId);
            return;
        }
        // else, if event.isLock()
        lockEvents.put(lockId, event);
        if (locks == null) {
            locks = new HashSet<>();
            lockSet.put(ttid, locks);
        } else {
            if (locks.contains(lockId)) return; // TODO(TraianSF): handle recursive locking
            locks.forEach(lock -> addEdge(lock, lockId));
        }
        locks.add(lockId);
    }

    /**
     * Computes cycles in the lock acquisition graph as lists of edges between
     * existing lock acquisitions and new lock acquisitions.
     * @return the cycles in the lock aqusition graph
     */
    public void runDeadlockDetection() {
        getCycles().forEach(this::reportDeadlock);
    }

    private void addEdge(Long l1, Long l2) {
        sccGraph.addEdge(l1,l2);
        eventEdges.put(Pair.of(l1,l2),Pair.of(lockEvents.get(l1), lockEvents.get(l2)));
    }

    private List<List<Pair<ReadonlyEventInterface, ReadonlyEventInterface>>> getCycles() {
        Collection<List<Long>> cycles = sccGraph.getScc();
        List<List<Pair<ReadonlyEventInterface, ReadonlyEventInterface>>> eventCycles = new ArrayList<>();
        for (List<Long> cycle : cycles) {
            // reverse the order of vertices because getScc returns them as they were stored on an internal stack
            java.util.Collections.reverse((List<?>) cycle);
            if (cycle.size()==1) continue;
            List<Pair<ReadonlyEventInterface, ReadonlyEventInterface>> eventCycle = new ArrayList<>();
            for (int i = 0; i < cycle.size()-1; i++) {
                long v1 = cycle.get(i);
                long v2 = cycle.get(i+1);
                Pair eventPair = eventEdges.get(Pair.of(v1, v2));
                // make sure that edges on the cycle correspond the graph edges
                assert(eventPair.getLeft() != null && eventPair.getRight() != null);
                eventCycle.add(eventEdges.get(Pair.of(v1, v2)));
            }
            long v1 = cycle.get(cycle.size()-1);
            long v2 = cycle.get(0);
            eventCycle.add(eventEdges.get(Pair.of(v1, v2)));
            eventCycles.add(eventCycle);
        }
        return eventCycles;
    }


    private void reportDeadlock(List<Pair<ReadonlyEventInterface, ReadonlyEventInterface>> cycle) {
        signatureProcessor.reset();
        StringBuilder report = new StringBuilder();
        StringBuilder summary = new StringBuilder();
        StringBuilder details = new StringBuilder();
        cycle.forEach(eventEventPair -> {
            String locSig;
            ReadonlyEventInterface before = eventEventPair.getLeft();
            summary.append("M"+ before.getLockId() + " " +
                    "(" + before.getLockRepresentation() + ")" +
                    " => ");
            ReadonlyEventInterface after = eventEventPair.getRight();
            details.append('\n');
            details.append("    M" + after.getLockId() + " acquired " +
                    "while holding M" + before.getLockId());
            details.append('\n');
            details.append(" ---->  at ");
            locSig = metadata.getLocationSig(after.getLocationId());
            signatureProcessor.process(locSig);
            details.append(locSig);
            details.append('\n');
            locSig = metadata.getLocationSig(before.getLocationId());
            signatureProcessor.process(locSig);
            details.append(String.format("        - locked %s at %s %n",
                    "M"+ before.getLockId(),
                    locSig));
        });
        summary.append("M"+cycle.get(0).getLeft().getLockId());
        report.append("Potential deadlock detected: {{{\n");
        report.append("    Cycle in lock acquisition graph: ");
        report.append(summary);
        report.append("\n");
        report.append(details);
        report.append("}}}\n");
        config.logger().report(signatureProcessor.simplify(report.toString()), Logger.MSGTYPE.REAL);
    }

    private void reportUnlockOfUnlockedMutex(ReadonlyEventInterface event) {
        assert(event.isUnlock());
        StringBuilder report = new StringBuilder();
        report.append("Unlock of an unlocked mutex: \n");
        report.append("\t\t" + metadata.getLocationSig(event.getLocationId()));
        report.append("\n");
        config.logger().report(report.toString(), Logger.MSGTYPE.REAL);
    }

}
