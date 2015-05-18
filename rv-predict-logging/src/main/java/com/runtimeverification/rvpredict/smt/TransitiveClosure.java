package com.runtimeverification.rvpredict.smt;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

public class TransitiveClosure {

    private int elementID = 0;

    private boolean finalized;

    private final List<Pair<Integer, Integer>> relations = Lists.newArrayList();

    private boolean[][] inRelation;

    public int getAndIncrementElementID() {
        if (finalized) {
            throw new IllegalStateException("The transitive closure has been finalized.");
        }
        return elementID++;
    }

    public void addRelation(int x, int y) {
        if (finalized) {
            throw new RuntimeException("The transitive closure has been finalized.");
        }
        relations.add(Pair.of(x, y));
    }

    public boolean inRelation(int x, int y) {
        if (!finalized) {
            throw new RuntimeException("The transitive closure has not been finalized.");
        }
        return inRelation[x][y];
    }

    public void finish() {
        finalized = true;
        inRelation = new boolean[elementID][elementID];
        for (Pair<Integer, Integer> relation : relations) {
            inRelation[relation.getLeft()][relation.getRight()] = true;
        }
        for (int k = 0; k < elementID; k++) {
            for (int x = 0; x < elementID; x++) {
                for (int y = 0; y < elementID; y++) {
                    inRelation[x][y] = inRelation[x][y] || inRelation[x][k] && inRelation[k][y];
                }
            }
        }
    }

}
