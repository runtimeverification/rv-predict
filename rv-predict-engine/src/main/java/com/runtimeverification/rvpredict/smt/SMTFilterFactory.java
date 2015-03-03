package com.runtimeverification.rvpredict.smt;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.smt.visitors.SMTLib1Filter;

/**
 * Factory generating SMT filters based on the user configuration options
 */
public class SMTFilterFactory {
    public static SMTFilter getSMTFilter(Configuration config) {
        return new SMTLib1Filter();
    }
}
