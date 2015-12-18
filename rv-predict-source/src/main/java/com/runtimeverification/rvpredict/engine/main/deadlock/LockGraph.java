package com.runtimeverification.rvpredict.engine.main.deadlock;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.runtimeverification.rvpredict.log.Event;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Class for representing the lock acquisition graph abstraction
 * Useful in detectiong lock inversion patterns which could potential lead to
 * deadlocks
 *
 * @author TraianSF
 */
public class LockGraph {
    private int nextLockId = 0;
    private BiMap<Long, Integer> lockIdToVertex = HashBiMap.create();
    private List<Collection<Integer>> graph = new ArrayList<>();
    private Map<Long,Event> lockEvents = new HashMap<>();
    private Map<Pair<Integer,Integer>, Pair<Event,Event>> eventEdges = new HashMap<>();
    private Map<Long, Set<Long>> lockSet = new HashMap<>();


    private void addEdge(Long l1, Long l2) {
        int v1 = getLockId(l1);
        int v2 = getLockId(l2);
        graph.get(v1).add(v2);
        eventEdges.put(Pair.of(v1,v2),Pair.of(lockEvents.get(l1), lockEvents.get(l2)));
    }

    private int getLockId(Long l1) {
        Integer v1 = lockIdToVertex.get(l1);
        if (v1 == null) {
            v1 = nextLockId++;
            lockIdToVertex.put(l1,v1);
            graph.add(new HashSet<>());
        }
        return v1;
    }

    /**
     * Computes cycles in the lock acquisition graph as lists of edges between
     * existing lock acquisitions and new lock acquisitions.
     * @return the cycles in the lock aqusition graph
     */
    public List<List<Pair<Event,Event>>> getCycles() {
        List<List<Integer>> cycles = new SCCTarjan().scc(graph);
        List<List<Pair<Event,Event>>> eventCycles = new ArrayList<>();
        for (List<Integer> cycle : cycles) {
            if (cycle.size()==1) continue;
            List<Pair<Event,Event>> eventCycle = new ArrayList<>();
            for (int i = 0; i < cycle.size()-1; i++) {
                int v1 = cycle.get(i);
                int v2 = cycle.get(i+1);
                eventCycle.add(eventEdges.get(Pair.of(v1, v2)));
            }
            int v1 = cycle.get(cycle.size()-1);
            int v2 = cycle.get(0);
            eventCycle.add(eventEdges.get(Pair.of(v1, v2)));
            eventCycles.add(eventCycle);
        }
        return eventCycles;
    }

    /**
     * Maintains the lock acquisition set for each thread and at each new
     * aquisition adds edges to the graph from the existing aquired locks to
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
}
