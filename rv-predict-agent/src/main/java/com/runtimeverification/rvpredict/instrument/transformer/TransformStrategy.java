package com.runtimeverification.rvpredict.instrument.transformer;

/**
 * Transformation strategy.
 *
 * @author YilongL
 */
public enum TransformStrategy {
    FULL, INTERCEPTION;

    public boolean logMemoryAccess() {
        return this == FULL;
    }

    public boolean replaceStandardLibraryClass() {
        return this == FULL;
    }

    public boolean logMonitorEvent() {
        return this == FULL;
    }

    public boolean logCallStackEvent() {
        return this == FULL;
    }

    public boolean interceptMethodCall() {
        return this == FULL || this == INTERCEPTION;
    }

}
