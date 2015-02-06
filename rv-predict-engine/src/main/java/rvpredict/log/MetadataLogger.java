package rvpredict.log;

import rvpredict.instrumentation.MetaData;

import java.io.*;
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
public class MetadataLogger implements Runnable {
    private ObjectOutputStream metadataOS;
    private final LoggingEngine loggingEngine;
    private boolean shutdown = false;
    private Thread owner;

    public MetadataLogger(LoggingEngine engine) {
        loggingEngine = engine;
    }

    @Override
    public void run() {
        try {
            metadataOS = loggingEngine.getLoggingFactory().createMetadataOS();
            while (!shutdown) {
                synchronized (metadataOS) {
                    metadataOS.wait(60000);
                }
                saveMetaData();
            }
            saveMetaData();
        } catch (InterruptedException e) {
            if (!shutdown) {
                System.err.println("Warning: Process is being forcefully shut down. Metadata might be lost.");
                System.err.print(e.getMessage());
            }
            saveMetaData();
        } catch (IOException e) {
            System.err.println("Error: I/O error while creating metadata log file. Metadata will not be recorded.");
            System.err.println(e.getMessage());
        }
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
                    volatileFieldIds.add(MetaData.getVariableId(var));
                }
                saveObject(volatileFieldIds);
                MetaData.unsavedVolatileVariables.clear();
            }

            /* save <VarSig, VarId> pairs */
            synchronized (MetaData.varSigToVarId) {
                saveObject(new ArrayList<>(MetaData.unsavedVarIdToVarSig));
                MetaData.unsavedVarIdToVarSig.clear();
            }

            /* save <StmtSig, LocId> pairs */
            synchronized (MetaData.stmtSigToLocId) {
                saveObject(new ArrayList<>(MetaData.unsavedLocIdToStmtSig));
                MetaData.unsavedLocIdToStmtSig.clear();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println("I/O Error while saving metadata." +
                    " Metadata will be unreadable. Exiting...");
            System.exit(1);
        }
    }

    /**
     * Signals shutdown, wakes the thread, then waits for it to finish and closes the stream.
     */
    public void finishLogging() throws InterruptedException, IOException {
        shutdown = true;
        if (owner == null) return;
        synchronized (metadataOS) {
            metadataOS.notify();
        }
        owner.join();
        metadataOS.close();
    }

    public void setOwner(Thread owner) {
        this.owner = owner;
    }
}
