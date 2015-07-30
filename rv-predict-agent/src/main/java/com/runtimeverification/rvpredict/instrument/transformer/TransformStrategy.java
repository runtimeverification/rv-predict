package com.runtimeverification.rvpredict.instrument.transformer;

/**
 * Transformation strategy.
 *
 * @author YilongL
 */
public enum TransformStrategy {
    FULL,

    THREAD;

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

    public boolean interceptMethodCall(String methodName) {
        return this == FULL || (this == THREAD && methodName.equals("start0"));
    }

}
