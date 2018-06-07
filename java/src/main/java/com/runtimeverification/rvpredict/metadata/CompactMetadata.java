package com.runtimeverification.rvpredict.metadata;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.LockRepresentation;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
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
    public OptionalLong getOriginalThreadCreationLocId(long otid) {
        Pair<Long, Long> info = otidToCreationInfo.get(otid);
        return info == null ? OptionalLong.empty() : OptionalLong.of(info.getRight());
    }

    @Override
    public OptionalLong getParentOTID(long otid) {
        Pair<Long, Long> info = otidToCreationInfo.get(otid);
        return info == null ? OptionalLong.empty() : OptionalLong.of(info.getLeft());
    }

    @Override
    public String getRaceDataSig(
            ReadonlyEventInterface e1,
            Collection<ReadonlyEventInterface> stackTrace1,
            Collection<ReadonlyEventInterface> stackTrace2,
            Configuration config) {
        StringBuilder sb = new StringBuilder();
        long idx = e1.getDataObjectExternalIdentifier();
        formatAddressWithBracketing(idx, Arrays.asList(stackTrace1, stackTrace2), sb);
        return sb.toString();
    }

    @Override
    public String getVariableSig(long idx) {
        return String.format("[0x%016x]", idx);
    }

    @Override
    public String getLockSig(ReadonlyEventInterface event, Collection<ReadonlyEventInterface> stackTrace) {
        StringBuilder sb = new StringBuilder();
        LockRepresentation lockRepresentation = event.getLockRepresentation();
        /* Don't print this "<Type>Lock@" prefix, for now, because it really
         * confuses the symbolization / report generation.
         */
        if (false && lockRepresentation.getLockType() != LockRepresentation.LockType.WRITE_LOCK) {
            // All the compact trace locks should be write locks, this code is just in case
            // something unexpected happens.
            sb.append(lockRepresentation.getLockName());
            sb.append("@");
        }
        formatAddressWithBracketing(
                lockRepresentation.getLockAddress(), Collections.singletonList(stackTrace), sb);
        return sb.toString();
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

    private void formatAddressWithBracketing(
            long address, List<Collection<ReadonlyEventInterface>> stackTraces, StringBuilder sb) {
        sb.append(String.format("[0x%016x", address));
        generateBracketing(address, stackTraces, sb);
        sb.append("]");
    }

    private void generateBracketing(
            long address, List<Collection<ReadonlyEventInterface>> stackTraces, StringBuilder sb) {
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
        String delim = " :";
        if (maybeCfaAfter.isPresent()) {
            ReadonlyEventInterface cfaAfter = maybeCfaAfter.get();
            sb.append(String.format("%s 0x%016x/0x%016x",
                    delim,
                    cfaAfter.getLocationId(),
                    cfaAfter.getCanonicalFrameAddress()));
            delim = "";
        }
        Optional<ReadonlyEventInterface> maybeCfaBefore = globalBefore.getBracket();
        if (maybeCfaBefore.isPresent()) {
            ReadonlyEventInterface cfaBefore = maybeCfaBefore.get();
            sb.append(String.format("%s 0x%016x/0x%016x",
                    delim,
                    cfaBefore.getLocationId(),
                    cfaBefore.getCanonicalFrameAddress()));
        }
    }

    @Override
    public boolean isVolatile(long addressForVolatileCheck) {
        return false;
    }
}
