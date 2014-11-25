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
import rvpredict.db.TraceCache;
import rvpredict.instrumentation.GlobalStateForInstrumentation;
import rvpredict.trace.EventType;

import java.io.*;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Engine for interacting with database.
 *
 * @author jeffhuang
 *
 */
public class DBEngine {

    private static final AtomicLong globalEventID  = new AtomicLong(0);

    private static final String DB_NAME = "RVDatabase";
    private final String directory;

    private Connection conn;

    private final String sharedvarsigtablename;
    private final String sharedarrayloctablename;

    // TODO: What if the program does not terminate??

    private static LinkedBlockingQueue<List<EventItem>> queue;
    // we can also use our own Stack implementation here
    private static ArrayList<EventItem> buffer;

    private final int BUFFER_THRESHOLD;
    private final boolean asynchronousLogging;

    private final GlobalStateForInstrumentation globalState;

    public void saveCurrentEventsToDB() {
        queue.add(buffer);
        buffer = new ArrayList<>(BUFFER_THRESHOLD);
    }

    // private final String NO_AUTOCLOSE = ";DB_CLOSE_ON_EXIT=FALSE";//BUGGY in
    // H2, DON'T USE IT

    public void finishLogging() {
        shutdown = true;
        if (asynchronousLogging) {
            try {
                System.out.print("Done executing. Flushing log buffers to disk.");
                loggingThread.join();
                System.out.println(" done.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            saveEventsToDB(buffer);
        }
        try {
            saveMetaData();
            metadataOS.close();
            traceOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeDB();
    }

    private void saveEventsToDB(List<EventItem> stack) {
        assert !stack.isEmpty() : "stack should not be empty here as we're saving metadata, too";

        try {
            traceOS =  new EventOutputStream(new BufferedOutputStream(
                    new FileOutputStream(Paths.get(directory, stack.get(0).GID + TraceCache.TRACE_SUFFIX).toFile())));
            for (EventItem eventItem : stack) {
                traceOS.writeEvent(eventItem);
            }
            saveMetaData();
            traceOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    EventOutputStream traceOS;
    ObjectOutputStream metadataOS;
    Thread loggingThread;
    boolean shutdown = false;

    private void startAsynchronousLogging() {

        queue = new LinkedBlockingQueue<>();
        buffer = new ArrayList<>(BUFFER_THRESHOLD);

        loggingThread = new Thread(new Runnable() {

            @Override
            public void run() {

                while (true) {
                    try {
                        List<EventItem> stack = queue.poll(1, TimeUnit.SECONDS);
                        if (stack == null) {
                            if (shutdown && queue.size() == 0) {
                                break;
                            } else continue;
                        }
                        saveEventsToDB(stack);
                        if (shutdown && queue.size()%10 == 0) System.out.print('.');

                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        });

        loggingThread.setDaemon(true);

        loggingThread.start();

    }

    public DBEngine(GlobalStateForInstrumentation globalState, String directory, String name, boolean asynchronousLogging) {
        BUFFER_THRESHOLD = 10*Config.instance.commandLine.window_size;
        this.globalState = globalState;
        this.directory = directory;
        this.asynchronousLogging = asynchronousLogging;
        sharedarrayloctablename = "sharedarrayloc_" + name;
        sharedvarsigtablename = "sharedvarsig_" + name;
        connectDB(directory);

        try {
            metadataOS = new ObjectOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(Paths.get(directory, "metadata.bin").toFile())));
            if (asynchronousLogging) {
                startAsynchronousLogging();
            } else {
                traceOS =  new EventOutputStream(new BufferedOutputStream(
                        new FileOutputStream(Paths.get(directory, 1 + TraceCache.TRACE_SUFFIX).toFile())));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeDB() {
        try {
            conn.createStatement().execute("SHUTDOWN");
            // conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Drops all relevant tables of the database. Used for a clean start.
     * @throws SQLException
     *
     */
    public void dropAll() throws SQLException {
        Statement stmt = conn.createStatement();

        String sql_dropTable;
        sql_dropTable = "DROP TABLE IF EXISTS " + sharedvarsigtablename;
        stmt.execute(sql_dropTable);
        sql_dropTable = "DROP TABLE IF EXISTS " + sharedarrayloctablename;
        stmt.execute(sql_dropTable);
    }

    /**
     * save an event to database. must be synchronized. otherwise, races when
     * writing to file might occur for synchronous logging.
     */
    // TODO(YilongL): why synchronize this method? too slow!
    public synchronized void saveEvent(EventType TYPE, int ID, long ADDRL, long ADDRR, long VALUE) {
        long TID = Thread.currentThread().getId();
        EventItem e = new EventItem(DBEngine.globalEventID.incrementAndGet(), TID, ID, ADDRL, ADDRR, VALUE, TYPE);
        if (asynchronousLogging) {
            if (buffer.size() >= BUFFER_THRESHOLD) {
                saveCurrentEventsToDB();
            }
            buffer.add(e);
        } else {
            if (shutdown) return;
            try {
                if (e.GID % BUFFER_THRESHOLD == 0) {
                    traceOS.close();
                    saveMetaData();
                    traceOS =  new EventOutputStream(new BufferedOutputStream(
                            new FileOutputStream(Paths.get(directory, e.GID + TraceCache.TRACE_SUFFIX).toFile())));
                }
                traceOS.writeEvent(e);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void saveEvent(EventType eventType, int locId, long arg) {
        saveEvent(eventType, locId, arg, 0, 0);
    }

    public void saveEvent(EventType eventType, int locId) {
        saveEvent(eventType, locId, 0, 0, 0);
    }

    private void connectDB(String directory) {
        try {
            Driver driver = new rvpredict.h2.Driver();
            String db_url = "jdbc:h2:" + directory + "/" + DB_NAME + ";DB_CLOSE_ON_EXIT=FALSE";
            conn = driver.connect(db_url, null);

            // conn = DriverManager.getConnection(db_url);
            // conn.setAutoCommit(true);
            // check if Database may be already in use
            // kill?
        } catch (SQLException e) {
            e.printStackTrace();
            // DBORDER++;
            // conn =
            // DriverManager.getConnection("jdbc:h2:"+Util.getUserHomeDirectory()+dbname+DBORDER);//+";AUTO_SERVER=true"
        }
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
