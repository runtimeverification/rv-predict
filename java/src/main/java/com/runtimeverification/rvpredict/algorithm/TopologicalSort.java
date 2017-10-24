package com.runtimeverification.rvpredict.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

public class TopologicalSort {
    /**
     * Topological sort that puts parents first.
     *
     * nodeToParents must contain an entry for each node that should be sorted.
     * @throws TopologicalSortingException when there is a cycle in the graph.
     */
    public static <T> void sortFromParentLists(
            Map<T, List<T>> nodeToParents, List<T> sorted)
            throws TopologicalSortingException {
        Map<T, List<T>> nodeToChildren = new HashMap<>();
        nodeToParents.forEach((node, parents) ->
                parents.forEach(parent ->
                        nodeToChildren.computeIfAbsent(parent, k -> new ArrayList<>()).add(node)));
        Map<T, Integer> nodeToParentCount =
                nodeToParents.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().size()));
        Stack<T> unprocessedNodes = new Stack<>();
        nodeToParentCount.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .forEach(entry -> unprocessedNodes.add(entry.getKey()));
        sorted.clear();
        while (!unprocessedNodes.isEmpty()) {
            T node = unprocessedNodes.pop();
            sorted.add(node);
            nodeToChildren.getOrDefault(node, Collections.emptyList()).forEach(child -> {
                Integer parentCount = nodeToParentCount.get(child);
                if (parentCount == null) {
                    throw new IllegalStateException("Node without parent.");
                }
                if (parentCount == 0) {
                    throw new IllegalStateException("Node without unprocessed parents.");
                }
                parentCount--;
                nodeToParentCount.put(child, parentCount);
                if (parentCount == 0) {
                    unprocessedNodes.push(child);
                }
            });
        }
        if (sorted.size() > nodeToParents.size()) {
            throw new IllegalStateException("Sorted too many nodes.");
        }
        if (sorted.size() < nodeToParents.size()) {
            throw new TopologicalSortingException("Could not sort all nodes.");
        }
    }

    public static class TopologicalSortingException extends Exception {
        private TopologicalSortingException(String message) {
            super(message);
        }
    }
}
