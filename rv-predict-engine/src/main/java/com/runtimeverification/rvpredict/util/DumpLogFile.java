package com.runtimeverification.rvpredict.util;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventReader;
import com.runtimeverification.rvpredict.log.EventItem;
import com.runtimeverification.rvpredict.log.OfflineLoggingFactory;
import com.runtimeverification.rvpredict.trace.Event;
import com.runtimeverification.rvpredict.trace.EventUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Debugging class for dumping the contents of a log file to console.
 * Uses metadata information to desugar location pointers to actual locations in the code.
 *
 * @author TraianSF
 */
public class DumpLogFile {
    /**
     * @param args path to a file containing a trace segment.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage " + DumpLogFile.class.getName() + " <log_file_name>");
            System.exit(1);
        }
        Path path = Paths.get(args[0]).toAbsolutePath();
        Path directory = path.getParent();
        Configuration configuration = new Configuration();
        configuration.outdir = directory.toString();
        OfflineLoggingFactory loggingFactory = new OfflineLoggingFactory(configuration);
        String file = args[0];
        try (EventReader reader = new EventReader(Paths.get(file))) {
            System.out.println("Dumping events from " + file);
            while (true) {
                EventItem eventItem = reader.readEvent();
                Event event = EventUtils.of(eventItem);
                System.out.printf("%-60s %s%n", event.toString(), loggingFactory.getStmtSig(event.getLocId()));
            }
        } catch (EOFException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
