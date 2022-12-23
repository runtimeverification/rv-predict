package com.runtimeverification.rvpredict.violation;

import com.runtimeverification.error.data.ErrorCategory;
import com.runtimeverification.error.data.Language;
import com.runtimeverification.error.data.RawComponentField;
import com.runtimeverification.error.data.RawField;
import com.runtimeverification.error.data.RawFrame;
import com.runtimeverification.error.data.RawLock;
import com.runtimeverification.error.data.RawStackError;
import com.runtimeverification.error.data.RawStackTrace;
import com.runtimeverification.error.data.RawStackTraceComponent;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.metadata.SignatureProcessor;
import com.runtimeverification.rvpredict.trace.ThreadType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Helper for serializing races. One should call 'startNewRace' between races
 * (Since a race could be processed in multiple pieces, this can't be called automatically).
 */
public class RaceSerializer {
    private final Configuration config;
    private final SignatureProcessor signatureProcessor;
    private final MetadataInterface metadata;

    public RaceSerializer(Configuration config, SignatureProcessor signatureProcessor, MetadataInterface metadata) {
        this.config = config;
        this.signatureProcessor = signatureProcessor;
        this.metadata = metadata;
        startNewRace();
    }

    Optional<RawStackError> generateErrorData(
            ReadonlyEventInterface e1, ReadonlyEventInterface e2,
            List<SignalStackEvent> firstSignalStack,
            List<SignalStackEvent> secondSignalStack,
            boolean isSignalRace) {
        RawStackError error = new RawStackError();
        error.description_format = "Data race on %s";

        assert !firstSignalStack.isEmpty();
        assert !secondSignalStack.isEmpty();
        RawField f = new RawField();
        f.address = getRaceDataSig(
                e1, firstSignalStack.get(0).getStackTrace(), secondSignalStack.get(0).getStackTrace());
        error.description_fields = new ArrayList<>();
        error.description_fields.add(f);

        RawStackTrace t1 = new RawStackTrace();
        RawStackTrace t2 = new RawStackTrace();
        boolean reportableRace = fillStackTraceData(t1, e1, firstSignalStack);
        reportableRace = fillStackTraceData(t2, e2, secondSignalStack) | reportableRace;
        error.stack_traces = new ArrayList<>();
        if (getLocationSig(e1.getLocationId())
                .compareTo(getLocationSig(e2.getLocationId())) <= 0) {
            error.stack_traces.add(t1);
            error.stack_traces.add(t2);
        } else {
            error.stack_traces.add(t2);
            error.stack_traces.add(t1);
        }

        error.category = getErrorCategory(isSignalRace);
        error.error_id = getErrorId(e1, e2, isSignalRace);
        return reportableRace ? Optional.of(error) : Optional.empty();
    }

    RawStackTraceComponent generatePrimaryComponentData(ReadonlyEventInterface e, SignalStackEvent stack) {
        String accessType = e.isWrite() ? "Write" : "Read";
        String locksHeldDescription = getHeldLocksDescription(stack.getHeldLocks());
        RawStackTraceComponent primaryComponent = generateComponentData(stack);
        if (stack.getThreadType() == ThreadType.SIGNAL) {
            primaryComponent.description_format = accessType + " in signal %s" + locksHeldDescription;
            RawComponentField field = new RawComponentField();
            assert stack.getSignalNumber().isPresent();
            field.setSignal((int)stack.getSignalNumber().getAsLong());
            primaryComponent.description_fields.add(field);
        } else {
            primaryComponent.description_format =
                    accessType + " in thread " + stack.getOriginalThreadId() + locksHeldDescription;
        }
        for (HeldLock lock : stack.getHeldLocks()) {
            primaryComponent.description_fields.add(generateLockFieldData(lock, metadata));
        }
        return primaryComponent;
    }

    String getRaceDataSig(
            ReadonlyEventInterface event,
            Collection<ReadonlyEventInterface> firstStack,
            Collection<ReadonlyEventInterface> secondStack) {
        return metadata.getRaceDataSig(event, firstStack, secondStack, config);
    }

    public String getLocationSig(long locationId) {
        return metadata.getLocationSig(locationId);
    }

    ErrorCategory getErrorCategory(boolean isSignalRace) {
        ErrorCategory cat = new ErrorCategory();
        if (isSignalRace) {
            cat.setLintError();
        } else {
            Language lang = new Language();
            // TODO: get the language
            lang.setC();
            cat.setUndefined(lang);
        }
        return cat;
    }

    void startNewRace() {
        signatureProcessor.reset();
    }

    String simplify(String thing) {
        return signatureProcessor.simplify(thing);
    }

    private boolean fillStackTraceData(
            RawStackTrace t,
            ReadonlyEventInterface e,
            List<SignalStackEvent> signalStackEvents) {
        long otid = e.getOriginalThreadId();
        t.components = new ArrayList<>();
        assert !signalStackEvents.isEmpty();
        RawStackTraceComponent primaryComponent =
                generatePrimaryComponentData(e, signalStackEvents.get(0));
        t.components.add(primaryComponent);
        for (int stackIndex = 1; stackIndex < signalStackEvents.size(); stackIndex++) {
            SignalStackEvent stackEvent = signalStackEvents.get(stackIndex);
            RawStackTraceComponent interrupting = generateInterruptingComponentData(stackEvent);
            t.components.add(interrupting);
        }
        OptionalLong parentOTID = metadata.getParentOTID(otid);
        t.thread_id = Long.toString(otid);
        if (parentOTID.isPresent()) {
            t.thread_created_by = Long.toString(parentOTID.getAsLong());
            OptionalLong locId = metadata.getOriginalThreadCreationLocId(otid);
            if (locId.isPresent()) {
                t.thread_created_at = generateFrameData(locId.getAsLong()).orElse(null);
            }
        }
        return primaryComponent.frames.size() > 0;
    }

    private RawComponentField generateLockFieldData(HeldLock lock, MetadataInterface metadata) {
        RawComponentField cf = new RawComponentField();
        RawField f = new RawField();
        f.address = metadata.getLockSig(lock.getLock(), lock.getStackTrace());
        cf.setLock(f);
        return cf;
    }

    private Optional<RawLock> generateLockData(
            ReadonlyEventInterface event,
            Collection<ReadonlyEventInterface> stackTrace,
            long locId) {
        Optional<String> locSig = resolveLocId(locId);
        if (!locSig.isPresent()) {
            return Optional.empty();
        }

        RawLock l = new RawLock();
        RawField field = new RawField();
        field.address = metadata.getLockSig(event, stackTrace);
        l.id = field;
        l.locked_at = locSig.get();
        return Optional.of(l);
    }

    private RawStackTraceComponent generateComponentData(SignalStackEvent stack) {
        RawStackTraceComponent c = new RawStackTraceComponent();
        c.description_fields = new ArrayList<>();
        List<ReadonlyEventInterface> stackTrace = new ArrayList<>(stack.getStackTrace());
        Map<Long, HeldLock> idToLock = new HashMap<>();
        stack.getHeldLocks().forEach(lock -> {
            stackTrace.add(lock.getLock());
            idToLock.put(lock.getLock().getEventId(), lock);
        });
        stackTrace.sort((e1, e2) -> -e1.compareTo(e2));
        RawFrame lastFrame = null;
        c.frames = new ArrayList<>();
        for (int i = 0; i < stackTrace.size(); i++) {
            ReadonlyEventInterface elem = stackTrace.get(i);

            long locId = findEventLocation(stackTrace, i, elem, config);
            if (elem.isLock()) {
                Optional<RawLock> lock = generateLockData(
                        elem,
                        idToLock.get(elem.getEventId()).getStackTrace(),
                        locId);
                if (lock.isPresent()) {
                    assert lastFrame != null;
                    lastFrame.locks.add(lock.get());
                }
                continue;
            }
            Optional<RawFrame> frame = generateFrameData(locId);
            if (frame.isPresent()) {
                lastFrame = frame.get();
                c.frames.add(lastFrame);
            }
        }
        return c;
    }

    private Optional<RawFrame> generateFrameData(long locId) {
        Optional<String> locSig = resolveLocId(locId);
        if (!locSig.isPresent()) {
            return Optional.empty();
        }

        RawFrame f = new RawFrame();
        f.locks = new ArrayList<>();
        f.address = locSig.get();
        return Optional.of(f);
    }

    private Optional<String> resolveLocId(long locId) {
        String locSig;
        if (locId >= 0) {
            locSig = metadata.getLocationSig(locId);
            if (config.isExcludedLibrary(locSig)) {
                return Optional.empty();
            }
            signatureProcessor.process(locSig);
        } else {
            locSig = "... not available ...";
            if (config.isExcludedLibrary(locSig)) {
                return Optional.empty();
            }
        }
        return Optional.of(locSig);
    }

    // -----------------------------------------------------

    // TODO(virgil): Remove this and most methods called by it and just do a json -> string conversion.
    boolean generateMemAccReport(
            List<SignalStackEvent> signalStackEvents, StringBuilder sb) {
        assert !signalStackEvents.isEmpty();
        SignalStackEvent mainStack = signalStackEvents.get(0);
        boolean atLeastOneKnownElementInTheTrace =
                generateMainComponentMemAccReport(mainStack, sb);
        List<HeldLock> heldLocks;
        for (int stackIndex = 1; stackIndex < signalStackEvents.size(); stackIndex++) {
            SignalStackEvent stackEvent = signalStackEvents.get(stackIndex);
            sb.append("    Interrupting ");
            if (stackEvent.getSignalNumber().isPresent()) {
                sb.append("signal S");
                sb.append(stackEvent.getSignalNumber().getAsLong());
            } else {
                sb.append("thread ");
                sb.append(stackEvent.getOriginalThreadId());
            }
            Optional<ReadonlyEventInterface> maybeEvent = signalStackEvents.get(stackIndex).getEvent();
            if (!maybeEvent.isPresent()) {
                sb.append(" before any event.\n");
            } else {
                heldLocks = stackEvent.getHeldLocks();
                sb.append(getHeldLocksReport(heldLocks));
                sb.append("\n");
                generateStackTrace(maybeEvent.get(), stackEvent, sb);
            }
        }
        return atLeastOneKnownElementInTheTrace;
    }

    boolean generateMainComponentMemAccReport(
            SignalStackEvent mainStack, StringBuilder sb) {
        long otid = mainStack.getOriginalThreadId();
        List<HeldLock> heldLocks = mainStack.getHeldLocks();
        assert mainStack.getEvent().isPresent();
        ReadonlyEventInterface e = mainStack.getEvent().get();
        if (mainStack.getThreadType() == ThreadType.THREAD) {
            sb.append(String.format("    %s in thread %s%s%n",
                    e.isWrite() ? "Write" : "Read",
                    otid,
                    getHeldLocksReport(heldLocks)));
        } else {
            assert mainStack.getSignalNumber().isPresent();
            long sid = mainStack.getSignalNumber().getAsLong();
            sb.append(String.format("    %s in signal S%s%s%n",
                    e.isWrite() ? "Write" : "Read",
                    sid,
                    getHeldLocksReport(heldLocks)));
        }
        return generateStackTrace(e, mainStack, sb);
    }

    private RawStackTraceComponent generateInterruptingComponentData(SignalStackEvent stackEvent) {
        Optional<ReadonlyEventInterface> maybeEvent = stackEvent.getEvent();
        String descriptionSuffix;
        RawStackTraceComponent interrupting;
        if (maybeEvent.isPresent()) {
            descriptionSuffix = getHeldLocksDescription(stackEvent.getHeldLocks());
            interrupting = generateComponentData(stackEvent);
        } else {
            descriptionSuffix = " before any event";
            interrupting = new RawStackTraceComponent();
            interrupting.description_fields = new ArrayList<>();
            interrupting.frames = new ArrayList<>();
        }
        if (stackEvent.getThreadType() == ThreadType.THREAD) {
            interrupting.description_format =
                    "Interrupting thread " + stackEvent.getOriginalThreadId() + descriptionSuffix;
        } else {
            interrupting.description_format = "Interrupting signal %s" + descriptionSuffix;
            RawComponentField field = new RawComponentField();
            assert stackEvent.getSignalNumber().isPresent();
            field.setSignal(Math.toIntExact(stackEvent.getSignalNumber().getAsLong()));
            interrupting.description_fields.add(field);
        }
        for (HeldLock lock : stackEvent.getHeldLocks()) {
            interrupting.description_fields.add(generateLockFieldData(lock, metadata));
        }
        return interrupting;
    }

    private String getHeldLocksReport(List<HeldLock> heldLocks) {
        if (heldLocks.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < heldLocks.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            HeldLock lock = heldLocks.get(i);
            sb.append(metadata.getLockSig(lock.getLock(), lock.getStackTrace()));
        }
        final boolean plural = heldLocks.size() > 1;
        return String.format(" holding lock%s %s", plural ? "s" : "",
                sb.toString());
    }

    private boolean generateStackTrace(
            ReadonlyEventInterface e,
            SignalStackEvent stackEvent,
            StringBuilder sb) {
        long otid = e.getOriginalThreadId();
        int stackSize = 0;
        boolean isTopmostStack = true;

        List<ReadonlyEventInterface> stackTrace = new ArrayList<>(stackEvent.getStackTrace());
        Map<Long, HeldLock> idToLock = new HashMap<>();
        stackEvent.getHeldLocks().forEach(lock -> {
            stackTrace.add(lock.getLock());
            idToLock.put(lock.getLock().getEventId(), lock);
        });
        stackTrace.sort((e1, e2) -> -e1.compareTo(e2));

        for (int i = 0; i < stackTrace.size(); i++) {
            ReadonlyEventInterface elem = stackTrace.get(i);

            long locId = findEventLocation(stackTrace, i, elem, config);
            if (elem.isLock()) {
                if (!displayOneStackLocation(
                        isTopmostStack, elem, locId,
                        idToLock.get(elem.getEventId()).getStackTrace(),
                        sb)) {
                    continue;
                }
            } else {
                if (!displayOneStackLocation(
                        isTopmostStack, elem, locId,
                        Collections.emptyList(),
                        sb)) {
                    continue;
                }
            }
            stackSize++;
            isTopmostStack = false;
        }

        if (stackEvent.getThreadType() == ThreadType.THREAD) {
            OptionalLong parentOTID = metadata.getParentOTID(otid);
            if (parentOTID.isPresent()) {
                OptionalLong locId = metadata.getOriginalThreadCreationLocId(otid);
                sb.append(String.format("    Thread %s was created by thread %s%n", otid, parentOTID.getAsLong()));
                if (locId.isPresent()) {
                    String locationSig = metadata.getLocationSig(locId.getAsLong());
                    signatureProcessor.process(locationSig);
                    sb.append(String.format("        %s%s%n", metadata.getLocationPrefix(), locationSig));
                } else {
                    sb.append(String.format("        %sunknown location%n", metadata.getLocationPrefix()));
                }
            } else {
                if (otid == 1) {
                    sb.append(String.format("    Thread %s is the main thread%n", otid));
                } else {
                    sb.append(String.format("    Thread %s was created by n/a%n", otid));
                }
            }
        }
        return stackSize > 0;
    }

    private boolean displayOneStackLocation(
            boolean isTopmostStack,
            ReadonlyEventInterface event,
            long locId,
            Collection<ReadonlyEventInterface> stackTrace,
            StringBuilder sb) {
        Optional<String> maybeLocSig = resolveLocId(locId);
        if (!maybeLocSig.isPresent()) {
            assert !event.isLock() : "Locations for locks should have been handled in TraceState::updateLockLocToUserLoc";
            return false;
        }
        if (event.isLock()) {
            sb.append(String.format(
                    "        - locked %s %s%s%n",
                    metadata.getLockSig(event, stackTrace),
                    metadata.getLocationPrefix(),
                    maybeLocSig.get()));
        } else {
            sb.append(String.format(
                    "      %s %s%s%n",
                    isTopmostStack ? ">" : " ",
                    metadata.getLocationPrefix(),
                    maybeLocSig.get()));
        }
        return true;
    }

    // -----------------------------------------------------


    private String getErrorId(ReadonlyEventInterface e1, ReadonlyEventInterface e2, boolean isSignalRace) {
        if (isSignalRace) {
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

    private long findEventLocation(
            List<ReadonlyEventInterface> stacktrace, int eventIndex, ReadonlyEventInterface event,
            Configuration config) {
        OptionalLong locId;
        if (!event.isInvokeMethod() || !config.isCompactTrace()) {
            return event.getLocationId();
        }
        locId = event.getCallSiteAddress();
        if (!locId.isPresent()) {
            for (int j = eventIndex + 1; j < stacktrace.size(); j++) {
                ReadonlyEventInterface callingFunction = stacktrace.get(j);
                if (callingFunction.isInvokeMethod()) {
                    return callingFunction.getLocationId();
                }
            }
        }
        if (!locId.isPresent()) {
            return event.getLocationId();
        }
        return locId.getAsLong();
    }

    // TODO: Work with HeldLock.
    private String getHeldLocksDescription(List<HeldLock> heldLocks) {
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
}
