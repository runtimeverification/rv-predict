package rvpredict.util;

import rvpredict.config.Configuration;
import rvpredict.db.DBEngine;
import rvpredict.db.EventInputStream;
import rvpredict.db.EventItem;
import rvpredict.logging.OfflineLoggingFactory;
import rvpredict.trace.AbstractEvent;
import rvpredict.trace.Event;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.GZIPInputStream;

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
        DBEngine db = null;
        try {
            db = new DBEngine(new OfflineLoggingFactory(configuration));
        } catch (IOException e) {
            System.err.println("Error while reading the logs.");
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Metadata file corrupted.");
            System.err.println(e.getMessage());
        }
        Map<Integer, String> locIdToStmtSig = db.getLocIdToStmtSig();
        String file = args[0];
        File f = new File(file);
        try {
            InputStream in = new FileInputStream(f);
            if (file.endsWith(OfflineLoggingFactory.ZIP_EXTENSION)) {
                in = new GZIPInputStream(in);
            }
            EventInputStream inputStream = new EventInputStream(
                    new BufferedInputStream(in));
            System.out.println("Dumping events from " + file);
            //noinspection InfiniteLoopStatement
            while (true) {
                EventItem eventItem = inputStream.readEvent();
                Event event = AbstractEvent.of(eventItem);
                System.out.println(event.toString() + locIdToStmtSig.get(event.getID()));
            }
        } catch (EOFException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
