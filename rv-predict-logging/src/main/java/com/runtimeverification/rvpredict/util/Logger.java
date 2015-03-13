package com.runtimeverification.rvpredict.util;

import com.runtimeverification.rvpredict.config.Configuration;

/**
 * @author TraianSF
 * @author YilongL
 */
public class Logger {

    public synchronized void report(String msg, MSGTYPE type) {
        switch (type) {
        case ERROR:
            System.err.println(msg);
            break;
        case REAL:
        case INFO:
            System.out.println(msg);
            break;
        case STATISTICS:
        case VERBOSE:
            if (Configuration.verbose) {
                System.out.println(msg);
            }
            break;
        case POTENTIAL:
            break;
        default:
            break;
        }
    }

    public enum MSGTYPE {
        REAL, POTENTIAL, STATISTICS, INFO, VERBOSE, ERROR
    }
}
