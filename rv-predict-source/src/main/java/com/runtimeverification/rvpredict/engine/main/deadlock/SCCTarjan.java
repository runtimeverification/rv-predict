package com.runtimeverification.rvpredict.engine.main.deadlock;

import java.util.*;

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

public class SCCTarjan {
    private int time;
    private List<Collection<Integer>> graph;
    private int[] lowlink;
    private boolean[] used;
    private List<Integer> stack;
    private List<List<Integer>> components;

    public List<List<Integer>> scc(List<Collection<Integer>> graph) {
        int n = graph.size();
        this.graph = graph;
        lowlink = new int[n];
        used = new boolean[n];
        stack = new ArrayList<>();
        components = new ArrayList<>();

        for (int u = 0; u < n; u++)
            if (!used[u])
                dfs(u);

        return components;
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
}
