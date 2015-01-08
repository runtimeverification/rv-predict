/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package rvpredict.logging;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import rvpredict.config.Configuration;
import rvpredict.db.EventItem;
import rvpredict.db.EventOutputStream;
import rvpredict.instrumentation.MetaData;
import rvpredict.trace.EventType;

/**
 * Class encapsulating functionality for recording events to disk.
 * TODO(TraianSF): Maybe we should rename the class now that there is no DB code left.
 *
 * @author TraianSF
 *
 */
public class DBEngine {

    private final AtomicLong globalEventID  = new AtomicLong(0);
    private static final int BUFFER_THRESHOLD = 10000;
    private final Thread metadataLoggingThread;
    private final ThreadLocalEventStream threadLocalTraceOS;
    private final ObjectOutputStream metadataOS;
    private boolean shutdown = false;

    /**
     * Method invoked at the end of the logging task, to insure that
     * all data is flushed to disk before concluding.
     */
    public void finishLogging() {
        shutdown = true;
        try {
            synchronized (metadataOS) {
                metadataOS.notify();
            }
            metadataLoggingThread.join();
            for (EventOutputStream stream : threadLocalTraceOS.getStreamsMap().values()) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // TODO(TraianSF) We can probably safely ignore file errors at this (shutdown) stage
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            metadataOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DBEngine(Configuration config) {
        threadLocalTraceOS = new ThreadLocalEventStream(config);
        metadataOS = createMetadataOS(config.outdir);
        metadataLoggingThread = startMetadataLogging();
    }

    private Thread startMetadataLogging() {
        Thread metadataLoggingThread = new Thread(new Runnable() {

            @Override
            public void run() {
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

        });

        metadataLoggingThread.setDaemon(true);

        metadataLoggingThread.start();
        return metadataLoggingThread;
    }

    private ObjectOutputStream createMetadataOS(String directory) {
        try {
            return new ObjectOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(Paths.get(directory, rvpredict.db.DBEngine.METADATA_BIN).toFile())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks if we are in the process of class loading or instrumentation.
     * <p>
     * Note that this method would not be necessary if we had mock for
     * fundamental JDK classes that are used in class loading and
     * instrumentation, i.e, {@code ArrayList}, {@code HashMap}, {@code Vector},
     * etc.
     */
    private boolean skipSavingEvent() {
        StackTraceElement[] stackTraceElems = Thread.currentThread().getStackTrace();
        if (stackTraceElems.length == 0) {
            return false;
        }

        // TODO(YilongL): implement a profiling mechanism to see the top
        // event-producing classes

        String entryClass = stackTraceElems[stackTraceElems.length - 1].getClassName();
        /* YilongL: note that we cannot skip saving event by checking
         * entryClass.startsWith("java") because java.lang.Thread.run() */
        if (entryClass.startsWith("sun")) {
            // sun.instrument.InstrumentationImpl.loadClassAndCallPremain
            return true;
        }

        /* a typical stack trace of class loading plus agent instrumentation:
         *      ...
         *      at rvpredict.instrumentation.Agent.transform(Agent.java:144)
         *      at sun.instrument.TransformerManager.transform(TransformerManager.java:188)
         *      at sun.instrument.InstrumentationImpl.transform(InstrumentationImpl.java:428)
         *      at java.lang.ClassLoader.defineClass1(Native Method)
         *      at java.lang.ClassLoader.defineClass(ClassLoader.java:760)
         *      at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:142)
         *      at java.net.URLClassLoader.defineClass(URLClassLoader.java:455)
         *      at java.net.URLClassLoader.access$100(URLClassLoader.java:73)
         *      at java.net.URLClassLoader$1.run(URLClassLoader.java:367)
         *      at java.net.URLClassLoader$1.run(URLClassLoader.java:361)
         *      at java.security.AccessController.doPrivileged(Native Method)
         *      at java.net.URLClassLoader.findClass(URLClassLoader.java:360)
         *      at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
         *      at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:308)
         *      at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
         *      ...
         */
        for (StackTraceElement e : stackTraceElems) {
            String className = e.getClassName();
            if (className.startsWith("java.lang.ClassLoader") || className.startsWith("sun")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Saves an {@link rvpredict.db.EventItem} to the database.
     * Each event is saved in a file corresponding to its own thread.
     *
     * @see rvpredict.db.EventItem#EventItem(long, long, int, long, long, long, rvpredict.trace.EventType)
     *      for a more elaborate description of the parameters.
     * @see java.lang.ThreadLocal
     *
     * @param eventType  type of event being recorded
     * @param id location id of the event
     * @param addrl  additional information identifying the event
     * @param addrr additional information identifying the event
     * @param value data involved in the event
     */
    public void saveEvent(EventType eventType, int id, long addrl, long addrr, long value) {
        if (shutdown || skipSavingEvent()) return;

        long gid = globalEventID.incrementAndGet();
        long tid = Thread.currentThread().getId();
        EventItem e = new EventItem(gid, tid, id, addrl, addrr, value, eventType);
        try {
            /*
             * TODO(YilongL): the following code seems to cause the infinite
             * recursion when running nailgun; on the other hand, since this
             * method can be called from RecordRT, we should try to keep its
             * dependence on other classes to a minimal degree
             */
            EventOutputStream traceOS = threadLocalTraceOS.get();
            traceOS.writeEvent(e);

            long eventsWritten = traceOS.getEventsWrittenCount();
            if (eventsWritten % BUFFER_THRESHOLD == 0) {
                // Flushing events and metadata periodically to allow crash recovery.
                traceOS.flush();
                synchronized (metadataLoggingThread) {
                    metadataLoggingThread.notify();
                }
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Wrapper for {@link #saveEvent(rvpredict.trace.EventType, int, long, long, long)}
     * The missing arguments default to 0.
     */
    public void saveEvent(EventType eventType, int locId, long arg) {
        saveEvent(eventType, locId, arg, 0, 0);
    }

    /**
     * Wrapper for {@link #saveEvent(rvpredict.trace.EventType, int, long, long, long)}
     * The missing arguments default to 0.
     */
     public void saveEvent(EventType eventType, int locId) {
        saveEvent(eventType, locId, 0, 0, 0);
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
            metadataOS.writeLong(globalEventID.get());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
