package com.runtimeverification.rvpredict.trace;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.compact.readers.SharedLibrarySegment;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class SharedLibrariesTest {
    @Test
    public void noAddressFoundWithoutLibraries() {
        SharedLibraries sharedLibraries = new SharedLibraries();
        Assert.assertFalse(sharedLibraries.getSharedLibraryNameFromAddress(11).isPresent());
    }

    @Test
    public void noAddressFoundOneLibraryWithoutSegments() {
        SharedLibraries sharedLibraries = new SharedLibraries();
        SharedLibrary sharedLibrary = new SharedLibrary("test", ImmutableList.of());
        sharedLibraries.addAll(Collections.singletonList(sharedLibrary));
        Assert.assertFalse(sharedLibraries.getSharedLibraryNameFromAddress(11).isPresent());
    }

    @Test
    public void addressFoundOneLibraryWithSegment() {
        SharedLibraries sharedLibraries = new SharedLibraries();
        SharedLibrary.Segment segment = new SharedLibrary.Segment(10, 20);
        SharedLibrary sharedLibrary = new SharedLibrary("test", ImmutableList.of(segment));
        sharedLibraries.addAll(Collections.singletonList(sharedLibrary));
        Assert.assertTrue(sharedLibraries.getSharedLibraryNameFromAddress(11).isPresent());
        Assert.assertEquals("test", sharedLibraries.getSharedLibraryNameFromAddress(11).get());
        Assert.assertFalse(sharedLibraries.getSharedLibraryNameFromAddress(21).isPresent());
    }
}
