package rvpredict.logging;

import rvpredict.instrumentation.MetaData;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Traian on 16.01.2015.
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

    private void saveObject(Object object) {
        try {
            metadataOS.writeObject(object);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Flush un-previously-saved metadata to disk.
     */
    private void saveMetaData() {
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

        try {
            metadataOS.writeLong(loggingEngine.getGlobalEventID());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
