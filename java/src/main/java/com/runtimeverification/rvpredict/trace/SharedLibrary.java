package com.runtimeverification.rvpredict.trace;

import com.google.common.collect.ImmutableList;

/**
 * A shared library as described in a trace.
 */
public class SharedLibrary {
    private final String name;
    private final ImmutableList<Segment> segments;

    public SharedLibrary(String name, ImmutableList<Segment> segments) {
        this.name = name;
        this.segments = segments;
    }

    public String getName() {
        return name;
    }

    public boolean containsAddress(long address) {
        for (Segment segment : segments) {
            if (segment.containsAddress(address)) {
                return true;
            }
        }
        return false;
    }

    public static class Segment {
        private final long sharedLibrarySegmentStart;
        private final long sharedLibrarySegmentEnd;

        public Segment(long sharedLibrarySegmentStart, long sharedLibrarySegmentEnd) {
            this.sharedLibrarySegmentStart = sharedLibrarySegmentStart;
            this.sharedLibrarySegmentEnd = sharedLibrarySegmentEnd;
        }

        public boolean containsAddress(long address) {
            return sharedLibrarySegmentStart <= address && address < sharedLibrarySegmentEnd;
        }
    }
}
