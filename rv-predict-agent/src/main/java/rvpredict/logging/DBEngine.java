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
import java.util.HashSet;
import java.util.List;
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

    // database schema
    private static final String[] SHARED_VAR_SIG_TABLE_COL_NAME = { "SIG" };
    private static final String[] SHARED_VAR_SIG_COL_TYPE = { "VARCHAR" };

    private static final String[] SHARED_ARRAY_LOC_TABLE_COLNAME = { "SIG" };
    private static final String[] SHARED_ARRAY_LOC_COL_TYPE = { "VARCHAR" };

    private Connection conn;
    private PreparedStatement prepStmt;

    private final String sharedvarsigtablename;
    private final String sharedarrayloctablename;

    // TODO: What if the program does not terminate??

    private static LinkedBlockingQueue<List<EventItem>> queue;
    // we can also use our own Stack implementation here
    private static ArrayList<EventItem> buffer;

    private final int BUFFER_THRESHOLD;
    private final boolean asynchronousLogging;

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
            RecordRT.saveMetaData(DBEngine.this, GlobalStateForInstrumentation.instance);
            metadataOS.close();
            traceOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeDB();
    }

    public void saveEventsToDB(List<EventItem> stack) {
        assert !stack.isEmpty() : "stack should not be empty here as we're saving metadata, too";

        try {
            traceOS =  new EventOutputStream(new BufferedOutputStream(
                    new FileOutputStream(Paths.get(directory, stack.get(0).GID + TraceCache.TRACE_SUFFIX).toFile())));
            for (EventItem eventItem : stack) {
                traceOS.writeEvent(eventItem);
            }
            RecordRT.saveMetaData(DBEngine.this, GlobalStateForInstrumentation.instance);
            traceOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    EventOutputStream traceOS;
    ObjectOutputStream metadataOS;
    Thread loggingThread;
    boolean shutdown = false;
    public void startAsynchronousLogging() {

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

    public DBEngine(String directory, String name, boolean asynchronousLogging) {
        BUFFER_THRESHOLD = 10*Config.instance.commandLine.window_size;
        this.directory = directory;
        this.asynchronousLogging = asynchronousLogging;
        sharedarrayloctablename = "sharedarrayloc_" + name;
        sharedvarsigtablename = "sharedvarsig_" + name;
        try {
            connectDB(directory);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void closeDB() {
        try {
            conn.createStatement().execute("SHUTDOWN");
            // conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Drops all relevant tables of the database. Used for a clean start.
     *
     * @throws Exception
     *             if errors are reported by the sql command
     */
    public void dropAll() throws Exception {
        Statement stmt = conn.createStatement();

        String sql_dropTable;
        sql_dropTable = "DROP TABLE IF EXISTS " + sharedvarsigtablename;
        stmt.execute(sql_dropTable);
        sql_dropTable = "DROP TABLE IF EXISTS " + sharedarrayloctablename;
        stmt.execute(sql_dropTable);
    }

    public void createSharedArrayLocTable(boolean newTable) throws Exception {
        Statement stmt = conn.createStatement();
        if (newTable) {
            String sql_dropTable = "DROP TABLE IF EXISTS " + sharedarrayloctablename;
            stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + sharedarrayloctablename + " ("
                + SHARED_ARRAY_LOC_TABLE_COLNAME[0] + " " + SHARED_ARRAY_LOC_COL_TYPE[0] + ")";
        stmt.execute(sql_createTable);

        String sql_insertdata = "INSERT INTO " + sharedarrayloctablename + " VALUES (?)";
        prepStmt = conn.prepareStatement(sql_insertdata);
    }

    public void createSharedVarSignatureTable(boolean newTable) throws Exception {
        Statement stmt = conn.createStatement();
        if (newTable) {
            String sql_dropTable = "DROP TABLE IF EXISTS " + sharedvarsigtablename;
            stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + sharedvarsigtablename + " ("
                + SHARED_VAR_SIG_TABLE_COL_NAME[0] + " " + SHARED_VAR_SIG_COL_TYPE[0] + ")";
        stmt.execute(sql_createTable);

        String sql_insertdata = "INSERT INTO " + sharedvarsigtablename + " VALUES (?)";
        prepStmt = conn.prepareStatement(sql_insertdata);
    }

    public void saveSharedArrayLocToDB(String sig) {
        try {
            prepStmt.setString(1, sig);

            prepStmt.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HashSet<String> loadSharedArrayLocs() {
        HashSet<String> sharedArrayLocs = new HashSet<String>();
        try {
            String sql_select = "SELECT * FROM " + sharedarrayloctablename;

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql_select);
            while (rs.next()) {
                // Get the data from the row using the column index
                String SIG = rs.getString(1);
                sharedArrayLocs.add(SIG);
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
        if (sharedArrayLocs.isEmpty())
            return null;
        else
            return sharedArrayLocs;
    }

    public HashSet<String> loadSharedVariables() {
        HashSet<String> sharedVariables = new HashSet<String>();
        try {
            String sql_select = "SELECT * FROM " + sharedvarsigtablename;

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql_select);
            while (rs.next()) {
                // Get the data from the row using the column index
                String SIG = rs.getString(1);
                sharedVariables.add(SIG);
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
        if (sharedVariables.isEmpty())
            return null;
        else
            return sharedVariables;
    }

    public void saveSharedVarSignatureToDB(String sig) {
        try {
            prepStmt.setString(1, sig);

            prepStmt.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * save an event to database. must be synchronized. otherwise, races when
     * writing to file might occur for synchronous logging.
     */
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
                    RecordRT.saveMetaData(DBEngine.this, GlobalStateForInstrumentation.instance);
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

    private void connectDB(String directory) throws Exception {
        try {
            Driver driver = new rvpredict.h2.Driver();
            String db_url = "jdbc:h2:" + directory + "/" + DB_NAME + ";DB_CLOSE_ON_EXIT=FALSE";
            conn = driver.connect(db_url, null);

            // conn = DriverManager.getConnection(db_url);
            // conn.setAutoCommit(true);
            // check if Database may be already in use
            // kill?
        } catch (Exception e) {
            e.printStackTrace();
            // DBORDER++;
            // conn =
            // DriverManager.getConnection("jdbc:h2:"+Util.getUserHomeDirectory()+dbname+DBORDER);//+";AUTO_SERVER=true"
        }
    }

    public void saveObject(Object threadTidList) {
        try {
            metadataOS.writeObject(threadTidList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
