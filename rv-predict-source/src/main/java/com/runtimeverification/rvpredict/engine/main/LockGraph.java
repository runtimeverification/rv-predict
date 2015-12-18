package com.runtimeverification.rvpredict.engine.main;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.runtimeverification.rvpredict.log.Event;
import org.apache.commons.lang3.tuple.Pair;
import org.kframework.utils.algorithms.SCCTarjan;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by traian on 17.12.2015.
 */
public class LockGraph {
    int nextLockId = 0;
    BiMap<Long, Integer> lockIdToVertex = HashBiMap.create();
    List<Collection<Integer>> graph = new ArrayList<>();

    public void addEdge(Long l1, Long l2) {
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

    public List<List<Pair<Event,Event>>> getCycles() {
        List<List<Integer>> cycles = new SCCTarjan().scc(graph);
        List<List<Pair<Event,Event>>> eventCycles = new ArrayList<>();
        for (List<Integer> cycle : cycles) {
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

    Map<Long,Event> lockEvents = new HashMap<>();
    Map<Pair<Integer,Integer>, Pair<Event,Event>> eventEdges = new HashMap<>();
    Map<Long, Set<Long>> lockSet = new HashMap<>();

    public void handle(Event event) {
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
