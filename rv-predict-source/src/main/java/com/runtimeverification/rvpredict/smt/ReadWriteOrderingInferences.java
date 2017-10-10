package com.runtimeverification.rvpredict.smt;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Infers some more happen-before relations, either directly or through an intermediate not-before relation.
 *
 * 1. If all writes for a given value are on a single thread, then none of the reads can be before the first write.
 * Normally we would say that they must be after that write, but we may want to detect a race between the first write
 * and one of its reads, so we want to be able to say that those happen at the same time. Note that, on a given thread,
 * this may happen only to the first read with that value, all the other ones are surely after the first write.
 *
 * 2. NOT IMPLEMENTED, SEE BELOW. If there are reads for a value that was not written, then those reads must not be
 * after any write for the same variable, so, again, we have a not-before relation between any write and those reads.
 *
 * However, as above, we must consider if these not-before relations prevent us from detecting races. Let us consider
 * the following example, where there is no write(x, 1):
 * thread 1: ... e1 = some_event ... e2 = read(x, 1) ... e3 = some_event ...
 * thread 2: ... e4 = write(x, 2) ... e5 = some_event ...
 * If we just add an "e4 not before e2" relation, then we will have a "e1 before e5" relation, which means that we will
 * never be able to detect a race between those two. We could try to use that relation only when attempting to detect a
 * race between e3 and e5 or something similar, but then we wouldn't be able to use these relations for filtering out
 * race candidates. Right now this is the only purpose of this class, so this is not implemented yet.
 *
 * Note. If a read is not-before a write, then it must be after everything that precedes that write, so we can add
 * a happens-before relation between the read and the direct predecessors of the write. However, that is not
 * implemented here, we're just telling the {@link TransitiveClosure} class what relations we inferred.
 *
 * TODO(virgil) 1. Right now we're inferring relations for values that are written from a single thread.
 * However, we could take existing happens-before relations into account and we could infer that a given write can't
 * be relevant for a given read. As an example, any write which is forced to be after a read is not relevant. Also,
 * if the maximal writes before a given read have different values, then all writes which are before that read are
 * irrelevant. If, after ignoring all these writes, for a given read there is only one possible relevant written value,
 * then we may be able to add a not-happens-before relation as described above.
 *
 * TODO(virgil) 2. This does not belong in this class, but in a DisjointSections class: we could also find sections
 * which are disjoint. All sections with the same lock are disjoint and all atomic events are disjoint, but there are
 * also sections which are implicitly disjoint, e.g. if there are sections on threads that look like:
 *
 * lock l; read x, 0; write x, 1; unlock l ... section ... lock l; write x, 0; unlock l
 *
 * and there is no other way to set x to 0 except something similar to the above, then all those sections are disjoint.
 * It may be hard to generalize this, but we could either ask the SMT which sections are disjoint, or we could do some
 * simple generalizations.
 *
 * Anyway, from these disjoint sections we may also infer (not-)happens-before relations.
 */
class ReadWriteOrderingInferences {
    private Map<Long, List<Long>> notBefore = new HashMap<>();
    private Map<Long, List<Long>> before = new HashMap<>();

    // TODO(virgil): I should compute this outside.
    private Map<Long, ReadonlyEventInterface> eventIdToEvent = new HashMap<>();

    /**
     * Extract not-happens-before and happens-before inferences from the given list of events.
     *
     * @param ttidToAllEvents Events in the current window, grouped by ttid.
     */
    ReadWriteOrderingInferences(Map<Integer, List<ReadonlyEventInterface>> ttidToAllEvents) {
        Map<Integer, List<ReadonlyEventInterface>> ttidToInterestingEvents = new HashMap<>();
        ttidToAllEvents.forEach((ttid, events) -> {
            List<ReadonlyEventInterface> readsAndWrites =
                    events.stream().filter(ReadonlyEventInterface::isReadOrWrite).collect(Collectors.toList());
            ttidToInterestingEvents.put(ttid, readsAndWrites);
        });

        Map<Long, Map<Long, Map<Integer, List<ReadonlyEventInterface>>>> variableToValueToTtidToReads =
                createVariableToValueToTtidToDataAccessMap(ttidToInterestingEvents, ReadonlyEventInterface::isRead);
        Map<Long, Map<Long, Map<Integer, List<ReadonlyEventInterface>>>> variableToValueToTtidToWrites =
                createVariableToValueToTtidToDataAccessMap(ttidToInterestingEvents, ReadonlyEventInterface::isWrite);
        addToEventIdToEvent(variableToValueToTtidToReads);
        addToEventIdToEvent(variableToValueToTtidToWrites);

        loadRelationsFromValuesWrittenOnce(variableToValueToTtidToReads, variableToValueToTtidToWrites);
    }

    /**
     * Adds the inferred relations to a {@link TransitiveClosure}.
     */
    void addToMhb(TransitiveClosure.Builder mhbClosureBuilder, Optional<TransitiveClosure> happensBefore) {
        before.forEach((beforeId, afters) -> {
            ReadonlyEventInterface before = eventIdToEvent.get(beforeId);
            afters.stream()
                    .map(after -> eventIdToEvent.get(after))
                    // Filtering needed only because old regression tests contain broken data.
                    .filter(after -> !happensBefore.isPresent() || !happensBefore.get().inRelation(after, before))
                    .forEach(after -> mhbClosureBuilder.addRelation(before, after));
        });
        notBefore.forEach((notBeforeId, notAfters) -> {
            ReadonlyEventInterface notBefore = eventIdToEvent.get(notBeforeId);
            notAfters.stream()
                    .map(notAfter -> eventIdToEvent.get(notAfter))
                    // Filtering needed only because old regression tests contain broken data.
                    .filter(notAfter ->
                            !happensBefore.isPresent() || !happensBefore.get().inRelation(notBefore, notAfter))
                    .forEach(notAfter -> mhbClosureBuilder.addNotRelation(notBefore, notAfter));
        });
    }

    private void loadRelationsFromValuesWrittenOnce(
            Map<Long, Map<Long, Map<Integer, List<ReadonlyEventInterface>>>> variableToValueToTtidToReads,
            Map<Long, Map<Long, Map<Integer, List<ReadonlyEventInterface>>>> variableToValueToTtidToWrites) {
        Map<Long, ReadonlyEventInterface> variableToFirstRead =
                computeVariableToFirstAccess(variableToValueToTtidToReads);
        Map<Long, ReadonlyEventInterface> variableToFirstWrite =
                computeVariableToFirstAccess(variableToValueToTtidToWrites);
        variableToValueToTtidToWrites.forEach((writeVariable, valueToTtidToWrites) -> {
            ReadonlyEventInterface firstWriteOfVariableInWindow = variableToFirstWrite.get(writeVariable);
            assert firstWriteOfVariableInWindow != null;
            ReadonlyEventInterface firstReadOfVariableInWindow = variableToFirstRead.get(writeVariable);
            if (firstReadOfVariableInWindow == null) {
                return;
            }
            Map<Long, Map<Integer, List<ReadonlyEventInterface>>> valueToTtidToReads =
                    variableToValueToTtidToReads.get(writeVariable);
            if (valueToTtidToReads == null) {
                return;
            }
            valueToTtidToWrites.forEach((writeValue, ttidToWrites) -> {
                if (ttidToWrites.size() != 1) {
                    return;
                }
                if (firstReadOfVariableInWindow.getEventId() < firstWriteOfVariableInWindow.getEventId()
                        && writeValue == firstReadOfVariableInWindow.getDataValue()) {
                    // these reads may read the initial value of the variable, so they don't need to be after the
                    // write. One could refine this by letting reads which had a write on the same thread go through
                    // but for now we're rejecting everything.
                    return;
                }
                Map<Integer, List<ReadonlyEventInterface>> ttidToReads = valueToTtidToReads.get(writeValue);
                if (ttidToReads == null) {
                    return;
                }
                int writeTtid = ttidToWrites.keySet().iterator().next();
                List<ReadonlyEventInterface> writes = ttidToWrites.get(writeTtid);
                assert !writes.isEmpty();
                ReadonlyEventInterface write = writes.get(0);
                ttidToReads.forEach((readTtid, reads) -> {
                    if (readTtid == writeTtid) {
                        return;
                    }
                    addRelation(reads.get(0), write, true);
                    if (reads.size() > 1) {
                        addRelation(reads.get(1), write, false);
                    }
                });
            });
        });
    }

    private void addRelation(
            ReadonlyEventInterface notBefore, ReadonlyEventInterface notAfter,
            boolean canBeEqual) {
        if (canBeEqual) {
            this.notBefore.computeIfAbsent(notBefore.getEventId(), k -> new ArrayList<>()).add(notAfter.getEventId());
        } else {
            before.computeIfAbsent(notAfter.getEventId(), k -> new ArrayList<>()).add(notBefore.getEventId());
        }
    }

    private void addToEventIdToEvent(
            Map<Long, Map<Long, Map<Integer, List<ReadonlyEventInterface>>>> variableToValueToTtidToDataAccesses) {
        variableToValueToTtidToDataAccesses.values().forEach(valueToTtidToDataAccesses ->
                valueToTtidToDataAccesses.values().forEach(ttidToDataAccesses ->
                        ttidToDataAccesses.values().forEach(dataAccesses ->
                                dataAccesses.forEach(event -> eventIdToEvent.put(event.getEventId(), event)))));
    }

    private Map<Long, ReadonlyEventInterface> computeVariableToFirstAccess(
            Map<Long, Map<Long, Map<Integer, List<ReadonlyEventInterface>>>> variableToValueToTtidToDataAccesses) {
        Map<Long, ReadonlyEventInterface> variableToFirstAccess = new HashMap<>();
        variableToValueToTtidToDataAccesses.forEach((variable, valueToTtidToDataAccesses) -> {
            Optional<ReadonlyEventInterface> maybeMinAccess =
                    valueToTtidToDataAccesses.values().stream()
                            .map(ttidToDataAccesses -> ttidToDataAccesses.values().stream()
                                    .map(dataAccesses -> dataAccesses.get(0))
                                    .min(Comparator.comparingLong(ReadonlyEventInterface::getEventId)))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .min(Comparator.comparingLong(ReadonlyEventInterface::getEventId));
            assert maybeMinAccess.isPresent();
            variableToFirstAccess.put(
                    variable,
                    maybeMinAccess.get());
        });
        return variableToFirstAccess;
    }

    private Map<Long, Map<Long, Map<Integer, List<ReadonlyEventInterface>>>>
    createVariableToValueToTtidToDataAccessMap(
            Map<Integer, List<ReadonlyEventInterface>> ttidToEvents,
            Predicate<ReadonlyEventInterface> shouldInclude) {
        Map<Long, Map<Long, Map<Integer, List<ReadonlyEventInterface>>>> variableToValueToTtidToAccess =
                new HashMap<>();
        ttidToEvents.forEach((ttid, events) ->
                events.stream()
                        .filter(shouldInclude)
                        .forEach(event -> variableToValueToTtidToAccess
                                .computeIfAbsent(event.getDataInternalIdentifier(), k -> new HashMap<>())
                                .computeIfAbsent(event.getDataValue(), k -> new HashMap<>())
                                .computeIfAbsent(ttid, k -> new ArrayList<>())
                                .add(event)));
        variableToValueToTtidToAccess.forEach((variable, valueToTtidToAccess) ->
                valueToTtidToAccess.forEach((value, ttidToAccess) ->
                        ttidToAccess.forEach((ttid, access) -> access.sort(
                                Comparator.comparingLong(ReadonlyEventInterface::getEventId)))));
        return variableToValueToTtidToAccess;
    }
}
