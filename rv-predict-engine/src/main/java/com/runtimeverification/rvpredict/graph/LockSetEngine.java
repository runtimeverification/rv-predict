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
package com.runtimeverification.rvpredict.graph;

import java.util.Collection;
import java.util.ListIterator;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import com.runtimeverification.rvpredict.trace.Event;
import com.runtimeverification.rvpredict.trace.LockRegion;
import com.runtimeverification.rvpredict.trace.MemoryAccessEvent;

/**
 * Engine for computing the Lockset algorithm
 *
 */
public class LockSetEngine {

    private Table<Long, Long, List<LockRegion>> lockTbl = HashBasedTable.create();

    public void addAll(Collection<LockRegion> lockRegions) {
        for (LockRegion lockRegion : lockRegions) {
            add(lockRegion);
        }
    }

    public void add(LockRegion lockRegion) {
        long lockObj = lockRegion.getLockObj();
        long threadId = lockRegion.getThreadId();
        List<LockRegion> lockRegions = lockTbl.get(lockObj, threadId);
        if (lockRegions == null) {
            lockRegions = Lists.newArrayList();
            lockTbl.put(lockObj, threadId, lockRegions);
        }

        ListIterator<LockRegion> iter = lockRegions.listIterator(lockRegions.size());
        if (iter.hasPrevious()) {
            LockRegion prevLockRegion = iter.previous();
            assert lockRegion.getLock().getGID() > prevLockRegion.getUnlock().getGID() :
                "unexpected overlapping lock region: " + prevLockRegion + " and " + lockRegion;
        }

        lockRegions.add(lockRegion);
    }

    /**
     * Checks if two given {@code MemoryAccessEvent}'s hold a common lock.
     */
    public boolean hasCommonLock(MemoryAccessEvent e1, MemoryAccessEvent e2) {
        assert e1.getTID() != e2.getTID();

        for (Long lockObj : lockTbl.rowKeySet()) {
            /* check if both events hold lockObj */
            LockRegion lockRegion1 = getLockRegion(e1, lockObj);
            LockRegion lockRegion2 = getLockRegion(e2, lockObj);
            if (lockRegion1 != null && lockRegion2 != null
                    && (lockRegion1.isWriteLocked() || lockRegion2.isWriteLocked())) {
                return true;
            }
        }

        return false;
    }

    private LockRegion getLockRegion(Event e, Long lockObj) {
        // TODO(YilongL): optimize this method when necessary
        List<LockRegion> lockRegions = lockTbl.get(lockObj, e.getTID());
        if (lockRegions != null) {
            for (LockRegion lockRegion : lockRegions) {
                if (lockRegion.getLock() == null || lockRegion.getLock().getGID() < e.getGID()) {
                    if (lockRegion.getUnlock() == null
                            || e.getGID() < lockRegion.getUnlock().getGID()) {
                        return lockRegion;
                    }
                } else {
                    return null;
                }
            }
        }
        return null;
    }
}
