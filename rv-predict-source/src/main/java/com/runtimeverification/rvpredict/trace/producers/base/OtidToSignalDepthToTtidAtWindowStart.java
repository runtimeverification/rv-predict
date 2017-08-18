package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public class OtidToSignalDepthToTtidAtWindowStart
        extends ComputingProducer<OtidToSignalDepthToTtidAtWindowStart.State> {
    private final TtidSetDifference threadsRunningAtWindowStart;
    private final ThreadInfosComponent threadInfosComponent;

    protected static class State implements ProducerState {
        private final Map<Long, Map<Integer, Integer>> otidToSignalDepthToTtidAtWindowStart = new HashMap<>();

        @Override
        public void reset() {
            otidToSignalDepthToTtidAtWindowStart.clear();
        }
    }

    public OtidToSignalDepthToTtidAtWindowStart(
            ComputingProducerWrapper<TtidSetDifference> threadsRunningAtWindowStart,
            ComputingProducerWrapper<ThreadInfosComponent> threadInfosComponent) {
        super(new State());
        this.threadsRunningAtWindowStart = threadsRunningAtWindowStart.getAndRegister(this);
        this.threadInfosComponent = threadInfosComponent.getAndRegister(this);
    }

    @Override
    protected void compute() {
        threadsRunningAtWindowStart.getTtids().forEach(ttid ->
                getState().otidToSignalDepthToTtidAtWindowStart
                        .computeIfAbsent(
                                threadInfosComponent.getOriginalThreadIdForTraceThreadId(ttid),
                                k -> new HashMap<>())
                        .put(threadInfosComponent.getSignalDepth(ttid), ttid));
    }

    public OptionalInt getTtid(long otid, int signalDepth) {
        Integer ttid = getState().otidToSignalDepthToTtidAtWindowStart
                .getOrDefault(otid, Collections.emptyMap())
                .get(signalDepth);
        return ttid == null ? OptionalInt.empty() : OptionalInt.of(ttid);
    }
}
