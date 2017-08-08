package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;

import java.util.ArrayList;
import java.util.List;

public class InterThreadSyncEvents extends ComputingProducer {
    private final RawTraces rawTraces;

    private final List<ReadonlyEventInterface> syncEvents = new ArrayList<>();

    public InterThreadSyncEvents(ComputingProducerWrapper<RawTraces> rawTraces) {
        this.rawTraces = rawTraces.getAndRegister(this);
    }

    public List<ReadonlyEventInterface> getSyncEvents() {
        return syncEvents;
    }

    @Override
    protected void compute() {
        syncEvents.clear();

        rawTraces.getTraces().forEach(rawTrace -> {
            for (int i = 0; i < rawTrace.size(); i++) {
                ReadonlyEventInterface event = rawTrace.event(i);
                if (event.isStart() || event.isJoin()) {
                    syncEvents.add(event);
                }
            }
        });
    }
}
