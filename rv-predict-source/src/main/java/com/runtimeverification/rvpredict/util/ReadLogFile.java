package com.runtimeverification.rvpredict.util;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventReader;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
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
        Metadata metadata =
                Metadata.readFrom(Paths.get(directory.toString(), Configuration.METADATA_BIN), false);
        String file = args[0];
        if (file.endsWith("metadata.bin")) {
            System.out.println("#variable section#");
            for (int varId = 1;; varId++) {
                String varSig = metadata.getVariableSig(varId);
                if (varSig != null) {
                    System.out.printf("%s:%s%n", varId, varSig);
                } else {
                    break;
                }
            }
            System.out.println("#program location section#");
            for (int locId = 1;; locId++) {
                String locSig = metadata.getLocationSig(locId);
                if (locSig != null) {
                    System.out.printf("%s:%s%n", locId, locSig);
                } else {
                    break;
                }
            }
        } else {
            try (EventReader reader = new EventReader(Paths.get(file))) {
                System.out.println("Dumping events from " + file);
                while (true) {
                    ReadonlyEventInterface event = reader.readEvent();
                    String locSig = event.getLocationId() < 0 ? "n/a" : metadata.getLocationSig(event.getLocationId());
                    System.out.printf("%-60s %s%n", event.toString(), locSig);
                }
            } catch (EOFException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
