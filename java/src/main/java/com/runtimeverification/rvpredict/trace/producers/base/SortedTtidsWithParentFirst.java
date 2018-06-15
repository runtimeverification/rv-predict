package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.algorithm.TopologicalSort;
import com.runtimeverification.rvpredict.producerframework.ComputingProducer;
import com.runtimeverification.rvpredict.producerframework.ComputingProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.Producer;
import com.runtimeverification.rvpredict.producerframework.ProducerState;
import com.runtimeverification.rvpredict.trace.ThreadInfo;
import com.runtimeverification.rvpredict.trace.ThreadType;
import com.runtimeverification.rvpredict.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

public class SortedTtidsWithParentFirst extends ComputingProducer<SortedTtidsWithParentFirst.State> {
    private final TtidSet ttidsForCurrentWindow;
    private final ThreadInfosComponent threadInfosComponent;

    protected static class State implements ProducerState {
        private final List<Integer> sortedTtidsWithParentFirst = new ArrayList<>();

        @Override
        public void reset() {
            sortedTtidsWithParentFirst.clear();
        }
    }

    public <T extends Producer & TtidSet> SortedTtidsWithParentFirst(
            ComputingProducerWrapper<T> ttidsForCurrentWindow,
            ComputingProducerWrapper<ThreadInfosComponent> threadInfosComponent) {
        super(new State());
        this.ttidsForCurrentWindow = ttidsForCurrentWindow.getAndRegister(this);
        this.threadInfosComponent = threadInfosComponent.getAndRegister(this);
    }

    @Override
    protected void compute() {
        sortNormalThreads();
        addSignals();
    }

    private void sortNormalThreads() {
        // There is at most one parent, but I'm using a list for the value to make this
        // work with topological sorting.
        Map<Integer, List<Integer>> ttidToParent = ttidsForCurrentWindow.getTtids().stream()
                .map(threadInfosComponent::getThreadInfo)
                .filter(threadInfo -> threadInfo.getThreadType() == ThreadType.THREAD)
                .collect(Collectors.toMap(
                        ThreadInfo::getId,
                        threadInfo -> CollectionUtils.toList(threadInfo.getParentTtid())));
        Set<Integer> relevantTtids = new HashSet<>(ttidToParent.keySet());
        relevantTtids.forEach(ttid -> fillMissingParents(ttid, ttidToParent));
        try {
            TopologicalSort.sortFromParentLists(ttidToParent, getState().sortedTtidsWithParentFirst);
        } catch (TopologicalSort.TopologicalSortingException e) {
            throw new IllegalStateException(e);
        }
        getState().sortedTtidsWithParentFirst.removeIf(ttid -> !relevantTtids.contains(ttid));
    }

    private void fillMissingParents(Integer ttid, Map<Integer, List<Integer>> ttidToParent) {
        boolean firstIteration = true;
        while (true) {
            OptionalInt maybeParent = threadInfosComponent.getParentThread(ttid);
            if (!maybeParent.isPresent()) {
                return;
            }
            int parent = maybeParent.getAsInt();
            if (firstIteration) {
                firstIteration = false;
                assert ttidToParent.containsKey(ttid);
                assert ttidToParent.get(ttid).get(0) == parent;
            } else {
                ttidToParent.put(ttid, Collections.singletonList(parent));
            }
            if (ttidToParent.containsKey(parent)) {
                return;
            }
            ttid = parent;
        }
    }

    private void addSignals() {
        List<ThreadInfo> signals = ttidsForCurrentWindow.getTtids().stream()
                .map(threadInfosComponent::getThreadInfo)
                .filter(threadInfo -> threadInfo.getThreadType() == ThreadType.SIGNAL)
                .sorted(Comparator.comparingInt(ThreadInfo::getSignalDepth))
                .collect(Collectors.toList());
        for (ThreadInfo threadInfo : signals) {
            getState().sortedTtidsWithParentFirst.add(threadInfo.getId());
        }
    }

    public List<Integer> getTtids() {
        return getState().sortedTtidsWithParentFirst;
    }
}
