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
import com.runtimeverification.rvpredict.metadata.CompactTraceSignatureProcessor;
import com.runtimeverification.rvpredict.metadata.LLVMSignatureProcessor;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.metadata.SignatureProcessor;
import com.runtimeverification.rvpredict.trace.ThreadType;
import com.runtimeverification.rvpredict.trace.Trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

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

    private List<SignalStackEvent> firstSignalStack;
    private List<SignalStackEvent> secondSignalStack;

    public Race(
            ReadonlyEventInterface e1, ReadonlyEventInterface e2, Trace trace, Configuration config) {
        if (e1.getEventId() > e2.getEventId()) {
            ReadonlyEventInterface tmp = e1;
            e1 = e2;
            e2 = tmp;
        }

        this.e1 = e1.copy();
        this.e2 = e2.copy();
        this.trace = trace;
        this.config = config;
        if (config.isCompactTrace()) {
            this.signatureProcessor = new CompactTraceSignatureProcessor();
        } else if (config.isLLVMPrediction()) {
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
        int addr = Math.min(0, e1.getFieldIdOrArrayIndex()); // collapse all array indices to 0
        long loc1 = Math.min(e1.getLocationId(), e2.getLocationId());
        long loc2 = Math.max(e1.getLocationId(), e2.getLocationId());
        return "Race(" + addr + "," + loc1 + "," + loc2 + ")";
    }

    public String getRaceLocationSig() {
        return trace.metadata().getRaceLocationSig(e1, e2, trace, config);
    }
    public String generateRaceReport() {
        signatureProcessor.reset();
        String locSig = trace.metadata().getRaceLocationSig(e1, e2, trace, config);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Data race on %s:%n", locSig));
        boolean reportableRace;

        if (trace.metadata().getLocationSig(e1.getLocationId())
                .compareTo(trace.metadata().getLocationSig(e2.getLocationId())) <= 0) {
            reportableRace = generateMemAccReport(e1, getFirstSignalStack(), sb);
            sb.append(StandardSystemProperty.LINE_SEPARATOR.value());
            reportableRace |= generateMemAccReport(e2, getSecondSignalStack(), sb);
        } else {
            reportableRace = generateMemAccReport(e2, getSecondSignalStack(), sb);
            sb.append(StandardSystemProperty.LINE_SEPARATOR.value());
            reportableRace |= generateMemAccReport(e1, getFirstSignalStack(), sb);
        }

        sb.append(String.format("%n"));
        return reportableRace ? signatureProcessor.simplify(sb.toString()) : "";
    }

    public List<SignalStackEvent> getFirstSignalStack() {
        return firstSignalStack;
    }

    public List<SignalStackEvent> getSecondSignalStack() {
        return secondSignalStack;
    }

    public void setFirstSignalStack(List<SignalStackEvent> firstSignalStack) {
        this.firstSignalStack = firstSignalStack;
    }

    public void setSecondSignalStack(List<SignalStackEvent> secondSignalStack) {
        this.secondSignalStack = secondSignalStack;
    }

    public static class SignalStackEvent {
        private final ReadonlyEventInterface event;
        private final int ttid;

        public static SignalStackEvent fromEvent(ReadonlyEventInterface event, int ttid) {
            return new SignalStackEvent(event, ttid);
        }

        public static SignalStackEvent fromBeforeFirstEvent(int ttid) {
            return new SignalStackEvent(null, ttid);
        }

        private SignalStackEvent(ReadonlyEventInterface event, int ttid) {
            this.event = event;
            this.ttid = ttid;
        }

        public Optional<ReadonlyEventInterface> getEvent() {
            return Optional.ofNullable(event);
        }

        public int getTtid() {
            return ttid;
        }
    }

    private boolean generateMemAccReport(
            ReadonlyEventInterface e, List<SignalStackEvent> signalStackEvents, StringBuilder sb) {
        long otid = e.getOriginalThreadId();
        long sid = trace.getSignalNumber(trace.getTraceThreadId(e));
        List<ReadonlyEventInterface> heldLocks = trace.getHeldLocksAt(e);
        if (e.getSignalDepth() == 0) {
            sb.append(String.format("    Concurrent %s in thread T%s%s%n",
                    e.isWrite() ? "write" : "read",
                    otid,
                    getHeldLocksReport(heldLocks)));
        } else {
            sb.append(String.format("    Concurrent %s in signal S%s%s%n",
                    e.isWrite() ? "write" : "read",
                    sid,
                    getHeldLocksReport(heldLocks)));
        }
        boolean atLeastOneKnownElementInTheTrace = generateStackTrace(e, heldLocks, sb);
        for (int stackIndex = 1; stackIndex < signalStackEvents.size(); stackIndex++) {
            SignalStackEvent stackEvent = signalStackEvents.get(stackIndex);
            sb.append("    Interrupting ");
            int ttid = stackEvent.getTtid();
            if (trace.getThreadType(ttid) == ThreadType.THREAD) {
                sb.append("thread T");
                sb.append(trace.getOriginalThreadIdForTraceThreadId(ttid));
            } else {
                sb.append("signal S");
                sb.append(trace.getSignalNumber(ttid));
            }
            sb.append("\n");
            Optional<ReadonlyEventInterface> maybeEvent = signalStackEvents.get(stackIndex).getEvent();
            if (!maybeEvent.isPresent()) {
                sb.append(" before any event.");
                continue;
            }
            heldLocks = trace.getHeldLocksAt(maybeEvent.get());
            sb.append(getHeldLocksReport(heldLocks));
            generateStackTrace(maybeEvent.get(), heldLocks, sb);
        }
        return atLeastOneKnownElementInTheTrace;
    }

    private boolean generateStackTrace(
            ReadonlyEventInterface e,
            List<ReadonlyEventInterface> heldLocks,
            StringBuilder sb) {
        MetadataInterface metadata = trace.metadata();
        long otid = e.getOriginalThreadId();
        int stackSize = 0;
        boolean isTopmostStack = true;
        List<ReadonlyEventInterface> stacktrace = new ArrayList<>(trace.getStacktraceAt(e));
        stacktrace.addAll(heldLocks);
        stacktrace.sort((e1, e2) -> -e1.compareTo(e2));
        for (int i = 0; i < stacktrace.size(); i++) {
            ReadonlyEventInterface elem = stacktrace.get(i);

            OptionalLong locId = findEventLocation(stacktrace, i, elem);
            if (!locId.isPresent()) {
                continue;
            }
            if (!displayOneStackLocation(sb, metadata, isTopmostStack, elem, locId.getAsLong())) {
                continue;
            }
            stackSize++;
            isTopmostStack = false;
        }

        if (trace.getThreadType(e) == ThreadType.THREAD) {
            long parentOTID = metadata.getParentOTID(otid);
            if (parentOTID > 0) {
                long locId = metadata.getOriginalThreadCreationLocId(otid);
                sb.append(String.format("    T%s is created by T%s%n", otid, parentOTID));
                if (locId >= 0) {
                    String locationSig = metadata.getLocationSig(locId);
                    signatureProcessor.process(locationSig);
                    sb.append(String.format("        %s%s%n", metadata.getLocationPrefix(), locationSig));
                } else {
                    sb.append(String.format("        %sunknown location%n", metadata.getLocationPrefix()));
                }
            } else {
                if (otid == 1) {
                    sb.append(String.format("    T%s is the main thread%n", otid));
                } else {
                    sb.append(String.format("    T%s is created by n/a%n", otid));
                }
            }
        }
        return stackSize>0;
    }

    private OptionalLong findEventLocation(
            List<ReadonlyEventInterface> stacktrace, int eventIndex, ReadonlyEventInterface event) {
        OptionalLong locId;
        if (!event.isInvokeMethod()) {
            return OptionalLong.of(event.getLocationId());
        }
        locId = event.getCallSiteAddress();
        if (!locId.isPresent()) {
            for (int j = eventIndex + 1; j < stacktrace.size(); j++) {
                ReadonlyEventInterface callingFunction = stacktrace.get(j);
                if (callingFunction.isInvokeMethod()) {
                    return OptionalLong.of(callingFunction.getLocationId());
                }
            }
        }
        return locId;
    }

    private boolean displayOneStackLocation(
            StringBuilder sb, MetadataInterface metadata,
            boolean isTopmostStack, ReadonlyEventInterface event, long locId) {
        String locSig = locId >= 0 ? metadata.getLocationSig(locId)
                : "... not available ...";
        if (config.isExcludedLibrary(locSig)) {
            assert !event.isLock() : "Locations for locks should have been handled in TraceState::updateLockLocToUserLoc";
            return false;
        }
        if (locId >= 0) {
            signatureProcessor.process(locSig);
        }
        if (event.isLock()) {
            sb.append(String.format(
                    "        - locked %s %s%s%n",
                    event.getLockRepresentation(),
                    metadata.getLocationPrefix(),
                    locSig));
        } else {
            sb.append(String.format(
                    "      %s %s%s%n",
                    isTopmostStack ? ">" : " ",
                    metadata.getLocationPrefix(),
                    locSig));
        }
        return true;
    }

    private String getHeldLocksReport(List<ReadonlyEventInterface> heldLocks) {
        if (heldLocks.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < heldLocks.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(heldLocks.get(i).getLockRepresentation());
        }
        return sb.toString();
    }
}
