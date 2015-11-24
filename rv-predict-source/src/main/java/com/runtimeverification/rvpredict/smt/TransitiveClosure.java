package com.runtimeverification.rvpredict.smt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.runtimeverification.rvpredict.log.Event;

public class TransitiveClosure {

    /**
     * Map from event to its group ID in the contracted graph.
     */
    private final Map<Event, Integer> eventToGroupId;

    /**
     * Relation matrix indexed by group ID.
     */
    private final boolean[][] relation;

    private TransitiveClosure(Map<Event, Integer> eventToGroupId, boolean[][] inRelation) {
        this.eventToGroupId = eventToGroupId;
        this.relation = inRelation;
    }

    public boolean inRelation(Event e1, Event e2) {
        return relation[eventToGroupId.get(e1)][eventToGroupId.get(e2)];
    }

    public static Builder builder(int size) {
        return new Builder(size);
    }

    public static class Builder {

        private final Map<Event, Integer> eventToGroupId;

        private final List<Pair<Event, Event>> relations = new ArrayList<>();

        private Builder(int size) {
            eventToGroupId = new HashMap<>(size);
        }

        public void createNewGroup(Event e) {
            eventToGroupId.put(e, eventToGroupId.size());
        }

        /**
         * Add event {@code y} to the group of event {@code x}.
         */
        public void addToGroup(Event y, Event x) {
            eventToGroupId.put(y, eventToGroupId.get(x));
        }

        public void addRelation(Event x, Event y) {
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
