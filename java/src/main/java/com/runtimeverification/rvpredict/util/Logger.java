package com.runtimeverification.rvpredict.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;

import com.google.common.base.Strings;

/**
 * @author TraianSF
 * @author YilongL
 */
public class Logger {

    private static final String RV_PREDICT_CONSOLE_PREFIX = "[RV-Predict] ";
    private static final int    WIDTH   =   75;
    private static final String DASH    =   "-";

    private PrintStream debug = System.err;
    private PrintStream result;
    private boolean report_progress;

    public Logger(boolean report_progress) {
        PrintStream blackhole = new PrintStream(new OutputStream() {
            public void write(int b) throws IOException {
            }
            public void write(byte[] b) throws IOException {
            }
            public void write(byte[] b, int off, int len) throws IOException {
            }
        });
        debug = blackhole;
        result = blackhole;
        this.report_progress = report_progress;
    }
    public Logger() {
        this(false);
    }
    public void enableProgressReport() {
        report_progress = true;
    }
    public void setLogDir(String logDir) throws FileNotFoundException {
        // TODO(YilongL): make sure this log file doesn't grow out of control
        debug = new PrintStream(new FileOutputStream(Paths.get(logDir, "debug.log").toFile()));
        result = new PrintStream(new FileOutputStream(Paths.get(logDir, "result.txt").toFile()));
    }

    public void reportPhase(String phaseMsg) {
        report(center(phaseMsg), MSGTYPE.PHASE);
    }

    private static String center(String msg) {
        int fillWidth = WIDTH - msg.length();
        return "\n" + Strings.repeat(DASH, fillWidth / 2) + msg
                + Strings.repeat(DASH, (fillWidth + 1) / 2);
    }

    public void debug(String msg) {
        debug.println(msg);
    }

    public void debug(Throwable e) {
        e.printStackTrace(debug);
    }

    public void reportRace(String report) {
        result.println(report);
    }

    public synchronized void report(String msg, MSGTYPE type) {
        switch (type) {
        case ERROR:
            System.err.println(RV_PREDICT_CONSOLE_PREFIX + "Error: " + msg);
            break;
        case INFO:
            System.err.println(RV_PREDICT_CONSOLE_PREFIX + msg);
            break;
        case PROGRESS:
            if (!report_progress)
                return;
            /*FALLTHROUGH*/
        case PHASE:
        case VERBOSE:
            System.err.println(msg);
            break;
        case REPORT:
            System.out.println(msg);
            break;
        }
    }

    public enum MSGTYPE {
        ERROR, INFO, PHASE, PROGRESS, REPORT, VERBOSE
    }

}
