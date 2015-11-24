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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.google.common.base.StandardSystemProperty;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.util.Constants;

/**
 * Represents a data race. A data race is uniquely identified by the two memory
 * access events that it consists of. However, different races can have
 * identical race signature, which is given by {@link Race#toString()}. For the
 * purpose of race detection, we are more interested in races that have
 * different signatures.
 *
 * @author YilongL
 */
public class Race {

    private static final String RVPREDICT_RT_PKG_PREFIX = Constants.RVPREDICT_RUNTIME_PKG_PREFIX
            .replace('/', '.');

    private final Event e1;
    private final Event e2;
    private final Trace trace;

    public Race(Event e1, Event e2, Trace trace) {
        if (e1.getGID() > e2.getGID()) {
            Event tmp = e1;
            e1 = e2;
            e2 = tmp;
        }

        this.e1 = e1.copy();
        this.e2 = e2.copy();
        this.trace = trace;
    }

    public Event firstEvent() {
        return e1;
    }

    public Event secondEvent() {
        return e2;
    }

    @Override
    public int hashCode() {
        return e1.hashCode() * 31 + e2.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Race)) {
            return false;
        }

        Race otherRace = (Race) object;
        return e1.equals(otherRace.e1) && e2.equals(otherRace.e2);
    }

    @Override
    public String toString() {
        int addr = Math.min(0, e1.getFieldIdOrArrayIndex()); // collapse all array indices to 0
        int loc1 = Math.min(e1.getLocId(), e2.getLocId());
        int loc2 = Math.max(e1.getLocId(), e2.getLocId());
        return "Race(" + addr + "," + loc1 + "," + loc2 + ")";
    }

    public String getRaceLocationSig() {
        int idx = e1.getFieldIdOrArrayIndex();
        if (idx < 0) {
            String sig = trace.metadata().getVariableSig(-idx).replace("/", ".");
            int object = e1.getObjectHashCode();
            return object == 0 ? "@" + sig : sig;
        }
        return "#" + idx;
    }

    public String generateRaceReport() {
        String locSig = getRaceLocationSig();
        switch (locSig.charAt(0)) {
            case '#':
                locSig = "array element " + locSig;
                break;
            case '@':
                locSig = locSig.substring(1);
                break;
            default:
                locSig = "field " + locSig;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Data race on %s: {{{%n", locSig));

        if (trace.metadata().getLocationSig(e1.getLocId())
                .compareTo(trace.metadata().getLocationSig(e2.getLocId())) <= 0) {
            generateMemAccReport(e1, sb);
            sb.append(StandardSystemProperty.LINE_SEPARATOR.value());
            generateMemAccReport(e2, sb);
        } else {
            generateMemAccReport(e2, sb);
            sb.append(StandardSystemProperty.LINE_SEPARATOR.value());
            generateMemAccReport(e1, sb);
        }

        sb.append(String.format("}}}%n"));
        return sb.toString().replace(RVPREDICT_RT_PKG_PREFIX, "");
    }

    private void generateMemAccReport(Event e, StringBuilder sb) {
        long tid = e.getTID();
        Metadata metadata = trace.metadata();
        List<Event> heldLocks = trace.getHeldLocksAt(e);
        sb.append(String.format("    Concurrent %s in thread T%s (locks held: {%s})%n",
                e.isWrite() ? "write" : "read",
                tid,
                getHeldLocksReport(heldLocks)));
        boolean isTopmostStack = true;
        List<Event> stacktrace = new ArrayList<>(trace.getStacktraceAt(e));
        stacktrace.addAll(heldLocks);
        Collections.sort(stacktrace, (e1, e2) -> -e1.compareTo(e2));
        for (Event elem : stacktrace) {
            String locSig = elem.getLocId() != -1 ? metadata.getLocationSig(elem.getLocId())
                    : "... not available ...";
            if (elem.isLock()) {
                sb.append(String.format("        - locked %s at %s %n", getLockRepresentation(elem),
                        locSig));
            } else {
                sb.append(String.format(" %s  at %s%n", isTopmostStack ? "---->" : "     ", locSig));
                isTopmostStack = false;
            }
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
        long lockId = lock.getLockId();
        int upper32 = (int)(lockId >> 32);
        String lower32 = Integer.toHexString((int) lockId);
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
