package com.runtimeverification.rvpredict.smt.constraintsources;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.ConstraintSource;
import com.runtimeverification.rvpredict.smt.constraints.And;
import com.runtimeverification.rvpredict.smt.constraints.DisjointSections;
import com.runtimeverification.rvpredict.trace.LockRegion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DisjointLocks implements ConstraintSource {
    private final Collection<List<LockRegion>> lockRegionsCollection;
    private final OverlappingThreadChecker threadsCanOverlap;

    @FunctionalInterface
    public interface OverlappingThreadChecker {
        boolean check(int ttid1, int ttid2);
    }

    public DisjointLocks(
            Collection<List<LockRegion>> lockRegionsCollection, OverlappingThreadChecker threadsCanOverlap) {
        this.lockRegionsCollection = lockRegionsCollection;
        this.threadsCanOverlap = threadsCanOverlap;
    }

    @Override
    public ModelConstraint createConstraint() {
        ImmutableList.Builder<ModelConstraint> constraints = new ImmutableList.Builder<>();
        lockRegionsCollection.forEach(lockRegions -> {
            /* assert lock regions mutual exclusion */
            lockRegions.forEach(lr1 -> lockRegions.forEach(lr2 -> {
                if (lr1.getTTID() < lr2.getTTID()
                        && (lr1.isWriteLocked() || lr2.isWriteLocked())
                        && threadsCanOverlap.check(lr1.getTTID(), lr2.getTTID())) {
                    constraints.add(new DisjointSections(
                            Optional.ofNullable(lr1.getLock()),
                            Optional.ofNullable(lr1.getUnlock()),
                            Optional.ofNullable(lr2.getLock()),
                            Optional.ofNullable(lr2.getUnlock())));
                }
            }));
        });
        return new And(constraints.build());
    }
}
