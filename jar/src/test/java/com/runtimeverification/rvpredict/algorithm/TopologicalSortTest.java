package com.runtimeverification.rvpredict.algorithm;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.assertException;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.containsInOrder;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.hasSize;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isEmpty;

public class TopologicalSortTest {
    @Test
    public void sortsEmptyList() throws TopologicalSort.TopologicalSortingException {
        List<Integer> sorted = new ArrayList<>();
        TopologicalSort.sortFromParentLists(Collections.emptyMap(), sorted);
        Assert.assertThat(sorted, isEmpty());
    }

    @Test
    public void sortsTwoElements() throws TopologicalSort.TopologicalSortingException {
        List<Integer> sorted = new ArrayList<>();
        TopologicalSort.sortFromParentLists(
                ImmutableMap.of(
                        1, Collections.singletonList(2),
                        2, Collections.emptyList()),
                sorted);
        Assert.assertThat(sorted, containsInOrder(2, 1));
    }

    @Test
    public void sortsMultipleElements() throws TopologicalSort.TopologicalSortingException {
        List<Integer> sorted = new ArrayList<>();
        TopologicalSort.sortFromParentLists(
                ImmutableMap.of(
                        1, Collections.singletonList(2),
                        2, ImmutableList.of(3, 4),
                        3, Collections.singletonList(4),
                        4, Collections.emptyList()),
                sorted);
        Assert.assertThat(sorted, containsInOrder(4, 3, 2, 1));
    }

    @Test
    public void sortsWithAmbiguousOrder() throws TopologicalSort.TopologicalSortingException {
        List<Integer> sorted = new ArrayList<>();
        TopologicalSort.sortFromParentLists(
                ImmutableMap.of(
                        1, Collections.singletonList(2),
                        2, Collections.emptyList(),
                        3, Collections.singletonList(4),
                        4, Collections.emptyList()),
                sorted);
        Assert.assertThat(sorted, hasSize(4));
    }

    @Test
    public void exceptionForCycle() {
        List<Integer> sorted = new ArrayList<>();
        assertException(
                TopologicalSort.TopologicalSortingException.class,
                () -> TopologicalSort.sortFromParentLists(
                        ImmutableMap.of(
                                1, Collections.singletonList(2),
                                2, Collections.singletonList(3),
                                3, Collections.singletonList(1)),
                        sorted));
    }
}
