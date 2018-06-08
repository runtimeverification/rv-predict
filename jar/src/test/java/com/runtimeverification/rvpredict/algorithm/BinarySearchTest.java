package com.runtimeverification.rvpredict.algorithm;

import com.runtimeverification.rvpredict.testutils.MoreAsserts;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class BinarySearchTest {
    @Test
    public void doesNotFindKeyInEmptyList() {
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Collections.<Integer>emptyList(), i -> i, 1),
                MoreAsserts.isAbsentInt());
    }

    @Test
    public void doesNotFindKeyInListWithLargerElements() {
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Collections.singletonList(2), i -> i, 1),
                MoreAsserts.isAbsentInt());
    }

    @Test
    public void findsKeyInOneElementList() {
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Collections.singletonList(2), i -> i, 3),
                MoreAsserts.isPresentWithIntValue(0));
    }

    @Test
    public void all7ElementListSearches() {
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 1),
                MoreAsserts.isAbsentInt());
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 2),
                MoreAsserts.isPresentWithIntValue(0));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 3),
                MoreAsserts.isPresentWithIntValue(0));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 4),
                MoreAsserts.isPresentWithIntValue(1));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 5),
                MoreAsserts.isPresentWithIntValue(1));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 6),
                MoreAsserts.isPresentWithIntValue(2));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 7),
                MoreAsserts.isPresentWithIntValue(2));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 8),
                MoreAsserts.isPresentWithIntValue(3));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 9),
                MoreAsserts.isPresentWithIntValue(3));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 10),
                MoreAsserts.isPresentWithIntValue(4));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 11),
                MoreAsserts.isPresentWithIntValue(4));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 12),
                MoreAsserts.isPresentWithIntValue(5));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 13),
                MoreAsserts.isPresentWithIntValue(5));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 14),
                MoreAsserts.isPresentWithIntValue(6));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14), i -> i, 15),
                MoreAsserts.isPresentWithIntValue(6));
    }

    @Test
    public void all8ElementListSearches() {
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 1),
                MoreAsserts.isAbsentInt());
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 2),
                MoreAsserts.isPresentWithIntValue(0));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 3),
                MoreAsserts.isPresentWithIntValue(0));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 4),
                MoreAsserts.isPresentWithIntValue(1));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 5),
                MoreAsserts.isPresentWithIntValue(1));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 6),
                MoreAsserts.isPresentWithIntValue(2));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 7),
                MoreAsserts.isPresentWithIntValue(2));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 8),
                MoreAsserts.isPresentWithIntValue(3));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 9),
                MoreAsserts.isPresentWithIntValue(3));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 10),
                MoreAsserts.isPresentWithIntValue(4));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 11),
                MoreAsserts.isPresentWithIntValue(4));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 12),
                MoreAsserts.isPresentWithIntValue(5));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 13),
                MoreAsserts.isPresentWithIntValue(5));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 14),
                MoreAsserts.isPresentWithIntValue(6));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 15),
                MoreAsserts.isPresentWithIntValue(6));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 16),
                MoreAsserts.isPresentWithIntValue(7));
        Assert.assertThat(
                BinarySearch.getIndexLessOrEqual(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16), i -> i, 17),
                MoreAsserts.isPresentWithIntValue(7));
    }
}
