package com.runtimeverification.rvpredict.smt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class TransitiveClosure {

    /**
     * Map from ID to its group ID in the contracted graph.
     */
    private final int[] idToGroupId;

    /**
     * Relation matrix indexed by group ID.
     */
    private final boolean[][] relation;

    private TransitiveClosure(int[] idToGroupId, boolean[][] inRelation) {
        this.idToGroupId = idToGroupId;
        this.relation = inRelation;
    }

    public boolean inRelation(int x, int y) {
        return relation[idToGroupId[x]][idToGroupId[y]];
    }

    public static Builder builder(int size) {
        return new Builder(size);
    }

    public static class Builder {

        private int numOfGroups = 0;

        private final int[] idToGroupId;

        private final List<Pair<Integer, Integer>> relations = new ArrayList<>();

        private Builder(int size) {
            idToGroupId = new int[size];
        }

        public void createNewGroup(int id) {
            idToGroupId[id] = numOfGroups++;
        }

        /**
         * Add node {@code y} to the group of node {@code x}.
         */
        public void addToGroup(int y, int x) {
            idToGroupId[y] = idToGroupId[x];
        }

        public void addRelation(int x, int y) {
            relations.add(Pair.of(x, y));
        }

        public TransitiveClosure build() {
            boolean[][] f = new boolean[numOfGroups][numOfGroups];
            relations.forEach(p -> f[idToGroupId[p.getLeft()]][idToGroupId[p.getRight()]] = true);
            for (int k = 0; k < numOfGroups; k++) {
                for (int x = 0; x < numOfGroups; x++) {
                    for (int y = 0; y < numOfGroups; y++) {
                        f[x][y] = f[x][y] || f[x][k] && f[k][y];
                    }
                }
            }

            return new TransitiveClosure(idToGroupId, f);
        }
    }
}
