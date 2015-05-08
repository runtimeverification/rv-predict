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
package com.runtimeverification.rvpredict.violation;

import java.util.List;

import com.google.common.base.StandardSystemProperty;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.util.Constants;

/**
 * Data race violation
 *
 */
public class Race extends AbstractViolation {

    private final Event e1;
    private final Event e2;
    private final Trace trace;

    private final int locId1;
    private final int locId2;
    private final String varSig;
    private final String stmtSig1;
    private final String stmtSig2;

    public Race(Event e1, Event e2, Trace trace,
            Metadata metadata) {
        if (e1.getLocId() > e2.getLocId()) {
            Event tmp = e1;
            e1 = e2;
            e2 = tmp;
        }

        this.e1 = e1.copy();
        this.e2 = e2.copy();
        this.trace = trace;
        locId1 = e1.getLocId();
        locId2 = e2.getLocId();
        int idx = e1.getAddr().fieldIdOrArrayIndex();
        varSig = idx < 0 ? metadata.getVariableSig(-idx).replace("/", ".") : "#" + idx;
        stmtSig1 = metadata.getLocationSig(locId1);
        stmtSig2 = metadata.getLocationSig(locId2);
        if (stmtSig1 == null) {
            System.err.println("[Warning]: missing metadata for location ID " + locId1);
        }
        if (stmtSig2 == null) {
            System.err.println("[Warning]: missing metadata for location ID " + locId2);
        }
    }

    @Override
    public int hashCode() {
        return locId1 * 17 + locId2;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Race)) {
            return false;
        }

        Race otherRace = (Race) object;
        return locId1 == otherRace.locId1 && locId2 == otherRace.locId2;
    }

    @Override
    public String toString() {
        String stmtSig1 = this.stmtSig1;
        String stmtSig2 = this.stmtSig2;
        if (stmtSig1.compareTo(stmtSig2) > 0) {
            String tmp = stmtSig1;
            stmtSig1 = stmtSig2;
            stmtSig2 = tmp;
        }

        return String.format("Race on %s between%s",
            varSig.startsWith("#") ? "an array access" : "field " + varSig,
            stmtSig1.equals(stmtSig2) ?
                String.format(" two instances of:%n    %s%n", stmtSig1) :
                String.format(":%n    %s%n    %s%n", stmtSig1, stmtSig2));
    }

    public String generateRaceReport() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Possible data race on %s: {{{%n",
                (varSig.startsWith("#") ? "array element " : "field ") + varSig));

        generateMemAccReport(e1, sb);
        sb.append(StandardSystemProperty.LINE_SEPARATOR.value());
        generateMemAccReport(e2, sb);

        sb.append(String.format("}}}%n"));
        return sb.toString();
    }

    private void generateMemAccReport(Event e, StringBuilder sb) {
        long tid = e.getTID();
        Metadata metadata = trace.metadata();
        List<Event> heldLocks = trace.getHeldLocksAt(e);
        sb.append(String.format("    Concurrent %s in thread T%s (locks held: {%s})%n",
                e.isWrite() ? "write" : "read",
                tid,
                getHeldLocksReport(heldLocks)));
        for (Integer locId : trace.getStacktraceAt(e)) {
            String sig = locId >= 0 ? metadata.getLocationSig(locId) : "... not available ...";
            sb.append(String.format("        at %s%n", sig));
        }

        long parentTID = metadata.getParentTID(tid);
        if (parentTID > 0) {
            int locId = metadata.getThreadCreationLocId(tid);
            sb.append(String.format("    T%s is created by T%s%n", tid, parentTID));
            sb.append(String.format("        at %s%n", locId >= 0 ? metadata.getLocationSig(locId)
                    : "unknown location"));
        } else {
            if (tid == 1) {
                sb.append(String.format("    T%s is the main thread%n", tid));
            } else {
                sb.append(String.format("    T%s is created by n/a%n", tid));
            }
        }

        if (!heldLocks.isEmpty()) {
            sb.append(String.format("    Locks acquired by this thread (reporting in chronological order):%n"));
            for (Event lock : heldLocks) {
                sb.append(String.format("      %s%n", getLockRepresentation(lock)));
                for (Integer locId : trace.getStacktraceAt(lock)) {
                    String sig = locId >= 0 ? metadata.getLocationSig(locId)
                            : "... not available ...";
                    sb.append(String.format("        at %s%n", sig));
                }
            }
        }
    }

    private String getHeldLocksReport(List<Event> heldLocks) {
        StringBuilder sb = new StringBuilder();
        if (!heldLocks.isEmpty()) {
            for (int i = 0; i < heldLocks.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(getLockRepresentation(heldLocks.get(i)));
            }
        }
        return sb.toString();
    }

    private String getLockRepresentation(Event lock) {
        long lockId = lock.getSyncObject();
        int upper32 = (int)(lockId >> 32);
        int lower32 = (int) lockId;
        if (lock.getType() == EventType.READ_LOCK) {
            assert upper32 == 0;
            return "ReadLock@" + lower32;
        } else {
            if (upper32 == 0) {
                return "WriteLock@" + lower32;
            } else {
                assert upper32 == Constants.MONITOR_C;
                return "Monitor@" + lower32;
            }
        }
    }

}
