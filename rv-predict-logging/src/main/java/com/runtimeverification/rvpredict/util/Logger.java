package com.runtimeverification.rvpredict.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;

import com.google.common.base.Strings;

/**
 * @author TraianSF
 * @author YilongL
 */
public class Logger {

    private static final int    WIDTH   =   75;
    private static final String DASH    =   "-";

    private PrintStream debug = System.err;
    private PrintStream result;

    public void setLogDir(String logDir) throws FileNotFoundException {
        // TODO(YilongL): make sure this log file doesn't grow out of control
        debug = new PrintStream(new FileOutputStream(Paths.get(logDir, "debug.log").toFile()));
        result = new PrintStream(new FileOutputStream(Paths.get(logDir, "result.txt").toFile()));
    }

    public void reportPhase(String phaseMsg) {
        report(center(phaseMsg), MSGTYPE.INFO);
    }

    private static String center(String msg) {
        int fillWidth = WIDTH - msg.length();
        return "\n" + Strings.repeat(DASH, fillWidth / 2) + msg
                + Strings.repeat(DASH, (fillWidth + 1) / 2);
    }

    public PrintStream debug() {
        return debug;
    }

    public void reportRace(String report) {
        result.println(report);
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
