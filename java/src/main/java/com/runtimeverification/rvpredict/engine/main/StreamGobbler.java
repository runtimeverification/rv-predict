package com.runtimeverification.rvpredict.engine.main;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Daemon thread used to connect IO streams between external processes.
 *
 * @author YilongL
 */
public class StreamGobbler extends Thread {

    private final InputStream is;
    private final PrintStream os;

    /**
     * Flush the print stream at the end of each line.
     */
    private final boolean flush;

    private StreamGobbler(InputStream is, PrintStream os, boolean flush) {
        this.is = is;
        this.os = os;
        this.flush = flush;
    }

    @Override
    public void run() {
        try (Scanner scanner = new Scanner(is)) {
            while (scanner.hasNextLine()) {
                os.println(scanner.nextLine());
                if (flush) {
                    os.flush();
                }
            }
        }
    }

    public static StreamGobbler spawn(InputStream is, PrintStream os) {
        return spawn(is, os, false);
    }

    public static StreamGobbler spawn(InputStream is, PrintStream os, boolean flush) {
        StreamGobbler gobbler = new StreamGobbler(is, os, flush);
        gobbler.setDaemon(true);
        gobbler.start();
        return gobbler;
    }

}
