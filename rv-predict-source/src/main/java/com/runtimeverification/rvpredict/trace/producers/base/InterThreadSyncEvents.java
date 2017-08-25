package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerState;

import java.util.ArrayList;
import java.util.List;

public class InterThreadSyncEvents extends ComputingProducer<InterThreadSyncEvents.State> {
    private final RawTraces rawTraces;

    protected static class State implements ProducerState {
        private final List<ReadonlyEventInterface> syncEvents = new ArrayList<>();

        @Override
        public void reset() {
            syncEvents.clear();
        }
    }

    public InterThreadSyncEvents(ComputingProducerWrapper<RawTraces> rawTraces) {
        super(new State());
        this.rawTraces = rawTraces.getAndRegister(this);
    }

    public List<ReadonlyEventInterface> getSyncEvents() {
        return getState().syncEvents;
    }

    @Override
    protected void compute() {
        rawTraces.getTraces().forEach(rawTrace -> {
            for (int i = 0; i < rawTrace.size(); i++) {
                ReadonlyEventInterface event = rawTrace.event(i);
                if (event.isStart() || event.isJoin()) {
                    getState().syncEvents.add(event);
                }
            }
        });
    }
}
