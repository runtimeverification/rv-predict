package com.runtimeverification.rvpredict.trace.producers.base;

import com.google.common.collect.Lists;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.producers.signals.ThreadsWhereSignalIsEnabled;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

public class PreviousSignalsTraceMerger
        extends ComputingProducer<PreviousSignalsTraceMerger.State>
        implements RawTracesCollection {
    private final RawTraces currentWindowRawTraces;
    private final RawTraces previousSignalsRawTraces;
    private final ThreadsWhereSignalIsEnabled threadsWhereSignalIsEnabled;
    private final DesiredThreadCountForSignal desiredThreadCountForSignal;

    public PreviousSignalsTraceMerger(
            ComputingProducerWrapper<RawTraces> currentWindowRawTraces,
            ComputingProducerWrapper<RawTraces> previousSignalsRawTraces,
            ComputingProducerWrapper<ThreadsWhereSignalIsEnabled> threadsWhereSignalIsEnabled,
            ComputingProducerWrapper<DesiredThreadCountForSignal> desiredThreadCountForSignal) {
        super(new State());

        this.currentWindowRawTraces = currentWindowRawTraces.getAndRegister(this);
        this.previousSignalsRawTraces = previousSignalsRawTraces.getAndRegister(this);
        this.threadsWhereSignalIsEnabled = threadsWhereSignalIsEnabled.getAndRegister(this);
        this.desiredThreadCountForSignal = desiredThreadCountForSignal.getAndRegister(this);
    }

    @Override
    protected void compute() {
        getState().traces.addAll(currentWindowRawTraces.getTraces());
        Map<Long, Integer> threadCountForSignal = new HashMap<>();
        for (RawTrace trace : currentWindowRawTraces.getTraces()) {
            OptionalLong maybeSignalNumber = trace.getThreadInfo().getSignalNumber();
            if (maybeSignalNumber.isPresent()) {
                threadCountForSignal.compute(maybeSignalNumber.getAsLong(), (k, v) -> v == null ? 1 : v + 1);
            }
        }

        int maxCount = desiredThreadCountForSignal.getCount();
        for (RawTrace trace : Lists.reverse(previousSignalsRawTraces.getTraces())) {
            OptionalLong maybeSignalNumber = trace.getThreadInfo().getSignalNumber();
            assert (maybeSignalNumber.isPresent());
            long signalNumber = maybeSignalNumber.getAsLong();

            if (threadsWhereSignalIsEnabled.threadsForSignal(signalNumber).isEmpty()) {
                continue;
            }

            int count = threadCountForSignal.computeIfAbsent(signalNumber, k -> 0);
            if (count >= maxCount) {
                continue;
            }

            getState().traces.add(trace);
            threadCountForSignal.put(signalNumber, count + 1);
        }
    }

    public List<RawTrace> getTraces() {
        return getState().traces;
    }

    protected static class State implements ProducerState {
        private List<RawTrace> traces = new ArrayList<>();

        @Override
        public void reset() {
            traces.clear();
        }
    }
}
