package com.runtimeverification.rvpredict.trace;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

public class SharedLibraryTest {
    @Test
    public void noAddressFoundOneLibraryWithoutSegments() {
        SharedLibrary sharedLibrary = new SharedLibrary("test", ImmutableList.of());
        Assert.assertFalse(sharedLibrary.containsAddress(11));
    }

    @Test
    public void addressFoundOneLibraryWithSegment() {
        SharedLibrary.Segment segment = new SharedLibrary.Segment(10, 20);
        SharedLibrary sharedLibrary = new SharedLibrary("test", ImmutableList.of(segment));
        Assert.assertTrue(sharedLibrary.containsAddress(11));
        Assert.assertFalse(sharedLibrary.containsAddress(21));
        Assert.assertTrue(segment.containsAddress(11));
        Assert.assertFalse(segment.containsAddress(21));
    }
}
