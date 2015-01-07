package rvpredict.util;

import rvpredict.db.DBEngine;
import rvpredict.db.EventInputStream;
import rvpredict.db.EventItem;
import rvpredict.db.TraceCache;
import rvpredict.trace.AbstractEvent;
import rvpredict.trace.SyncEvent;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by Traian on 02.01.2015.
 */
public class DumpLogFile {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage " + DumpLogFile.class.getName() + " <log_file_name>");
            System.exit(1);
        }
        Path path = Paths.get(args[0]).toAbsolutePath();
        Path directory = path.getParent();
        DBEngine db = new DBEngine(directory.toString());
        Map<Integer, String> locIdToStmtSig = db.getLocIdToStmtSig();
        String file = args[0];
        File f = new File(file);
        try {
            InputStream in = new FileInputStream(f);
            if (file.endsWith(TraceCache.ZIP_EXTENSION)) {
                in = new GZIPInputStream(in);
            }
            EventInputStream inputStream = new EventInputStream(
                    new BufferedInputStream(in));
            System.out.println("Dumping events from " + file);
            while (true) {
                EventItem event = inputStream.readEvent();
                AbstractEvent abstractEvent = AbstractEvent.of(event);
                System.out.println(abstractEvent.toString() + locIdToStmtSig.get(abstractEvent.getID()));
            }
        } catch (EOFException _) {
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
