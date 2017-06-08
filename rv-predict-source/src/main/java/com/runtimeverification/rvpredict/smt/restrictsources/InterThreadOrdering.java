package com.runtimeverification.rvpredict.smt.restrictsources;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.RestrictSourceWithHappensBefore;
import com.runtimeverification.rvpredict.smt.TransitiveClosure;
import com.runtimeverification.rvpredict.smt.restricts.And;
import com.runtimeverification.rvpredict.smt.restricts.Before;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class InterThreadOrdering implements RestrictSourceWithHappensBefore {
    private final List<ReadonlyEventInterface> interThreadSyncEvents;
    private final Function<Long, OptionalInt> otidToTtidConverter;
    private final Function<Integer, Optional<ReadonlyEventInterface>> firstEventExtractor;
    private final Function<Integer, Optional<ReadonlyEventInterface>> lastEventExtractor;

    public InterThreadOrdering(
            List<ReadonlyEventInterface> interThreadSyncEvents,
            Function<Long, OptionalInt> otidToTtidConverter,
            Function<Integer, Optional<ReadonlyEventInterface>> firstEventExtractor,
            Function<Integer, Optional<ReadonlyEventInterface>> lastEventExtractor) {
        this.interThreadSyncEvents = interThreadSyncEvents;
        this.otidToTtidConverter = otidToTtidConverter;
        this.firstEventExtractor = firstEventExtractor;
        this.lastEventExtractor = lastEventExtractor;
    }

    @Override
    public ModelRestrict createRestrict() {
        ImmutableList.Builder<ModelRestrict> restricts = new ImmutableList.Builder<>();
        processSyncEvents(
                (event, first) -> restricts.add(new Before(event, first)),
                (last, event) -> restricts.add(new Before(last, event)));
        return new And(restricts.build());
    }

    @Override
    public void addToMhbClosure(TransitiveClosure.Builder mhbClosureBuilder) {
        processSyncEvents(
                mhbClosureBuilder::addRelation,
                mhbClosureBuilder::addRelation);
    }

    private void processSyncEvents(
            BiConsumer<ReadonlyEventInterface, ReadonlyEventInterface> firstEventConsumer,
            BiConsumer<ReadonlyEventInterface, ReadonlyEventInterface> lastEventConsumer) {
        interThreadSyncEvents.forEach(event -> {
            if (event.isStart()) {
                OptionalInt maybeTtid = otidToTtidConverter.apply(event.getSyncedThreadId());
                if (!maybeTtid.isPresent()) {
                    return;
                }
                firstEventExtractor.apply(maybeTtid.getAsInt())
                        .ifPresent(firstEvent -> firstEventConsumer.accept(event, firstEvent));
            } else if (event.isJoin()) {
                OptionalInt maybeTtid = otidToTtidConverter.apply(event.getSyncedThreadId());
                if (!maybeTtid.isPresent()) {
                    return;
                }
                lastEventExtractor.apply(maybeTtid.getAsInt())
                        .ifPresent(lastEvent -> lastEventConsumer.accept(lastEvent, event));
            }
        });
    }
}
