package com.runtimeverification.rvpredict.util;

/**
 * Class for holding various constant items
 * @author TraianSF
 */
public interface Constants {

    String RVPREDICT_PKG_PREFIX = "com/runtimeverification/rvpredict/";

    String RVPREDICT_RUNTIME_PKG_PREFIX = "com/runtimeverification/rvpredict/runtime/";

    byte JUC_LOCK_C = 0;

    byte MONITOR_C = 42;

    byte ATOMIC_LOCK_C = 43;

    long SIGNAL_LOCK_C = 44;

    long INVALID_SIGNAL = -1L;

    long INVALID_THREAD_ID = -1L;

    long INVALID_ADDRESS = -1L;

    int INVALID_TTID = -1;
}
