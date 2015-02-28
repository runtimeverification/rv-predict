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

import java.util.*;

import com.runtimeverification.rvpredict.trace.Event;

/*
 * property: never call addEdge after canReach
 * TODO: must be optimized to handle big graph
 */
public class ReachabilityEngine {

    private long counter = 0;

    HashMap<Long, Long> idMap = new HashMap<Long, Long>();

    private int M = 100000;// five Os
    HashSet<Long> cachedNoReachSet = new HashSet<Long>();

    HashMap<Long, HashSet<Long>> edgeSetMap = new HashMap<Long, HashSet<Long>>();

    public void addEdge(Event e1, Event e2) {
        addInternalEdge(getId(e1.getGID()), getId(e2.getGID()));
    }

    private void addInternalEdge(long i1, long i2) {
        HashSet<Long> s = edgeSetMap.get(i1);
        if (s == null) {
            s = new HashSet<Long>();
            edgeSetMap.put(i1, s);
        }

        s.add(i2);
    }

    public boolean deleteEdge(long i1, long i2) {
        i1 = getId(i1);
        i2 = getId(i2);

        HashSet<Long> s = edgeSetMap.get(i1);
        if (s == null) {
            s = new HashSet<Long>();
            edgeSetMap.put(i1, s);
        }
        if (s.contains(i2)) {
            s.remove(i2);
            return true;
        }
        return false;
    }

    private long getId(long id) {
        Long ID = idMap.get(id);
        if (ID == null) {
            ID = counter++;
            idMap.put(id, ID);// oh, forgot to do this
        }
        return ID;
    }

    private boolean hasEdge(long i1, long i2) {
        HashSet<Long> s = edgeSetMap.get(i1);
        if (s == null) {
            s = new HashSet<Long>();
            edgeSetMap.put(i1, s);
        }
        return s.contains(i2);
    }

    public boolean canReach(Long i1, Long i2) {
        // must have corresponding real id

        // what if idMap does not contain id?

        i1 = idMap.get(i1);
        i2 = idMap.get(i2);
        if (i1 == null || i2 == null) {
            // TODO(YilongL): this is a hack to patch the already broken code
            return false;
        }

        // return reachmx[i1][i2];
        long SIG = i1 * M + i2;
        if (cachedNoReachSet.contains(SIG))
            return false;
        else if (hasEdge(i1, i2))
            return true;
        else {
            // DFS - without cache
            java.util.ArrayDeque<Long> stack = new java.util.ArrayDeque<Long>();
            HashSet<Long> visitedNodes = new HashSet<Long>();
            stack.push(i1);

            while (!stack.isEmpty()) {

                long i1_ = stack.pop();

                visitedNodes.add(i1_);

                if (!hasEdge(i1, i1_))
                    addInternalEdge(i1, i1_);

                if (i1_ == i2) {
                    return true;
                } else {
                    if (hasEdge(i1_, i2)) {
                        addInternalEdge(i1, i2);
                        return true;
                    } else {
                        Iterator<Long> sIter = edgeSetMap.get(i1_).iterator();
                        while (sIter.hasNext()) {
                            long i1__ = sIter.next();
                            // System.out.print("DEBUG: "+i1+" "+i1_+" "+
                            // i1__+"\n");
                            long sig = i1__ * M + i2;
                            if (!visitedNodes.contains(i1__) && !cachedNoReachSet.contains(sig))
                                stack.push(i1__);
                        }
                    }
                }
            }

            cachedNoReachSet.add(SIG);
            return false;
        }
    }
}