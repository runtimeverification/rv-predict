package com.runtimeverification.rvpredict.smt;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransitiveClosure {

    /**
     * Map from event to its group ID in the contracted graph.
     */
    private final Map<ReadonlyEventInterface, Integer> eventToGroupId;

    /**
     * Relation matrix indexed by group ID.
     */
    private final boolean[][] relation;

    private TransitiveClosure(Map<ReadonlyEventInterface, Integer> eventToGroupId, boolean[][] inRelation) {
        this.eventToGroupId = eventToGroupId;
        this.relation = inRelation;
    }

    boolean inRelation(ReadonlyEventInterface e1, ReadonlyEventInterface e2) {
        return relation[eventToGroupId.get(e1)][eventToGroupId.get(e2)];
    }

    public static Builder builder(int size) {
        return new Builder(size);
    }

    public static class Builder {

        private final Map<ReadonlyEventInterface, Integer> eventToGroupId;

        private final List<Pair<ReadonlyEventInterface, ReadonlyEventInterface>> relations = new ArrayList<>();
        private final List<Pair<ReadonlyEventInterface, ReadonlyEventInterface>> notRelations = new ArrayList<>();

        private Builder(int size) {
            eventToGroupId = new HashMap<>(size);
        }

        public void createNewGroup(ReadonlyEventInterface e) {
            eventToGroupId.put(e, eventToGroupId.size());
        }

        /**
         * Add event {@code y} to the group of event {@code x}.
         */
        public void addToGroup(ReadonlyEventInterface y, ReadonlyEventInterface x) {
            eventToGroupId.put(y, eventToGroupId.get(x));
        }

        public void addRelation(ReadonlyEventInterface x, ReadonlyEventInterface y) {
            relations.add(Pair.of(x, y));
        }

        void addNotRelation(ReadonlyEventInterface x, ReadonlyEventInterface y) {
            notRelations.add(Pair.of(x, y));
        }

        public TransitiveClosure build() {
            int numOfGroups = eventToGroupId.size();
            boolean[][] f = new boolean[numOfGroups][numOfGroups];
            relations.forEach(p -> f[eventToGroupId.get(p.getLeft())]
                    [eventToGroupId.get(p.getRight())] = true);
            notRelations.forEach(p -> {
                int leftId = eventToGroupId.get(p.getLeft());
                int rightId = eventToGroupId.get(p.getRight());
                // If leftId is not before rightId, i.e. rightId <= leftId, then we can infer two things: that
                // every predecessor of rightId is before leftId and that every succesor of leftId is after rightId.
                // However, a predecessor of rightId may still race with leftId, so it's relation should not be strict.
                for (int i = 0; i < f.length; i++) {
                    if (f[leftId][i]) {
                        f[rightId][i] = true;
                    }
                }
            });
            for (int k = 0; k < numOfGroups; k++) {
                for (int x = 0; x < numOfGroups; x++) {
                    for (int y = 0; y < numOfGroups; y++) {
                        f[x][y] = f[x][y] || f[x][k] && f[k][y];
                    }
                }
            }

            return new TransitiveClosure(eventToGroupId, f);
        }
    }
}
