package com.runtimeverification.rvpredict.smt.constraintsources;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.ConstraintSource;
import com.runtimeverification.rvpredict.smt.ConstraintType;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.constraints.And;
import com.runtimeverification.rvpredict.smt.constraints.SingleNonSignalHasDepth0;
import com.runtimeverification.rvpredict.trace.ThreadType;

import java.util.Collection;
import java.util.function.Function;

public class NonSignalsHaveDepth0ConstraintSource implements ConstraintSource {
    private final Collection<Integer> threadIds;
    private final Function<Integer, ThreadType> ttidToThreadType;

    public NonSignalsHaveDepth0ConstraintSource(Collection<Integer> threadIds, Function<Integer, ThreadType> ttidToThreadType) {
        this.threadIds = threadIds;
        this.ttidToThreadType = ttidToThreadType;
    }

    @Override
    public ModelConstraint createConstraint(ConstraintType constraintType) {
        ImmutableList.Builder<ModelConstraint> allConstraints = new ImmutableList.Builder<>();
        threadIds.stream()
                .filter(ttid -> ttidToThreadType.apply(ttid) == ThreadType.THREAD)
                .forEach(ttid -> allConstraints.add(new SingleNonSignalHasDepth0(ttid)));
        return new And(allConstraints.build());
    }
}
