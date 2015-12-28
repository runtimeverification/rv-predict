package com.runtimeverification.rvpredict.engine.deadlock;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * An optimized version of Tarjan's Strongly connected components algorithm
 * http://en.wikipedia.org/wiki/Tarjan's_strongly_connected_components_algorithm
 *
 * This is a modified version of
 * https://github.com/indy256/codelibrary/blob/master/java/src/SCCTarjan.java
 * which is licensed under the Unlicense http://unlicense.org/UNLICENSE
 *
 * The graph is represented as a list of collections of integers where
 * - vertexes are identified by indexes in the list
 * - the collection of integers at index i represents
 *   the identifiers of the outgoing vertices of vertex i
 * @see SCCTarjanTest for usage example
 *
 * @author TraianSF
 */

class SCCTarjan<Vertex> {
    private int nextLockId = 0;
    private int time;
    private int[] lowlink;
    private boolean[] used;
    private List<Integer> stack;
    private List<List<Integer>> components;

    private List<Collection<Integer>> graph = new ArrayList<>();

    public void addEdge(Vertex l1, Vertex l2) {
        int v1 = getLockId(l1);
        int v2 = getLockId(l2);
        graph.get(v1).add(v2);
    }

    public Collection<List<Vertex>> getScc() {
        if (components == null) {
            scc();
        }
        Collection<List<Vertex>> sccs = new ArrayList<>();
        components.forEach(component -> {
            List<Vertex> scc = new ArrayList<>();
            component.forEach(
                    v -> scc.add(lockIdToVertex.inverse().get(v)));
            sccs.add(scc);
        });
        return sccs;
    }

    private void scc() {
        int n = graph.size();
        lowlink = new int[n];
        used = new boolean[n];
        stack = new ArrayList<>();
        components = new ArrayList<>();

        for (int u = 0; u < n; u++)
            if (!used[u])
                dfs(u);
    }

    private void dfs(int u) {
        lowlink[u] = time++;
        used[u] = true;
        stack.add(u);
        boolean isComponentRoot = true;

        for (int v : graph.get(u)) {
            if (!used[v])
                dfs(v);
            if (lowlink[u] > lowlink[v]) {
                lowlink[u] = lowlink[v];
                isComponentRoot = false;
            }
        }

        if (isComponentRoot) {
            List<Integer> component = new ArrayList<>();
            while (true) {
                int k = stack.remove(stack.size() - 1);
                component.add(k);
                lowlink[k] = Integer.MAX_VALUE;
                if (k == u)
                    break;
            }
            components.add(component);
        }
    }

    private BiMap<Vertex, Integer> lockIdToVertex = HashBiMap.create();
    private int getLockId(Vertex l1) {
        Integer v1 = lockIdToVertex.get(l1);
        if (v1 == null) {
            graph.add(new HashSet<>());
            v1 = nextLockId++;
            lockIdToVertex.put(l1,v1);
        }
        return v1;
    }

}
