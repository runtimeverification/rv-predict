package com.runtimeverification.rvpredict.trace.producers.base;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.trace.SharedLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes the shared libraries found in the trace. Assumes that a library fully fits in a trace window.
 */
public class SharedLibraries extends ComputingProducer<SharedLibraries.State> {
    private final RawTraces rawTraces;

    public SharedLibraries(ComputingProducerWrapper<RawTraces> rawTraces) {
        super(new State());
        this.rawTraces = rawTraces.getAndRegister(this);
    }

    @Override
    protected void compute() {
        Map<Long, String> libraryNames = new HashMap<>();
        Map<Long, ImmutableList.Builder<SharedLibrary.Segment>> librarySegments = new HashMap<>();
        rawTraces.getTraces().forEach(rawTrace -> {
            for (int i = 0; i < rawTrace.size(); i++) {
                ReadonlyEventInterface event = rawTrace.event(i);
                if (event.getType() == EventType.SHARED_LIBRARY) {
                    libraryNames.put(event.getSharedLibraryId(), event.getSharedLibraryName());
                } else if (event.getType() == EventType.SHARED_LIBRARY_SEGMENT) {
                    librarySegments
                            .computeIfAbsent(event.getSharedLibraryId(), k -> new ImmutableList.Builder<>())
                            .add(new SharedLibrary.Segment(
                                    event.getSharedLibrarySegmentStart(), event.getSharedLibrarySegmentEnd()));
                }
            }
        });
        libraryNames.forEach((id, name) ->
                getState().sharedLibraries.add(new SharedLibrary(
                        name,
                        librarySegments
                                .getOrDefault(id, new ImmutableList.Builder<>())
                                .build())));
        for (Long id : librarySegments.keySet()) {
            if (!libraryNames.containsKey(id)) {
                throw new IllegalStateException("Library segment without library name for id: " + id + ".");
            }
        }
    }

    public Collection<SharedLibrary> getLibraries() {
        return getState().sharedLibraries;
    }

    public static class State implements ProducerState {
        List<SharedLibrary> sharedLibraries = new ArrayList<>();
        @Override
        public void reset() {
            sharedLibraries.clear();
        }
    }
}
