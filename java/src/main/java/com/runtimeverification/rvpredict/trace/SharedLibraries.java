package com.runtimeverification.rvpredict.trace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Collection of shared libraries used in the trace.
 */
public class SharedLibraries {
    private final List<SharedLibrary> sharedLibraries = new ArrayList<>();

    public void addAll(Collection<SharedLibrary> libraries) {
        sharedLibraries.addAll(libraries);
    }

    /**
     * Given an addres, returns the name of the shared library that contains that address (if any).
     */
    public Optional<String> getSharedLibraryNameFromAddress(long address) {
        Optional<String> name = Optional.empty();
        // This could be optimized (e.g. one could do a binary search over the segments), but I (Virgil)
        // am assuming that the number of library segments is relatively small and that this method is
        // called rarely.
        for (SharedLibrary library : sharedLibraries) {
            if (library.containsAddress(address)) {
                assert !name.isPresent();
                name = Optional.of(library.getName());
            }
        }
        return name;
    }
}
