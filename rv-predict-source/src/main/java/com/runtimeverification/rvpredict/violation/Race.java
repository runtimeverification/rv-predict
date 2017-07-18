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
import com.runtimeverification.error.data.ErrorCategory;
import com.runtimeverification.error.data.Language;
import com.runtimeverification.error.data.RawComponentField;
import com.runtimeverification.error.data.RawField;
import com.runtimeverification.error.data.RawFrame;
import com.runtimeverification.error.data.RawLock;
import com.runtimeverification.error.data.RawStackError;
import com.runtimeverification.error.data.RawTrace;
import com.runtimeverification.error.data.RawTraceComponent;

import java.util.ArrayList;
import java.util.Collections;
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

    public String getRaceDataSig() {
        return trace.metadata().getRaceDataSig(e1, e2, trace, config);
    }
    public String generateRaceReport() {
        if (config.isJsonReport()) {
            signatureProcessor.reset();
            RawStackError error = new RawStackError();
            boolean reportableRace = generateErrorJson(error, trace.metadata());
            StringBuilder sb = new StringBuilder();
            error.toJsonBuffer(sb);
            return reportableRace ? signatureProcessor.simplify(sb.toString()) : "";
        } else {
            signatureProcessor.reset();
            String locSig = trace.metadata().getRaceDataSig(e1, e2, trace, config);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Data race on %s:%n", locSig));
            boolean reportableRace;
    
            if (trace.metadata().getLocationSig(e1.getLocationId())
                    .compareTo(trace.metadata().getLocationSig(e2.getLocationId())) <= 0) {
                reportableRace = generateMemAccReport(e1, getFirstSignalStack(), trace.metadata(), sb);
                sb.append(StandardSystemProperty.LINE_SEPARATOR.value());
                reportableRace |= generateMemAccReport(e2, getSecondSignalStack(), trace.metadata(), sb);
            } else {
                reportableRace = generateMemAccReport(e2, getSecondSignalStack(), trace.metadata(), sb);
                sb.append(StandardSystemProperty.LINE_SEPARATOR.value());
                reportableRace |= generateMemAccReport(e1, getFirstSignalStack(), trace.metadata(), sb);
            }
    
            sb.append(String.format("%n"));
            return reportableRace ? signatureProcessor.simplify(sb.toString()) : "";
        }
    }

    public boolean generateErrorJson(RawStackError error, MetadataInterface metadata) {
        error.description_format = "Data race on %s";
        RawField f = new RawField();
        f.address = metadata.getRaceDataSig(e1, e2, trace, config);
        error.description_fields = new ArrayList<>();
        error.description_fields.add(f);
        RawTrace t1 = new RawTrace();
        RawTrace t2 = new RawTrace();
        boolean reportableRace = generateTraceJson(t1, metadata, e1, getFirstSignalStack());
        reportableRace |= generateTraceJson(t2, metadata, e2, getSecondSignalStack());
        error.traces = new ArrayList<>();
        if (trace.metadata().getLocationSig(e1.getLocationId())
                .compareTo(trace.metadata().getLocationSig(e2.getLocationId())) <= 0) {
            error.traces.add(t1);
            error.traces.add(t2);
        } else {
            error.traces.add(t2);
            error.traces.add(t1);
        }
        error.category = getCategory();
        error.error_id = getErrorId();
        return reportableRace;
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

    private ErrorCategory getCategory() {
        ErrorCategory cat = new ErrorCategory();
        if (isSignalRace()) {
           cat.setLintError();
        } else {
           Language lang = new Language();
           // TODO: get the language
           lang.setC();
           cat.setUndefined(lang);
        }
        return cat;
    }

    private boolean isSignalRace() {
        return e1.getOriginalThreadId() == e2.getOriginalThreadId();
    }

    private String getErrorId() {
        if (isSignalRace()) {
            if (e1.isWrite() && e2.isWrite()) {
                return "RVP1";
            } else {
                return "RVP2";
            }
        } else {
            if (e1.isWrite() && e2.isWrite()) {
                return "CEER5";
            } else {
                return "CEER4";
            }
        }
    }

    private boolean generateTraceJson(
            RawTrace t, MetadataInterface metadata, ReadonlyEventInterface e, List<SignalStackEvent> signalStackEvents) {
        long otid = e.getOriginalThreadId();
        long sid = trace.getSignalNumber(trace.getTraceThreadId(e));
        List<ReadonlyEventInterface> heldLocks = trace.getHeldLocksAt(e);
        RawTraceComponent c = new RawTraceComponent();
        t.components = new ArrayList<>();
        t.components.add(c);
        String accessType = e.isWrite() ? "Write" : "Read";
        boolean isSignal = e.getSignalDepth() != 0;
        String locksHeldDescription = getHeldLocksDescription(heldLocks);
        c.description_fields = new ArrayList<>();
        if (isSignal) {
            c.description_format = accessType + " in signal %s" + locksHeldDescription;
            RawComponentField field = new RawComponentField();
            field.setSignal((int)sid);
            c.description_fields.add(field);
        } else {
            c.description_format = accessType + " in thread " + otid + locksHeldDescription;
        }
        for (ReadonlyEventInterface lock : heldLocks) {
            c.description_fields.add(generateLockFieldJson(lock, metadata));
        }
        boolean atLeastOneKnownElementInTheTrace = generateComponentJson(c, metadata, e, heldLocks);
        for (int stackIndex = 1; stackIndex < signalStackEvents.size(); stackIndex++) {
            SignalStackEvent stackEvent = signalStackEvents.get(stackIndex);
            RawTraceComponent interrupting = new RawTraceComponent();
            interrupting.description_fields = new ArrayList<>();
            t.components.add(interrupting);
            int ttid = stackEvent.getTtid();
            Optional<ReadonlyEventInterface> maybeEvent = stackEvent.getEvent();
            String descriptionSuffix;
            if (!maybeEvent.isPresent()) {
                descriptionSuffix = " before any event";
                heldLocks = Collections.emptyList();
            } else {
                heldLocks = trace.getHeldLocksAt(maybeEvent.get());
                descriptionSuffix = getHeldLocksDescription(heldLocks);
            }
            if (trace.getThreadType(ttid) == ThreadType.THREAD) {
                interrupting.description_format = "Interrupting thread " + trace.getOriginalThreadIdForTraceThreadId(ttid) + descriptionSuffix;
            } else {
                interrupting.description_format = "Interrupting signal %s" + descriptionSuffix;
                RawComponentField field = new RawComponentField();
                field.setSignal((int)trace.getSignalNumber(ttid));
                interrupting.description_fields.add(field);
            }
            for (ReadonlyEventInterface lock : heldLocks) {
                interrupting.description_fields.add(generateLockFieldJson(lock, metadata));
            }
            if (maybeEvent.isPresent()) {
                generateComponentJson(interrupting, metadata, maybeEvent.get(), heldLocks);
            }
        }
        long parentOTID = trace.metadata().getParentOTID(otid);
        t.thread_id = Long.toString(otid);
        if (parentOTID > 0) {
            t.thread_created_by = Long.toString(parentOTID);
            long locId = trace.metadata().getOriginalThreadCreationLocId(otid);
            if (locId >= 0) {
                RawFrame created_at = new RawFrame();
                generateFrameJson(created_at, metadata, locId);
                t.thread_created_at = created_at;
            }
        }
        return atLeastOneKnownElementInTheTrace;
    }

    private boolean generateMemAccReport(
            ReadonlyEventInterface e, List<SignalStackEvent> signalStackEvents,
            MetadataInterface metadata, StringBuilder sb) {
        long otid = e.getOriginalThreadId();
        long sid = trace.getSignalNumber(trace.getTraceThreadId(e));
        List<ReadonlyEventInterface> heldLocks = trace.getHeldLocksAt(e);
        if (e.getSignalDepth() == 0) {
            sb.append(String.format("    %s in thread %s%s%n",
                    e.isWrite() ? "Write" : "Read",
                    otid,
                    getHeldLocksReport(heldLocks, metadata)));
        } else {
            sb.append(String.format("    %s in signal S%s%s%n",
                    e.isWrite() ? "Write" : "Read",
                    sid,
                    getHeldLocksReport(heldLocks, metadata)));
        }
        boolean atLeastOneKnownElementInTheTrace = generateStackTrace(e, heldLocks, sb);
        for (int stackIndex = 1; stackIndex < signalStackEvents.size(); stackIndex++) {
            SignalStackEvent stackEvent = signalStackEvents.get(stackIndex);
            sb.append("    Interrupting ");
            int ttid = stackEvent.getTtid();
            if (trace.getThreadType(ttid) == ThreadType.THREAD) {
                sb.append("thread ");
                sb.append(trace.getOriginalThreadIdForTraceThreadId(ttid));
            } else {
                sb.append("signal S");
                sb.append(trace.getSignalNumber(ttid));
            }
            Optional<ReadonlyEventInterface> maybeEvent = signalStackEvents.get(stackIndex).getEvent();
            if (!maybeEvent.isPresent()) {
                sb.append(" before any event.\n");
            } else {
                heldLocks = trace.getHeldLocksAt(maybeEvent.get());
                sb.append(getHeldLocksReport(heldLocks, metadata));
                sb.append("\n");
                generateStackTrace(maybeEvent.get(), heldLocks, sb);
            }
        }
        return atLeastOneKnownElementInTheTrace;
    }

    private boolean generateComponentJson(
            RawTraceComponent c,
            MetadataInterface metadata,
            ReadonlyEventInterface e,
            List<ReadonlyEventInterface> heldLocks) {
        long otid = e.getOriginalThreadId();
        int stackSize = 0;
        List<ReadonlyEventInterface> stacktrace = new ArrayList<>(trace.getStacktraceAt(e));
        stacktrace.addAll(heldLocks);
        stacktrace.sort((e1, e2) -> -e1.compareTo(e2));
        RawFrame last = null;
        c.frames = new ArrayList<>();
        for (int i = 0; i < stacktrace.size(); i++) {
            ReadonlyEventInterface elem = stacktrace.get(i);

            OptionalLong locId = findEventLocation(stacktrace, i, elem);
            if (!locId.isPresent()) {
                continue;
            }
            if (elem.isLock()) {
                if (!generateLockJson(last, metadata, elem, locId.getAsLong())) {
                    continue;
                } 
            } else {
                last = new RawFrame();
                if (!generateFrameJson(last, metadata, locId.getAsLong())) {
                    continue;
                }
                c.frames.add(last);
            }
            stackSize++;
        }
        return stackSize > 0;
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
                sb.append(String.format("    Thread %s created by thread %s%n", otid, parentOTID));
                if (locId >= 0) {
                    String locationSig = metadata.getLocationSig(locId);
                    signatureProcessor.process(locationSig);
                    sb.append(String.format("        %s%s%n", metadata.getLocationPrefix(), locationSig));
                } else {
                    sb.append(String.format("        %sunknown location%n", metadata.getLocationPrefix()));
                }
            } else {
                if (otid == 1) {
                    sb.append(String.format("    Thread %s is the main thread%n", otid));
                } else {
                    sb.append(String.format("    Thread %s is created by n/a%n", otid));
                }
            }
        }
        return stackSize>0;
    }

    private OptionalLong findEventLocation(
            List<ReadonlyEventInterface> stacktrace, int eventIndex, ReadonlyEventInterface event) {
        OptionalLong locId;
        if (!event.isInvokeMethod() || !config.isCompactTrace()) {
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

    private boolean generateFrameJson(
            RawFrame f, MetadataInterface metadata, long locId) {
        f.locks = new ArrayList<>();
        String locSig = locId >= 0 ? trace.metadata().getLocationSig(locId)
                : "... not available ...";
        f.address = locSig;
        if (config.isExcludedLibrary(locSig)) {
            return false;
        }
        if (locId >= 0) {
            signatureProcessor.process(locSig);
        }
        return true;
    }

    private boolean generateLockJson(
            RawFrame f, MetadataInterface metadata,
            ReadonlyEventInterface event, long locId) {
        String locSig = locId >= 0 ? metadata.getLocationSig(locId)
                : "... not available ...";
        RawLock l = new RawLock();
        RawField field = new RawField();
        field.address = metadata.getLockSig(event, trace);
        l.id = field;
        l.locked_at = metadata.getLocationSig(locId);
        if (config.isExcludedLibrary(locSig)) {
            return false;
        }
        if (locId > 0) {
            signatureProcessor.process(locSig);
        }
        f.locks.add(l);
        return true;
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
                    metadata.getLockSig(event, trace),
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

    private String getHeldLocksDescription(List<ReadonlyEventInterface> heldLocks) {
        if (heldLocks.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < heldLocks.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("%s");
        }
        final boolean plural = heldLocks.size() > 1;
        return String.format(" holding lock%s %s", plural ? "s" : "",
          sb.toString());
    }

    private RawComponentField generateLockFieldJson(ReadonlyEventInterface lock, MetadataInterface metadata) {
        RawComponentField cf = new RawComponentField();
        RawField f = new RawField();
        f.address = metadata.getLockSig(lock, trace);
        cf.setLock(f);
        return cf;
    }

    private String getHeldLocksReport(List<ReadonlyEventInterface> heldLocks, MetadataInterface metadata) {
        if (heldLocks.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < heldLocks.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(metadata.getLockSig(heldLocks.get(i), trace));
        }
        final boolean plural = heldLocks.size() > 1;
        return String.format(" holding lock%s %s", plural ? "s" : "",
          sb.toString());
    }
}
