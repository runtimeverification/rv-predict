package com.runtimeverification.rvpredict.util;

import com.google.common.base.Strings;

/**
 * @author TraianSF
 * @author YilongL
 */
public class Logger {

    private static final int    WIDTH   =   75;
    private static final String DASH    =   "-";

    public void reportPhase(String phaseMsg) {
        report(center(phaseMsg), MSGTYPE.INFO);
    }

    private static String center(String msg) {
        int fillWidth = WIDTH - msg.length();
        return "\n" + Strings.repeat(DASH, fillWidth / 2) + msg
                + Strings.repeat(DASH, (fillWidth + 1) / 2);
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
        default:
            break;
        }
    }

    public enum MSGTYPE {
        REAL, INFO, ERROR
    }

}
