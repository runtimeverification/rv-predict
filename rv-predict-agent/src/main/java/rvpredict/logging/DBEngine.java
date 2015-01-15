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

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.tuple.Pair;
import rvpredict.config.Configuration;
import rvpredict.db.EventItem;
import rvpredict.db.EventOutputStream;
import rvpredict.db.TraceCache;
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

    private static final AtomicInteger threadId = new AtomicInteger();
    private final AtomicLong globalEventID  = new AtomicLong(0);
    private static final int BUFFER_THRESHOLD = 10000;
    private final Thread metadataLoggingThread;
    private final ThreadLocalEventStream threadLocalTraceOS;
    private final ObjectOutputStream metadataOS;
    private boolean shutdown = false;

    public static DataOutputStream newDataOutputStream(Configuration config) {
        DataOutputStream dataOutputStream = null;
        try {
            int id = threadId.incrementAndGet();
            OutputStream outputStream = new FileOutputStream(Paths.get(config.outdir,
                    id + "_" + TraceCache.TRACE_SUFFIX
                            + (config.zip ? TraceCache.ZIP_EXTENSION : "")).toFile());
            if (config.zip) {
                outputStream = new GZIPOutputStream(outputStream,true);
            }
            dataOutputStream = new DataOutputStream(new BufferedOutputStream(
                    outputStream));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) { // GZIPOutputStream exception
            e.printStackTrace();
        }
        return dataOutputStream;
    }


    /**
     * Writes an {@link rvpredict.db.EventItem} to the underlying output stream.
     *
     * @param      event   an {@link rvpredict.db.EventItem} to be written.
     *                     If no exception is thrown, the counter
     *                     <code>eventsWrittenCount</code> is incremented by
     *                     {@link rvpredict.db.EventItem#SIZEOF}.
     * @exception  java.io.IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public static final void writeEvent(DataOutputStream dataOutputStream, EventItem event) throws IOException {
        dataOutputStream.writeLong(event.GID);
        dataOutputStream.writeLong(event.TID);
        dataOutputStream.writeInt(event.ID);
        dataOutputStream.writeLong(event.ADDRL);
        dataOutputStream.writeLong(event.ADDRR);
        dataOutputStream.writeLong(event.VALUE);
        dataOutputStream.writeByte(event.TYPE.ordinal());
    }

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
            for (EventOutputStream stream : threadLocalTraceOS.getStreams()) {
                stream.close();
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
        loggersRegistry = new LinkedBlockingQueue<>();
        threadLocalTraceOS = new ThreadLocalEventStream(loggersRegistry);
        metadataOS = createMetadataOS(config.outdir);
        metadataLoggingThread = startMetadataLogging();
    }

    BlockingQueue<BlockingQueue<List<EventItem>>> loggersRegistry;
    static final BlockingQueue<List<EventItem>> LOGGING_END_MARK = new LinkedBlockingQueue<>();
    private Thread startLogging(final Configuration config) {
        Thread loggingThread = new Thread(new Runnable() {
            final List<EventItem> LOGGER_END_MARK = new ArrayList<>();
            BlockingQueue<List<EventItem>> loggerQueue;
            @Override
            public void run() {
                List<Pair<Thread, BlockingQueue<List<EventItem>>>> loggers = new LinkedList<>();
                try {
                    while (LOGGING_END_MARK != (loggerQueue = loggersRegistry.take())) {
                        final DataOutputStream outputStream = newDataOutputStream(config);
                        Thread loggerThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    List<EventItem> buffer;
                                    while (LOGGER_END_MARK != (buffer = loggerQueue.take())) {
                                        for (EventItem event : buffer) {
                                            writeEvent(outputStream, event);
                                        }
                                        synchronized (metadataOS) {
                                            metadataOS.notify();
                                        }
                                        outputStream.flush();
                                    }
                                    outputStream.close();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        loggerThread.setDaemon(true);
                        loggerThread.start();
                        loggers.add(Pair.of(loggerThread,loggerQueue));
                    }
                    for (Pair<Thread, BlockingQueue<List<EventItem>>> loggerThread : loggers) {
                        loggerThread.getRight().add(LOGGER_END_MARK);
                        loggerThread.getLeft().join();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        loggingThread.setDaemon(true);
        loggingThread.start();
        return loggingThread;
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
        EventOutputStream traceOS = threadLocalTraceOS.get();
        traceOS.writeEvent(e);
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
