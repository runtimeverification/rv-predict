package com.runtimeverification.rvpredict.util;

import com.google.common.base.Strings;
import com.runtimeverification.rvpredict.config.Configuration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by TraianSF on 05.08.2014.
 */
public class Logger {

    public static final int WIDTH = 75;
    public static final String DASH = "-";

    private PrintWriter out;
    Configuration config;

    /**
     * Initialize the file printer. All race detection statistics are stored
     * into the file race-report.txt
     *
     * @param config
     */
    public Logger(Configuration config) {
        this.config = config;
        try {
            String fname = "race-report.txt";

            File file = new File(config.outdir);
            file.mkdirs();
            out = new PrintWriter(new FileWriter(config.outdir + "/" + fname, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closePrinter() {
        if (out != null)
            out.close();
    }

    public PrintWriter getPrinter() {
        return out;
    }

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
            out.println(msg);
            break;
        case REAL:
        case INFO:
            System.out.println(msg);
            if (config.verbose) {
                out.println(msg);
            }
            break;
        case STATISTICS:
            if (config.verbose) {
                System.out.println(msg);
            }
            out.println(msg);
            break;
        case VERBOSE:
            if (config.verbose) {
                System.out.println(msg);
                out.println(msg);
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
