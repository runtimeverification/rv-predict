package com.runtimeverification.rvpredict.engine.main;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.runtimeverification.rvpredict.log.Event;
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

    public Stream<Stream<Event>> getCycles() {
        List<List<Integer>> cycles = new SCCTarjan().scc(graph);
        return cycles.stream().map(
                l -> l.stream().map(
                        v -> lockEvents.get(lockIdToVertex.inverse().get(v))));
    }

    Map<Long,Event> lockEvents = new HashMap<>();
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
