package rvpredict.util;

import rvpredict.config.Configuration;
import rvpredict.log.EventInputStream;
import rvpredict.log.EventItem;
import rvpredict.log.OfflineLoggingFactory;
import rvpredict.trace.Event;
import rvpredict.trace.EventUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        OfflineLoggingFactory loggingFactory = new OfflineLoggingFactory(configuration);
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
                Event event = EventUtils.of(eventItem);
                System.out.println(event.toString() + loggingFactory.getStmtSig(event.getID()));
            }
        } catch (EOFException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
