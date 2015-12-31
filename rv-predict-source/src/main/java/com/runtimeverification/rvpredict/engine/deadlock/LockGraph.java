package com.runtimeverification.rvpredict.engine.deadlock;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.util.Logger;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

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
    private final Map<Long,Event> lockEvents = new HashMap<>();
    private final Map<Pair<Long,Long>, Pair<Event,Event>> eventEdges = new HashMap<>();
    private final Map<Long, Set<Long>> lockSet = new HashMap<>();

    public LockGraph(Configuration config, Metadata metadata) {
        this.config = config;
        this.metadata = metadata;
    }

    /**
     * Maintains the lock acquisition set for each thread and at each new
     * acquisition adds edges to the graph from the existing acquired locks to
     * the newly acquired one.
     *
     * @param event  a lock/unlock event
     */
    public void handle(Event event) {
        assert event.isLock() || event.isUnlock();
        long lockId = event.getLockId();
        long tid = event.getTID();
        Set<Long> locks = lockSet.get(tid);
        if (event.isUnlock()) {
            locks.remove(lockId);
            return;
        }
        // else, if event.isLock()
        lockEvents.put(lockId, event);
        if (locks == null) {
            locks = new HashSet<>();
            lockSet.put(tid, locks);
        } else {
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

    private List<List<Pair<Event,Event>>> getCycles() {
        Collection<List<Long>> cycles = sccGraph.getScc();
        List<List<Pair<Event,Event>>> eventCycles = new ArrayList<>();
        for (List<Long> cycle : cycles) {
            // reverse the order of vertices because getScc returns them as they were stored on an internal stack
            java.util.Collections.reverse((List<?>) cycle);
            if (cycle.size()==1) continue;
            List<Pair<Event,Event>> eventCycle = new ArrayList<>();
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

    private void reportDeadlock(List<Pair<Event, Event>> cycle) {
        StringBuilder report = new StringBuilder();
        StringBuilder summary = new StringBuilder();
        StringBuilder details = new StringBuilder();
        cycle.forEach(eventEventPair -> {
            Event before = eventEventPair.getLeft();
            summary.append("M"+ before.getLockId() + " " +
                    "(" + before.getLockRepresentation() + ")" +
                    " => ");
            Event after = eventEventPair.getRight();
            details.append('\n');
            details.append("    M" + after.getLockId() + " acquired " +
                    "while holding M" + before.getLockId());
            details.append('\n');
            details.append(" ---->  at ");
            details.append(metadata.getLocationSig(after.getLocId()));
            details.append('\n');
            details.append(String.format("        - locked %s at %s %n",
                    "M"+ before.getLockId(),
                    metadata.getLocationSig(before.getLocId())));
        });
        summary.append("M"+cycle.get(0).getLeft().getLockId());
        report.append("Potential deadlock detected: {{{\n");
        report.append("    Cycle in lock acquisition graph: ");
        report.append(summary);
        report.append("\n");
        report.append(details);
        report.append("}}}\n");
        config.logger().report(report.toString(), Logger.MSGTYPE.REAL);
    }

}
