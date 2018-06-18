package com.runtimeverification.rvpredict.trace.producers.base;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.algorithm.BinarySearch;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.Producer;
import com.runtimeverification.rvpredict.producerframework.ProducerState;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class StackTraces extends ComputingProducer<StackTraces.State> {
    private final RawTracesCollection rawTraces;
    private final StackTracesLeaf startTraces;
    public <T extends Producer & RawTracesCollection>StackTraces(
            ComputingProducerWrapper<T> rawTraces,
            ComputingProducerWrapper<StackTracesLeaf> startTraces) {
        super(new State());
        this.rawTraces = rawTraces.getAndRegister(this);
        this.startTraces = startTraces.getAndRegister(this);
    }

    @Override
    protected void compute() {
        rawTraces.getTraces().forEach(rawTrace -> {
            Deque<StackTraceAfterEvent> stackTrace = new ArrayDeque<>();
            ImmutableList.Builder<StackTraceAfterEvent> stackTraces = ImmutableList.builder();

            Collection<ReadonlyEventInterface> existingStackTrace =
                    startTraces.getStackTraces().get(rawTrace.getThreadInfo().getId());
            if (existingStackTrace == null) {
                existingStackTrace = ImmutableList.of();
            }
            existingStackTrace.forEach(e -> addToStack(e, stackTrace, stackTraces));

            for (int i = 0; i < rawTrace.size(); i++) {
                ReadonlyEventInterface e = rawTrace.event(i);
                if (e.getType() == EventType.INVOKE_METHOD) {
                    addToStack(e, stackTrace, stackTraces);
                } else if (e.getType() == EventType.FINISH_METHOD) {
                    if (!stackTrace.isEmpty()) {
                        stackTrace.removeLast();
                    }
                    stackTraces.add(new StackTraceAfterEvent(
                            e, Optional.ofNullable(stackTrace.peekLast())));
                }
            }
            getState().ttidToStackTrace.put(rawTrace.getThreadInfo().getId(), stackTraces.build());
        });
    }

    private void addToStack(ReadonlyEventInterface e, Deque<StackTraceAfterEvent> stackTrace, ImmutableList.Builder<StackTraceAfterEvent> stackTraces) {
        StackTraceAfterEvent stack =
                new StackTraceAfterEvent(
                        e, Optional.ofNullable(stackTrace.peekLast()));
        stackTrace.addLast(stack);
        stackTraces.add(stack);
    }

    public ImmutableList.Builder<ReadonlyEventInterface> getStackTraceAfterEventBuilder(int ttid, long eventId) {
        return getStackTraceAfterEventInternal(ttid, eventId).map(
                StackTraceAfterEvent::asListBuilder).orElse(ImmutableList.builder());
    }

    private Optional<StackTraceAfterEvent> getStackTraceAfterEventInternal(int ttid, long eventId) {
        List<StackTraceAfterEvent> stacks = getState().ttidToStackTrace.get(ttid);
        if (stacks == null) {
            return Optional.empty();
        }
        OptionalInt maybeIndex = BinarySearch.getIndexLessOrEqual(stacks, StackTraceAfterEvent::getEventId, eventId);
        if (!maybeIndex.isPresent()) {
            return Optional.empty();
        }
        int index = maybeIndex.getAsInt();
        return Optional.of(stacks.get(index));
    }

    private class StackTraceAfterEvent {
        private final ReadonlyEventInterface event;
        private final Optional<StackTraceAfterEvent> previousEvent;

        private StackTraceAfterEvent(
                ReadonlyEventInterface event,
                Optional<StackTraceAfterEvent> previousEvent) {
            this.event = event;
            this.previousEvent = previousEvent;
            assert !previousEvent.isPresent() || previousEvent.get().getEventId() < event.getEventId();
        }

        private long getEventId() {
            return event.getEventId();
        }

        private ImmutableList.Builder<ReadonlyEventInterface> asListBuilder() {
            /*
              if (previousEvent.isPresent()) {

                  return previousEvent.get().asListBuilder().add(event);
              }
              return ImmutableList.<ReadonlyEventInterface>builder().add(event);
            */
            ImmutableList.Builder<ReadonlyEventInterface> parentBuilder =
                    previousEvent.map(StackTraceAfterEvent::asListBuilder).orElse(ImmutableList.builder());
            if (event.getType() == EventType.INVOKE_METHOD) {
                return parentBuilder.add(event);
            }
            return parentBuilder;
        }
    }

    protected static class State implements ProducerState {
        Map<Integer, List<StackTraceAfterEvent>> ttidToStackTrace = new HashMap<>();

        @Override
        public void reset() {
            ttidToStackTrace.clear();
        }
    }
}
