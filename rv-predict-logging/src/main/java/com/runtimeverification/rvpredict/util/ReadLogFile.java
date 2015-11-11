package com.runtimeverification.rvpredict.util;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventReader;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.metadata.Metadata;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Debugging class for printing the contents of a log file to console.
 * Uses metadata information to desugar location pointers to actual locations in the code.
 *
 * @author TraianSF
 */
public class ReadLogFile {
    /**
     * @param args path to a file containing a trace segment.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage " + ReadLogFile.class.getName() + " <log_file_name>");
            System.exit(1);
        }
        Path path = Paths.get(args[0]).toAbsolutePath();
        Path directory = path.getParent();
        Metadata metadata = Metadata.readFrom(Paths.get(directory.toString(), Configuration.METADATA_BIN));
        String file = args[0];
        try (EventReader reader = new EventReader(Paths.get(file))) {
            System.out.println("Dumping events from " + file);
            while (true) {
                Event event = reader.readEvent();
                String locSig = event.getLocId() < 0 ? "n/a" : metadata.getLocationSig(event.getLocId());
                System.out.printf("%-60s %s%n", event.toString(), locSig);
            }
        } catch (EOFException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
