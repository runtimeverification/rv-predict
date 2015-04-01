package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.metadata.Metadata;

import java.io.*;

/**
 * Functionality for recording metadata.
 * A thread wakes every minute to record the previously unsaved metadata
 * Data recorded:
 *  - volatile variables
 *  - location identification
 *  - number of events written so far
 */
public class MetadataLogger implements LoggingTask {

    private final Metadata metadata;
    private ObjectOutputStream metadataOS;
    private final LoggingEngine loggingEngine;
    private boolean shutdown = false;
    private Thread owner;

    public MetadataLogger(LoggingEngine engine) {
        loggingEngine = engine;
        this.metadata = Metadata.instance();
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

    /**
     * Flush un-previously-saved metadata to disk.
     */
    private void saveMetaData() {
        metadata.writeUnsavedMetadataTo(metadataOS);
    }

    /**
     * Signals shutdown, wakes the thread, then waits for it to finish and closes the stream.
     */
    @Override
    public void finishLogging() throws InterruptedException, IOException {
        shutdown = true;
        if (owner == null) return;
        synchronized (metadataOS) {
            metadataOS.notify();
        }
        owner.join();
        metadataOS.close();
    }

    @Override
    public void setOwner(Thread owner) {
        this.owner = owner;
    }
}
