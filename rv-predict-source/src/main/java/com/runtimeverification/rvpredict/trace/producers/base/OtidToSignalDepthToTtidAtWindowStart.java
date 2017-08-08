package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public class OtidToSignalDepthToTtidAtWindowStart extends ComputingProducer {
    private final TtidSetDifference threadsRunningAtWindowStart;
    private final ThreadInfosComponent threadInfosComponent;

    private final Map<Long, Map<Integer, Integer>> otidToSignalDepthToTtidAtWindowStart = new HashMap<>();

    public OtidToSignalDepthToTtidAtWindowStart(
            ComputingProducerWrapper<TtidSetDifference> threadsRunningAtWindowStart,
            ComputingProducerWrapper<ThreadInfosComponent> threadInfosComponent) {
        this.threadsRunningAtWindowStart = threadsRunningAtWindowStart.getAndRegister(this);
        this.threadInfosComponent = threadInfosComponent.getAndRegister(this);
    }

    @Override
    protected void compute() {
        otidToSignalDepthToTtidAtWindowStart.clear();

        threadsRunningAtWindowStart.getTtids().forEach(ttid ->
                otidToSignalDepthToTtidAtWindowStart
                        .computeIfAbsent(
                                threadInfosComponent.getOriginalThreadIdForTraceThreadId(ttid),
                                k -> new HashMap<>())
                        .put(threadInfosComponent.getSignalDepth(ttid), ttid));
    }

    public OptionalInt getTtid(long otid, int signalDepth) {
        Integer ttid = otidToSignalDepthToTtidAtWindowStart
                .getOrDefault(otid, Collections.emptyMap())
                .get(signalDepth);
        return ttid == null ? OptionalInt.empty() : OptionalInt.of(ttid);
    }
}
