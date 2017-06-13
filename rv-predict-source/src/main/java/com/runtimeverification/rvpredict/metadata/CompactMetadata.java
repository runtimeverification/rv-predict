package com.runtimeverification.rvpredict.metadata;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.trace.Trace;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CompactMetadata implements MetadataInterface {
    private final Map<Long, Pair<Long, Long>> otidToCreationInfo = new ConcurrentHashMap<>();

    @Override
    public String getLocationSig(long locationId) {
        return String.format("{0x%016x}", locationId);
    }

    @Override
    public String getLocationPrefix() {
        return "";
    }

    @Override
    public void addOriginalThreadCreationInfo(long childOTID, long parentOTID, long locId) {
        otidToCreationInfo.put(childOTID, Pair.of(parentOTID, locId));
    }

    @Override
    public long getOriginalThreadCreationLocId(long otid) {
        Pair<Long, Long> info = otidToCreationInfo.get(otid);
        return info == null ? -1 : info.getRight();
    }

    @Override
    public long getParentOTID(long otid) {
        Pair<Long, Long> info = otidToCreationInfo.get(otid);
        return info == null ? 0 : info.getLeft();
    }

    @Override
    public String getRaceLocationSig(ReadonlyEventInterface e1,
            ReadonlyEventInterface e2, Trace trace, Configuration config) {
        StringBuilder sb = new StringBuilder();
        long idx = e1.getDataObjectExternalIdentifier();
        sb.append(String.format("[0x%016x", idx));
        generateBracketing(e1, Arrays.asList(trace.getStacktraceAt(e1), trace.getStacktraceAt(e2)), sb);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String getVariableSig(long idx) {
        return String.format("[0x%016x]", idx);
    }

    private static class EventBracket {
        private Optional<ReadonlyEventInterface> bracket = Optional.empty();
        private final long target;
        private final Where where;

        private enum Where {
            BEFORE(-1),
            AFTER(1);

            private final long comparisonSign;

            Where(long comparisonSign) {
                this.comparisonSign = comparisonSign;
            }

            public long getComparisonSign() {
                return comparisonSign;
            }
        }

        private EventBracket(long target, Where where) {
            this.target = target;
            this.where = where;
        }

        private void processStackEvent(ReadonlyEventInterface event) {
            long eventCfa = event.getCanonicalFrameAddress();
            if (bracket.isPresent()) {
                long bracketCfa = bracket.get().getCanonicalFrameAddress();
                if (Math.signum(bracketCfa - eventCfa) * (target - eventCfa) < 0) {
                    bracket = Optional.of(event);
                }
            } else if ((eventCfa - target) * where.getComparisonSign() > 0) {
                bracket = Optional.of(event);
            }
        }

        Optional<ReadonlyEventInterface> getBracket() {
            return bracket;
        }
    }

    private void generateBracketing(
            ReadonlyEventInterface event, List<Collection<ReadonlyEventInterface>> stackTraces, StringBuilder sb) {
        long address = event.getDataObjectExternalIdentifier();
        EventBracket globalBefore = new EventBracket(address, EventBracket.Where.BEFORE);
        EventBracket globalAfter = new EventBracket(address, EventBracket.Where.AFTER);
        for (Collection<ReadonlyEventInterface> stack : stackTraces) {
            EventBracket stackBefore = new EventBracket(address, EventBracket.Where.BEFORE);
            EventBracket stackAfter = new EventBracket(address, EventBracket.Where.AFTER);
            stack.stream().filter(ReadonlyEventInterface::isCallStackEvent).forEach(stackEvent -> {
                stackBefore.processStackEvent(stackEvent);
                stackAfter.processStackEvent(stackEvent);
            });
            Optional<ReadonlyEventInterface> maybeCfaBefore = stackBefore.getBracket();
            Optional<ReadonlyEventInterface> maybeCfaAfter = stackAfter.getBracket();

            maybeCfaBefore.ifPresent(globalBefore::processStackEvent);
            maybeCfaAfter.ifPresent(globalAfter::processStackEvent);

            if (maybeCfaBefore.isPresent() && maybeCfaAfter.isPresent()) {
                ReadonlyEventInterface cfaBefore = maybeCfaBefore.get();
                ReadonlyEventInterface cfaAfter = maybeCfaAfter.get();
                sb.append(String.format(" : 0x%016x/0x%016x 0x%016x/0x%016x",
                        cfaBefore.getLocationId(),
                        cfaBefore.getCanonicalFrameAddress(),
                        cfaAfter.getLocationId(),
                        cfaAfter.getCanonicalFrameAddress()));
                return;
            }
        }
        Optional<ReadonlyEventInterface> maybeCfaAfter = globalAfter.getBracket();
        if (maybeCfaAfter.isPresent()) {
            ReadonlyEventInterface cfaAfter = maybeCfaAfter.get();
            sb.append(String.format(" : 0x%016x/0x%016x",
                    cfaAfter.getLocationId(),
                    cfaAfter.getCanonicalFrameAddress()));
        }
        Optional<ReadonlyEventInterface> maybeCfaBefore = globalBefore.getBracket();
        if (maybeCfaBefore.isPresent()) {
            ReadonlyEventInterface cfaBefore = maybeCfaBefore.get();
            sb.append(String.format(" : 0x%016x/0x%016x",
                    cfaBefore.getLocationId(),
                    cfaBefore.getCanonicalFrameAddress()));
        }
    }

    @Override
    public boolean isVolatile(long addressForVolatileCheck) {
        return false;
    }
}
