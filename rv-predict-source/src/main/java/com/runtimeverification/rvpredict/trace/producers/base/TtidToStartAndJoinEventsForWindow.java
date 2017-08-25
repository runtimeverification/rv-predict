package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class TtidToStartAndJoinEventsForWindow extends ComputingProducer<TtidToStartAndJoinEventsForWindow.State> {
    private final InterThreadSyncEvents interThreadSyncEvents;
    private final OtidToMainTtid otidToMainTtid;

    protected static class State implements ProducerState {
        private final Map<Integer, ReadonlyEventInterface> ttidToStartEvent = new HashMap<>();
        private final Map<Integer, ReadonlyEventInterface> ttidToJoinEvent = new HashMap<>();

        @Override
        public void reset() {
            ttidToStartEvent.clear();
            ttidToJoinEvent.clear();
        }
    }

    public TtidToStartAndJoinEventsForWindow(
            ComputingProducerWrapper<InterThreadSyncEvents> interThreadSyncEvents,
            ComputingProducerWrapper<OtidToMainTtid> otidToMainTtid) {
        super(new State());
        this.interThreadSyncEvents = interThreadSyncEvents.getAndRegister(this);
        this.otidToMainTtid = otidToMainTtid.getAndRegister(this);
    }

    public Optional<ReadonlyEventInterface> getStartEvent(int ttid) {
        return Optional.ofNullable(getState().ttidToStartEvent.get(ttid));
    }

    public Optional<ReadonlyEventInterface> getJoinEvent(int ttid) {
        return Optional.ofNullable(getState().ttidToJoinEvent.get(ttid));
    }

    @Override
    protected void compute() {
        interThreadSyncEvents.getSyncEvents().forEach(event -> {
            if (event.isStart()) {
                OptionalInt maybeTtid = otidToMainTtid.getTtid(event.getSyncedThreadId());
                if (!maybeTtid.isPresent()) {
                    // Should have been processed at raw trace build time.
                    throw new IllegalStateException("Thread id not found for: " + event.getSyncedThreadId());
                }
                getState().ttidToStartEvent.put(maybeTtid.getAsInt(), event);
            } else if (event.isJoin()) {
                OptionalInt maybeTtid = otidToMainTtid.getTtid(event.getSyncedThreadId());
                if (!maybeTtid.isPresent()) {
                    // Should have been processed at raw trace build time.
                    throw new IllegalStateException("Thread id not found for: " + event.getSyncedThreadId());
                }
                getState().ttidToJoinEvent.put(maybeTtid.getAsInt(), event);
            }
        });
    }
}
