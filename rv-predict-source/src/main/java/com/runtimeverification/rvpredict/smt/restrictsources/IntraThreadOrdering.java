package com.runtimeverification.rvpredict.smt.restrictsources;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.RestrictSourceWithHappensBefore;
import com.runtimeverification.rvpredict.smt.TransitiveClosure;
import com.runtimeverification.rvpredict.smt.restricts.And;
import com.runtimeverification.rvpredict.smt.restricts.Before;

import java.util.List;
import java.util.Map;

public class IntraThreadOrdering implements RestrictSourceWithHappensBefore {
    private final Map<Integer, List<ReadonlyEventInterface>> eventsByThread;

    public IntraThreadOrdering(Map<Integer, List<ReadonlyEventInterface>> eventsByThread) {
        this.eventsByThread = eventsByThread;
    }

    @Override
    public ModelRestrict createRestrict() {
        ImmutableList.Builder<ModelRestrict> restricts = new ImmutableList.Builder<>();
        eventsByThread.values().forEach(events -> {
            for (int i = 1; i < events.size(); i++) {
                restricts.add(new Before(events.get(i - 1), events.get(i)));
            }
        });
        return new And(restricts.build());
    }

    @Override
    public void addToMhbClosure(TransitiveClosure.Builder mhbClosureBuilder) {
        eventsByThread.forEach((tid, events) -> {
            mhbClosureBuilder.createNewGroup(events.get(0));
            for (int i = 1; i < events.size(); i++) {
                ReadonlyEventInterface e1 = events.get(i - 1);
                ReadonlyEventInterface e2 = events.get(i);
                /* every group should start with a join event and end with a start event */
                if (e1.isStart() || e2.isJoin()) {
                    mhbClosureBuilder.createNewGroup(e2);
                    mhbClosureBuilder.addRelation(e1, e2);
                } else {
                    mhbClosureBuilder.addToGroup(e2, e1);
                }
            }
        });
    }
}
