package com.runtimeverification.rvpredict.util;

import com.runtimeverification.rvpredict.config.Configuration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by TraianSF on 05.08.2014.
 */
public class Logger {

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
