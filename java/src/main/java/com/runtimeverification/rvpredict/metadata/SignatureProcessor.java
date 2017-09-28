package com.runtimeverification.rvpredict.metadata;

import com.runtimeverification.rvpredict.util.Constants;

/**
 * Class Gathering information from the location signatures
 * and using it to simplify the reports.
 *
 * @author TraianSF
 */
public class SignatureProcessor {
    private static final String RVPREDICT_RT_PKG_PREFIX = Constants.RVPREDICT_RUNTIME_PKG_PREFIX
            .replace('/', '.');

    /**
     * Simplifies the given string using the internal information computed so far.
     * @param s the string whose locations should be be simplified
     * @return the simplified string obtained from {@code s}
     */
    public String simplify(String s) {
        return s.replace(RVPREDICT_RT_PKG_PREFIX, "");
    }

    /**
     * Process given signature and update internal state accordingly.
     * @param locSig location signature to be processed.
     */
    public void process(String locSig) {}

    /**
     * Reset the state of the processor.
     */
    public void reset() {}
}
