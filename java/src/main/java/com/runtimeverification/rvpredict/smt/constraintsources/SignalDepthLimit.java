package com.runtimeverification.rvpredict.smt.constraintsources;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.ConstraintSource;
import com.runtimeverification.rvpredict.smt.ConstraintType;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.constraints.And;
import com.runtimeverification.rvpredict.smt.constraints.MaxSignalInterruptionDepth;
import com.runtimeverification.rvpredict.smt.constraints.True;
import com.runtimeverification.rvpredict.trace.ThreadType;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class SignalDepthLimit implements ConstraintSource {
    private final int maxDepth;
    private final Collection<Integer> threadIds;
    private final Function<Integer, ThreadType> ttidToThreadType;
    private final Function<Integer, Integer> ttidToThreadDepth;

    public SignalDepthLimit(
            int maxDepth,
            Collection<Integer> threadIds,
            Function<Integer, ThreadType> ttidToThreadType,
            Function<Integer, Integer> ttidToThreadDepth) {
        this.maxDepth = maxDepth;
        this.threadIds = threadIds;
        this.ttidToThreadType = ttidToThreadType;
        this.ttidToThreadDepth = ttidToThreadDepth;
    }

    @Override
    public ModelConstraint createConstraint(ConstraintType constraintType) {
        if (maxDepth == 0) {
            return new True();
        }
        Optional<Integer> maybeWindowDepth = threadIds.stream()
                .filter(ttid -> ttidToThreadType.apply(ttid) == ThreadType.SIGNAL)
                .map(ttidToThreadDepth).max(Integer::compareTo);
        int windowDepth = maybeWindowDepth.orElse(maxDepth);
        int maxWindowDepth = windowDepth < maxDepth ? maxDepth : windowDepth;

        ImmutableList.Builder<ModelConstraint> allConstraints = new ImmutableList.Builder<>();
        threadIds.stream()
                .filter(ttid -> ttidToThreadType.apply(ttid) == ThreadType.SIGNAL)
                .forEach(ttid -> allConstraints.add(new MaxSignalInterruptionDepth(ttid, maxWindowDepth)));
        return new And(allConstraints.build());
    }
}
