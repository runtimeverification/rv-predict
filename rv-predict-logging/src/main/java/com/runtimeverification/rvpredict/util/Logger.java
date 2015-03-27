package com.runtimeverification.rvpredict.util;

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
        default:
            break;
        }
    }

    public enum MSGTYPE {
        REAL, INFO, ERROR
    }
}
