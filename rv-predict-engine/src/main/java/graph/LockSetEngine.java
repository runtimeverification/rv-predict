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
package graph;

import java.util.ListIterator;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import rvpredict.trace.LockRegion;
import rvpredict.trace.MemoryAccessEvent;

/**
 * Engine for computing the Lockset algorithm
 *
 */
public class LockSetEngine {

    private Table<Long, Long, List<LockRegion>> lockTbl = HashBasedTable.create();

    public void add(LockRegion lockRegion) {
        long addr = lockRegion.getLockObj();
        long threadId = lockRegion.getThreadId();
        List<LockRegion> lockRegions = lockTbl.get(addr, threadId);
        if (lockRegions == null) {
            lockRegions = Lists.newArrayList();
            lockTbl.put(addr, threadId, lockRegions);
        }

        // TODO(YilongL): what does the following mean?
        // filter out reentrant locks for CP
        ListIterator<LockRegion> iter = lockRegions.listIterator(lockRegions.size());
        while (iter.hasPrevious()) {
            LockRegion prevLockRegion = iter.previous();
            if (lockRegion.getLock() == null
                    || (prevLockRegion.getLock() != null && lockRegion.getLock().getGID() < prevLockRegion.getLock()
                            .getGID())) {
                iter.remove();
            } else {
                break;
            }
        }

        lockRegions.add(lockRegion);
    }

    // TODO(YilongL): what does the following mean?
    // NOTE: it's possible two lockpairs overlap, because we skipped wait nodes
    public boolean hasCommonLock(MemoryAccessEvent e1, MemoryAccessEvent e2) {
        assert e1.getTID() != e2.getTID();

        for (Long addr : lockTbl.rowKeySet()) {
            List<LockRegion> lockRegion1 = lockTbl.get(addr, e1.getTID());
            List<LockRegion> lockRegion2 = lockTbl.get(addr, e2.getTID());
            if (lockRegion1 != null && lockRegion2 != null) {
                return matchAnyLockPair(lockRegion1, e1.getGID())
                        && matchAnyLockPair(lockRegion2, e2.getGID());
            }
        }

        return false;
    }

    private boolean matchAnyLockPair(List<LockRegion> lockpair, long gid) {
        int s, e, mid;

        s = 0;
        e = lockpair.size() - 1;
        while (s <= e) {
            mid = (s + e) / 2;

            LockRegion lp = lockpair.get(mid);

            if (lp.getLock() == null) {
                if (gid < lp.getUnlock().getGID())
                    return true;
                else {
                    s = mid + 1;
                }
            } else if (lp.getUnlock() == null) {
                if (gid > lp.getLock().getGID())
                    return true;
                else {
                    e = mid - 1;
                }
            } else {
                if (gid > lp.getUnlock().getGID())
                    s = mid + 1;
                else if (gid < lp.getLock().getGID())
                    e = mid - 1;
                else
                    return true;
            }
        }

        return false;
    }

}
