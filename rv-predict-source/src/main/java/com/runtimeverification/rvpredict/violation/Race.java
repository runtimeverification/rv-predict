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

import com.google.common.base.StandardSystemProperty;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.LLVMSignatureProcessor;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.metadata.SignatureProcessor;
import com.runtimeverification.rvpredict.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private final ReadonlyEventInterface e1;
    private final ReadonlyEventInterface e2;
    private final Configuration config;
    private final Trace trace;
    private final SignatureProcessor signatureProcessor;

    public Race(ReadonlyEventInterface e1, ReadonlyEventInterface e2, Trace trace, Configuration config) {
        if (e1.getEventId() > e2.getEventId()) {
            ReadonlyEventInterface tmp = e1;
            e1 = e2;
            e2 = tmp;
        }

        this.e1 = e1.copy();
        this.e2 = e2.copy();
        this.trace = trace;
        this.config = config;
        if (config.isLLVMPrediction()) {
            signatureProcessor = new LLVMSignatureProcessor();
        } else {
            signatureProcessor = new SignatureProcessor();
        }
    }

    public ReadonlyEventInterface firstEvent() {
        return e1;
    }

    public ReadonlyEventInterface secondEvent() {
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
        int addr = Math.min(0, e1.getDataAddress().getFieldIdOrArrayIndex()); // collapse all array indices to 0
        int loc1 = Math.min(e1.getLocationId(), e2.getLocationId());
        int loc2 = Math.max(e1.getLocationId(), e2.getLocationId());
        return "Race(" + addr + "," + loc1 + "," + loc2 + ")";
    }

    public String getRaceLocationSig() {
        if(config.isLLVMPrediction()) {
            int idx = e1.getDataAddress().getObjectHashCode();
            if(idx != 0) {
                String sig = trace.metadata().getVariableSig(idx).replace("/", ".");
                return "@" + sig;
            } else {
                return "#" + e1.getDataAddress().getFieldIdOrArrayIndex();
            }
        } else {
            int idx = e1.getDataAddress().getFieldIdOrArrayIndex();
            if (idx < 0) {
                String sig = trace.metadata().getVariableSig(-idx).replace("/", ".");
                int object = e1.getDataAddress().getObjectHashCode();
                return object == 0 ? "@" + sig : sig;
            }
            return "#" + idx;
        }
    }

    public String generateRaceReport() {
        signatureProcessor.reset();
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
        sb.append(String.format("Data race on %s: %n", locSig));
        boolean reportableRace = false;

        if (trace.metadata().getLocationSig(e1.getLocationId())
                .compareTo(trace.metadata().getLocationSig(e2.getLocationId())) <= 0) {
            reportableRace |= generateMemAccReport(e1, sb);
            sb.append(StandardSystemProperty.LINE_SEPARATOR.value());
            reportableRace |= generateMemAccReport(e2, sb);
        } else {
            reportableRace |= generateMemAccReport(e2, sb);
            sb.append(StandardSystemProperty.LINE_SEPARATOR.value());
            reportableRace |= generateMemAccReport(e1, sb);
        }

        sb.append(String.format("%n"));
        return reportableRace ? signatureProcessor.simplify(sb.toString()) : "";
    }

    private boolean generateMemAccReport(ReadonlyEventInterface e, StringBuilder sb) {
        int stackSize = 0;
        long otid = e.getOriginalThreadId();
        long sid = e.getSignalNumber();
        Metadata metadata = trace.metadata();
        List<ReadonlyEventInterface> heldLocks = trace.getHeldLocksAt(e);
        if (e.getSignalDepth() == 0) {
            sb.append(String.format("    Concurrent %s in thread T%s signal S%s%s)%n",
                    e.isWrite() ? "write" : "read",
                    otid,
                    sid,
                    getHeldLocksReport(heldLocks)));
        } else {
            // TODO(virgil): The signal number is not enough to identify what is happening, one also needs
            // the signal handler or something similar.
            sb.append(String.format("    Concurrent %s in thread T%s signal S%s%s%n",
                    e.isWrite() ? "write" : "read",
                    otid,
                    sid,
                    getHeldLocksReport(heldLocks)));
        }
        boolean isTopmostStack = true;
        List<ReadonlyEventInterface> stacktrace = new ArrayList<>(trace.getStacktraceAt(e));
        stacktrace.addAll(heldLocks);
        Collections.sort(stacktrace, (e1, e2) -> -e1.compareTo(e2));
        for (ReadonlyEventInterface elem : stacktrace) {
            int locId = elem.getLocationId();
            String locSig = locId >= 0 ? metadata.getLocationSig(locId)
                    : "... not available ...";
            if (config.isExcludedLibrary(locSig)) {
                assert !elem.isLock() : "Locations for locks should have been handled in TraceState::updateLockLocToUserLoc";
                continue;
            }
            stackSize++;
            if (locId >= 0) {
                signatureProcessor.process(locSig);
            }
            if (elem.isLock()) {
                sb.append(String.format("        - locked %s at %s %n", elem.getLockRepresentation(),
                        locSig));
            } else {
                sb.append(String.format("      %s at %s%n", isTopmostStack ? ">" : " ", locSig));
                isTopmostStack = false;
            }
        }

        long parentOTID = metadata.getParentOTID(otid);
        if (parentOTID > 0) {
            int locId = metadata.getOriginalThreadCreationLocId(otid);
            sb.append(String.format("    T%s is created by T%s%n", otid, parentOTID));
            if (locId >= 0) {
                String locationSig = metadata.getLocationSig(locId);
                signatureProcessor.process(locationSig);
                sb.append(String.format("        at %s%n", locationSig));
            } else {
                sb.append("        at unknown location%n");
            }
        } else {
            if (otid == 1) {
                sb.append(String.format("    T%s is the main thread%n", otid));
            } else {
                sb.append(String.format("    T%s is created by n/a%n", otid));
            }
        }
        return stackSize>0;
    }

    private String getHeldLocksReport(List<ReadonlyEventInterface> heldLocks) {
	if (heldLocks.isEmpty())
		return "";
        StringBuilder sb = new StringBuilder();
        if (!heldLocks.isEmpty()) {
            for (int i = 0; i < heldLocks.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(heldLocks.get(i).getLockRepresentation());
            }
            sb.append(heldLocks.get(i).getLockRepresentation());
        }
        return sb.toString();
    }

}
