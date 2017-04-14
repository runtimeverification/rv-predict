package com.runtimeverification.rvpredict.smt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.runtimeverification.rvpredict.log.ReadonlyEvent;
import org.apache.commons.lang3.tuple.Pair;


public class TransitiveClosure {

    /**
     * Map from event to its group ID in the contracted graph.
     */
    private final Map<ReadonlyEvent, Integer> eventToGroupId;

    /**
     * Relation matrix indexed by group ID.
     */
    private final boolean[][] relation;

    private TransitiveClosure(Map<ReadonlyEvent, Integer> eventToGroupId, boolean[][] inRelation) {
        this.eventToGroupId = eventToGroupId;
        this.relation = inRelation;
    }

    public boolean inRelation(ReadonlyEvent e1, ReadonlyEvent e2) {
        return relation[eventToGroupId.get(e1)][eventToGroupId.get(e2)];
    }

    public static Builder builder(int size) {
        return new Builder(size);
    }

    public static class Builder {

        private final Map<ReadonlyEvent, Integer> eventToGroupId;

        private final List<Pair<ReadonlyEvent, ReadonlyEvent>> relations = new ArrayList<>();

        private Builder(int size) {
            eventToGroupId = new HashMap<>(size);
        }

        public void createNewGroup(ReadonlyEvent e) {
            eventToGroupId.put(e, eventToGroupId.size());
        }

        /**
         * Add event {@code y} to the group of event {@code x}.
         */
        public void addToGroup(ReadonlyEvent y, ReadonlyEvent x) {
            eventToGroupId.put(y, eventToGroupId.get(x));
        }

        public void addRelation(ReadonlyEvent x, ReadonlyEvent y) {
            relations.add(Pair.of(x, y));
        }

        public TransitiveClosure build() {
            int numOfGroups = eventToGroupId.size();
            boolean[][] f = new boolean[numOfGroups][numOfGroups];
            relations.forEach(p -> f[eventToGroupId.get(p.getLeft())]
                    [eventToGroupId.get(p.getRight())] = true);
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
