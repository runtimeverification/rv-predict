/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.runtimeverification.rvpredict.smt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.trace.LockRegion;

/**
 * Engine for computing the lockset algorithm.
 */
public class LockSetEngine {

    private Map<Long, Map<Long, List<LockRegion>>> lockIdToTidToLockRegions = new HashMap<>();

    public void add(LockRegion region) {
        List<LockRegion> regions = lockIdToTidToLockRegions
                .computeIfAbsent(region.getLockId(), p -> new HashMap<>())
                .computeIfAbsent(region.getTID(), p -> new ArrayList<>());
        LockRegion last = Iterables.getLast(regions, null);
        if (last != null && region.getLock().getGID() <= last.getUnlock().getGID()) {
            throw new IllegalArgumentException(
                    "Unexpected overlapping lock regions: " + last + " & " + region);
        }
        regions.add(region);
    }

    /**
     * Checks if two given {@code Event}'s hold a common lock.
     */
    public boolean hasCommonLock(Event e1, Event e2) {
        if (e1.getTID() == e2.getTID()) {
            throw new IllegalArgumentException();
        }

        for (long lockId : lockIdToTidToLockRegions.keySet()) {
            /* check if both events hold lockId */
            LockRegion r1 = getLockRegion(e1, lockId);
            LockRegion r2 = getLockRegion(e2, lockId);
            if (r1 != null && r2 != null
                    && (r1.isWriteLocked() || r2.isWriteLocked())) {
                return true;
            }
        }

        return false;
    }

    private LockRegion getLockRegion(Event e, long lockId) {
        for (LockRegion region : lockIdToTidToLockRegions.get(lockId).getOrDefault(e.getTID(),
                Collections.emptyList())) {
            if (region.include(e)) {
                return region;
            }
        }
        return null;
    }
}
