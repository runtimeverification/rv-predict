package rvpredict.logging;

import rvpredict.instrumentation.MetaData;
import rvpredict.trace.SyncEvent;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Functionality for recording metadata.
 * A thread wakes every minute to record the previously unsaved metadata
 * Data recorded:
 *  - volatile variables
 *  - location identification
 *  - number of events written so far
 */
public class MetadataLoggerThread implements Runnable {
    private final ObjectOutputStream metadataOS;
    private final LoggingEngine loggingEngine;
    private boolean shutdown = false;
    private Thread owner;

    public MetadataLoggerThread(LoggingEngine engine) {
        loggingEngine = engine;
        metadataOS = createMetadataOS(engine.getConfig().outdir);
    }

    @Override
    public void run() {
        owner = Thread.currentThread();
        while (!shutdown) {
            try {
                synchronized (metadataOS) {
                    metadataOS.wait(60000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            saveMetaData();
        }
        saveMetaData();
    }

    private static ObjectOutputStream createMetadataOS(String directory) {
        try {
            return new ObjectOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(Paths.get(directory, rvpredict.db.DBEngine.METADATA_BIN).toFile())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void saveObject(Object object) throws IOException {
        metadataOS.writeObject(object);
    }

    /**
     * Flush un-previously-saved metadata to disk.
     */
    private void saveMetaData() {
        try {
            /* save <volatileVariable, Id> pairs */
            synchronized (MetaData.volatileVariables) {
                Set<Integer> volatileFieldIds = new HashSet<>(MetaData.unsavedVolatileVariables.size());
                for (String var : MetaData.unsavedVolatileVariables) {
                    volatileFieldIds.add(MetaData.varSigToId.get(var));
                }
                saveObject(volatileFieldIds);
                MetaData.unsavedVolatileVariables.clear();
            }

            /* save <StmtSig, LocId> pairs */
            synchronized (MetaData.stmtSigToLocId) {
                saveObject(new ArrayList<>(MetaData.unsavedStmtSigToLocId));
                MetaData.unsavedStmtSigToLocId.clear();
            }

            /* Save current trace length */
            metadataOS.writeLong(loggingEngine.getGlobalEventID());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println("I/O Error while saving stmt-loc metadata." +
                    " Metadata will be unreadable. Exiting...");
            System.exit(1);
        }
    }

    /**
     * Signals shutdown, wakes the thread, then waits for it to finish and closes the stream.
     */
    public void finishLogging() {
        shutdown = true;
        synchronized (metadataOS) {
            metadataOS.notify();
        }
        try {
            owner.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            metadataOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
