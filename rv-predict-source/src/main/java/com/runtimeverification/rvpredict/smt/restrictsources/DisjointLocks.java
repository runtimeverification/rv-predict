package com.runtimeverification.rvpredict.smt.restrictsources;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.RestrictSource;
import com.runtimeverification.rvpredict.smt.restricts.And;
import com.runtimeverification.rvpredict.smt.restricts.DisjointSections;
import com.runtimeverification.rvpredict.trace.LockRegion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DisjointLocks implements RestrictSource {
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
    public ModelRestrict createRestrict() {
        ImmutableList.Builder<ModelRestrict> restricts = new ImmutableList.Builder<>();
        lockRegionsCollection.forEach(lockRegions -> {
            /* assert lock regions mutual exclusion */
            lockRegions.forEach(lr1 -> lockRegions.forEach(lr2 -> {
                if (lr1.getTTID() < lr2.getTTID()
                        && (lr1.isWriteLocked() || lr2.isWriteLocked())
                        && threadsCanOverlap.check(lr1.getTTID(), lr2.getTTID())) {
                    restricts.add(new DisjointSections(
                            Optional.ofNullable(lr1.getLock()),
                            Optional.ofNullable(lr1.getUnlock()),
                            Optional.ofNullable(lr2.getLock()),
                            Optional.ofNullable(lr2.getUnlock())));
                }
            }));
        });
        return new And(restricts.build());
    }
}
