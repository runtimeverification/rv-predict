// Copyright (c) 2014 K Team. All Rights Reserved.
// This is a modified version of
// https://github.com/indy256/codelibrary/blob/master/java/src/SCCTarjan.java
// which is licensed under the Unlicense http://unlicense.org/UNLICENSE

package org.kframework.utils.algorithms;

import java.util.*;

// optimized version of http://en.wikipedia.org/wiki/Tarjan's_strongly_connected_components_algorithm
public class SCCTarjan {
    int time;
    List<Collection<Integer>> graph;
    int[] lowlink;
    boolean[] used;
    List<Integer> stack;
    List<List<Integer>> components;

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

    void dfs(int u) {
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

    // Usage example
    public static void main(String[] args) {
        List<Collection<Integer>> g = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            g.add(new ArrayList<>());
        }
        g.get(0).add(1);
        g.get(0).add(2);
        g.get(2).add(1);
        g.get(1).add(2);
        g.get(3).add(2);
        g.get(4).add(3);
        g.get(5).add(4);
        g.get(6).add(5);
        g.get(7).add(6);
        g.get(8).add(9);
        g.get(9).add(7);


        List<List<Integer>> components = new SCCTarjan().scc(g);
        System.out.println(components);
    }
}
