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

import rvpredict.db.EventItem;
import rvpredict.config.Config;
import rvpredict.db.EventOutputStream;
import rvpredict.instrumentation.GlobalStateForInstrumentation;
import rvpredict.trace.EventType;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Engine for interacting with database.
 *
 * @author jeffhuang
 *
 */
public class DBEngine {

    private static final AtomicLong globalEventID  = new AtomicLong(0);

    private final int BUFFER_THRESHOLD;

    private final GlobalStateForInstrumentation globalState;
    private final Thread metadataLoggingThread;

    public void finishLogging() {
        shutdown = true;
        try {
            synchronized (metadataOS) {
                metadataOS.notify();
            }
            metadataLoggingThread.join();
            for (EventOutputStream stream : threadLocalTraceOS.getStreamsMap().values()) {
                try {
                    stream.flush();
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

    private final ThreadLocalEventStream threadLocalTraceOS;

    private EventOutputStream getTraceOS() {
        return threadLocalTraceOS.get();
    }

    private EventOutputStream newThreadLocalTraceOS(long gid) {
        EventOutputStream traceOS = newTraceOs(gid);
        threadLocalTraceOS.set(traceOS);
        return traceOS;
    }

    private EventOutputStream newTraceOs(long gid) {
        return threadLocalTraceOS.getNewTraceOs(gid);
    }

    private final ObjectOutputStream metadataOS;
    private boolean shutdown = false;

    public DBEngine(GlobalStateForInstrumentation globalState, String directory) {
        BUFFER_THRESHOLD = 10*Config.instance.commandLine.window_size;
        this.globalState = globalState;
        threadLocalTraceOS = new ThreadLocalEventStream(directory);
        metadataOS = createMetadataOS(directory);
        metadataLoggingThread = startMetadataLogging();
    }

    Thread startMetadataLogging() {
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
                            new FileOutputStream(Paths.get(directory, "metadata.bin").toFile())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * save an event to database. ThreadLocal
     */
    public void saveEvent(EventType eventType, int id, long addrl, long addrr, long value) {
        long tid = Thread.currentThread().getId();
        long gid = DBEngine.globalEventID.incrementAndGet();
        EventItem e = new EventItem(gid, tid, id, addrl, addrr, value, eventType);
        if (shutdown) return;
        try {
            EventOutputStream traceOS = getTraceOS();
            traceOS.writeEvent(e);
            long eventsWritten = traceOS.getEventsWrittenCount();
            if (eventsWritten % BUFFER_THRESHOLD == 0) {
                traceOS.close();
                newThreadLocalTraceOS(1 + e.GID);
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

    public void saveEvent(EventType eventType, int locId, long arg) {
        saveEvent(eventType, locId, arg, 0, 0);
    }

    public void saveEvent(EventType eventType, int locId) {
        saveEvent(eventType, locId, 0, 0, 0);
    }

    private void saveObject(Object threadTidList) {
        try {
            metadataOS.writeObject(threadTidList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveMetaData() {
        /* save <threadId, name> pairs */
        List<Entry<Long,String>> threadIdNamePairs = new ArrayList<>(globalState.unsavedStmtSigToLocId.size());
        Iterator<Entry<Long, String>> iter = globalState.unsavedThreadIdToName.iterator();
        while (iter.hasNext()) {
            threadIdNamePairs.add(iter.next());
            iter.remove();
        }
        saveObject(threadIdNamePairs);

        /* save <variable, id> pairs */
        synchronized (globalState.varSigToId) {
            // TODO(YilongL): I want to write the following but I couldn't
            // because DBEngine#getMetadata assumes certain order of the
            // saved objects
//            if (!globalState.unsavedVarSigToId.isEmpty()) {
//                saveObject(globalState.unsavedVarSigToId);
//                globalState.unsavedVarSigToId.clear();
//            }

            saveObject(globalState.unsavedVarSigToId);
            globalState.unsavedVarSigToId.clear();
        }

        /* save <volatileVariable, Id> pairs */
        synchronized (globalState.volatileVariables) {
            // TODO(YilongL): volatileVariable Id should be constructed when
            // reading metadata in backend; not here
            List<Entry<String, Integer>> volatileVarIdPairs = new ArrayList<>(globalState.unsavedVolatileVariables.size());
            for (String var : globalState.unsavedVolatileVariables) {
                volatileVarIdPairs.add(new SimpleEntry<>(var, globalState.varSigToId.get(var)));
            }
            saveObject(volatileVarIdPairs);
        }

        /* save <StmtSig, LocId> pairs */
        synchronized (globalState.stmtSigToLocId) {
            saveObject(globalState.unsavedStmtSigToLocId);
            globalState.unsavedStmtSigToLocId.clear();
        }
    }
}
