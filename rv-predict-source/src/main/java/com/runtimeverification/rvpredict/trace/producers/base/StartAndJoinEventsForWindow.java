package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class StartAndJoinEventsForWindow extends ComputingProducer {
    private final InterThreadSyncEvents interThreadSyncEvents;
    private final OtidToMainTtid otidToMainTtid;

    private final Map<Integer, ReadonlyEventInterface> ttidToStartEvent = new HashMap<>();
    private final Map<Integer, ReadonlyEventInterface> ttidToJoinEvent = new HashMap<>();

    public StartAndJoinEventsForWindow(
            ComputingProducerWrapper<InterThreadSyncEvents> interThreadSyncEvents,
            ComputingProducerWrapper<OtidToMainTtid> otidToMainTtid) {
        this.interThreadSyncEvents = interThreadSyncEvents.getAndRegister(this);
        this.otidToMainTtid = otidToMainTtid.getAndRegister(this);
    }

    public Optional<ReadonlyEventInterface> getStartEvent(int ttid) {
        return Optional.ofNullable(ttidToStartEvent.get(ttid));
    }

    public Optional<ReadonlyEventInterface> getJoinEvent(int ttid) {
        return Optional.ofNullable(ttidToJoinEvent.get(ttid));
    }

    @Override
    protected void compute() {
        ttidToStartEvent.clear();
        ttidToJoinEvent.clear();

        interThreadSyncEvents.getSyncEvents().forEach(event -> {
            if (event.isStart()) {
                OptionalInt maybeTtid = otidToMainTtid.getTtid(event.getSyncedThreadId());
                if (maybeTtid.isPresent()) {
                    ttidToStartEvent.put(maybeTtid.getAsInt(), event);
                }
            } else if (event.isJoin()) {
                OptionalInt maybeTtid = otidToMainTtid.getTtid(event.getSyncedThreadId());
                if (maybeTtid.isPresent()) {
                    ttidToJoinEvent.put(maybeTtid.getAsInt(), event);
                }
            }
        });
    }
}
