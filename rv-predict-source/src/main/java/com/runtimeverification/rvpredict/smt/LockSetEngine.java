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

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.trace.LockRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Engine for computing the lockset algorithm.
 */
public class LockSetEngine {

    private final Map<Long, Map<Long, List<LockRegion>>> lockIdToTtidToLockRegions = new HashMap<>();

    public void add(LockRegion region) {
        lockIdToTtidToLockRegions
                .computeIfAbsent(region.getLockId(), p -> new HashMap<>())
                .computeIfAbsent(region.getTTID(), p -> new ArrayList<>())
                .add(region);
    }

    /**
     * Checks if two given {@code ReadonlyEventInterface}'s hold a common lock.
     */
    public boolean hasCommonLock(
            ReadonlyEventInterface e1, ReadonlyEventInterface e2, int ttid1, int ttid2) {
        if (ttid1 == ttid2) {
            throw new IllegalArgumentException();
        }

        for (long lockId : lockIdToTtidToLockRegions.keySet()) {
            /* check if both events hold lockId */
            LockRegion r1 = getLockRegion(e1, lockId, ttid1);
            LockRegion r2 = getLockRegion(e2, lockId, ttid2);
            if (r1 != null && r2 != null
                    && (r1.isWriteLocked() || r2.isWriteLocked())) {
                return true;
            }
        }

        return false;
    }

    private LockRegion getLockRegion(ReadonlyEventInterface e, long lockId, int ttid) {
        /* given a lockId, an event can be protected by at most one write-locked
         * region and one read-locked region (due to reentrant read-write lock
         * downgrading); always prefer to return the write-locked region */
        LockRegion result = null;
        for (LockRegion region : lockIdToTtidToLockRegions.get(lockId).getOrDefault(ttid,
                Collections.emptyList())) {
            if (region.include(e, ttid)) {
                if (region.isWriteLocked()) {
                    return region;
                } else if (result == null) {
                    result = region;
                }
            }
        }
        return result;
    }
}
