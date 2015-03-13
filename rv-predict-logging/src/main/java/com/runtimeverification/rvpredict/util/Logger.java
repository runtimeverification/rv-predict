package com.runtimeverification.rvpredict.util;

import com.google.common.base.Strings;
import com.runtimeverification.rvpredict.config.Configuration;

/**
 * @author TraianSF
 * @author YilongL
 */
public class Logger {

    private static final int WIDTH = 75;
    private static final String DASH = "-";

    public static String center(String msg) {
        int fillWidth = WIDTH - msg.length();
        return "\n" + Strings.repeat(DASH, fillWidth / 2) + msg
                + Strings.repeat(DASH, (fillWidth + 1) / 2);
    }

    public void reportCenter(String msg, MSGTYPE type) {
        report(center(msg), type);
    }

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
