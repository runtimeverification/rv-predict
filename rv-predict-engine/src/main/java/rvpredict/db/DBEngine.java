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
package rvpredict.db;

import java.io.*;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.List;

import rvpredict.trace.*;
import violation.IViolation;

/**
 * Engine for interacting with database.
 *
 * @author jeffhuang
 *
 */
public class DBEngine {

    private final String directory;

    // currently we use the h2 database
    private final String dbname = "RVDatabase";
    public String appname = "main";

    private final String[] scheduletablecolname = { "ID", "SIG", "SCHEDULE" };
    private final String[] scheduletablecoltype = { "INT", "VARCHAR", "ARRAY" };

    private Connection conn;
    private PreparedStatement prepStmt;

    private final String scheduletablename;
    private final String propertytablename;

    private TraceCache traceCache=null;

    public void getMetadata(Map<Long, String> threadIdNameMap, Map<Integer, String> sharedVarIdSigMap, Map<Integer, String> volatileAddresses, Map<Integer, String> stmtIdSigMap) {
        try {
            ObjectInputStream metadataIS = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(Paths.get(directory, "metadata.bin").toFile())));
            while(true) {
                List<Map.Entry<Long, String>> threadTidList;
                try {
                    threadTidList = (List<Map.Entry<Long, String>>) metadataIS.readObject();
                } catch (EOFException _) { break;} // EOF should only happen for threadTid
                for (Map.Entry<Long,String> entry : threadTidList) {
                    threadIdNameMap.put(entry.getKey(), entry.getValue());
                }
                List<Map.Entry<String, Integer>> variableIdList = (List<Map.Entry<String, Integer>>) metadataIS.readObject();
                for (Map.Entry<String, Integer> entry : variableIdList) {
                    sharedVarIdSigMap.put(entry.getValue(), entry.getKey());
                }
                List<Map.Entry<String, Integer>> volatileVarList = (List<Map.Entry<String, Integer>>) metadataIS.readObject();
                for (Map.Entry<String, Integer> entry : volatileVarList) {
                    volatileAddresses.put(entry.getValue(), entry.getKey());
                }
                List<Map.Entry<String, Integer>> stmtSigIdList = (List<Map.Entry<String, Integer>>) metadataIS.readObject();
                for (Map.Entry<String, Integer> entry : stmtSigIdList) {
                    stmtIdSigMap.put(entry.getValue(), entry.getKey());
                }

            }
        }
        catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }


    }

    // private final String NO_AUTOCLOSE = ";DB_CLOSE_ON_EXIT=FALSE";//BUGGY in
    // H2, DON'T USE IT

    public DBEngine(String directory, String name) {
        appname = name;
        this.directory = directory;

        scheduletablename = "schedule_" + name;
        propertytablename = "property_" + name;
        connectDB(directory);
    }

    public void closeDB() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks that the trace was recorded properly
     * TODO: design and implement some proper checks.
     */
    public boolean checkLog() {
        return true;
    }

    public void createScheduleTable() throws Exception {
        String sql_dropTable = "DROP TABLE IF EXISTS " + scheduletablename;
        String sql_insertdata = "INSERT INTO " + scheduletablename + " VALUES (?,?,?)";

        Statement stmt = conn.createStatement();
        stmt.execute(sql_dropTable);

        String sql_createTable = "CREATE TABLE " + scheduletablename + " ("
                + scheduletablecolname[0] + " " + scheduletablecoltype[0] + " PRIMARY KEY, "
                + scheduletablecolname[1] + " " + scheduletablecoltype[1] + ", "
                + scheduletablecolname[2] + " " + scheduletablecoltype[2] + ")";
        stmt.execute(sql_createTable);

        prepStmt = conn.prepareStatement(sql_insertdata);
    }

    private void connectDB(String directory) {
        try {
            Class.forName("rvpredict.h2.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: cannot locate h2 database driver.  This should not happen.");
            e.printStackTrace();
            System.exit(1);
        }
        try{
            conn = DriverManager.getConnection("jdbc:h2:" + directory + "/" + dbname
                    + ";DB_CLOSE_ON_EXIT=FALSE");
        }  catch (SQLException e) {
            System.err.println("Errors when connecting to the database.  Exiting.");
            e.printStackTrace();
            System.exit(1);
        } // conn.setAutoCommit(true);
    }

    public long getTraceSize() throws Exception {
        if (traceCache == null) traceCache = new TraceCache(directory);
        return traceCache.getTraceSize();
    }

    /**
     * load trace from event min to event max
     *
     * @param min
     * @param max
     * @return
     * @throws Exception
     */
    public Trace getTrace(long min, long max, TraceInfo info) throws Exception {
        if (traceCache == null) traceCache = new TraceCache(directory);
        long traceSize = traceCache.getTraceSize();
        assert min <= traceSize : "This method should only be called with a valid min value";
        if (max > traceSize) max = traceSize; // resetting max to trace size.
        Trace trace = new Trace(info);
        AbstractEvent node = null;
        for (long index = min; index <= max; index++) {
            rvpredict.db.EventItem eventItem = traceCache.getEvent(index);
            long GID = eventItem.GID;
            long TID = eventItem.TID;
            int ID = eventItem.ID;
            long ADDRL = eventItem.ADDRL;
            long ADDRR = eventItem.ADDRR;
            long VALUE = eventItem.VALUE;
            EventType TYPE = eventItem.TYPE;

            switch (TYPE) {
                case INIT:
                    node = new InitEvent(GID, TID, ID, ADDRL, ADDRR, VALUE);
                    break;
                case READ:
                    node = new ReadEvent(GID, TID, ID, ADDRL, ADDRR, VALUE);
                    break;
                case WRITE:
                    node = new WriteEvent(GID, TID, ID, ADDRL, ADDRR, VALUE);
                    break;
                case LOCK:
                    node = new LockNode(GID, TID, ID, ADDRL);
                    break;
                case UNLOCK:
                    node = new UnlockNode(GID, TID, ID, ADDRL);
                    break;
                case WAIT:
                    node = new WaitNode(GID, TID, ID, ADDRL);
                    break;
                case NOTIFY:
                    node = new SyncEvent(GID, TID, ID, EventType.NOTIFY, ADDRL);
                    break;
                case START:
                    node = new StartNode(GID, TID, ID, ADDRL);
                    break;
                case JOIN:
                    node = new SyncEvent(GID, TID, ID, EventType.JOIN, ADDRL);
                    break;
                case BRANCH:
                    node = new BranchNode(GID, TID, ID);
                    break;
                case PROPERTY:
                    node = new PropertyNode(GID, TID, ID, "" + ADDRL);
                    break;

                default:
                    System.out.println(TYPE);
                    break;
            }

            trace.addRawNode(node);

        }

        trace.finishedLoading();

        return trace;
    }

    public int getScheduleSize() {
        try {
            String sql_select = "SELECT COUNT(*) FROM " + scheduletablename;

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql_select);
            if (rs.next()) {
                return rs.getInt(1);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Object[] getSchedule(int id) {
        try {
            String sql_select = "SELECT * FROM " + scheduletablename + " WHERE ID=" + id;

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql_select);
            if (rs.next()) {
                Object o = rs.getObject(3);
                Object[] schedule = (Object[]) o;

                return schedule;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public HashMap<String, Integer> getProperty() {
        try {
            String sql_select = "SELECT * FROM " + propertytablename;

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql_select);
            HashMap<String, Integer> map = new HashMap<String, Integer>();

            while (rs.next()) {

                String name = rs.getString(1);
                Integer id = rs.getInt(2);

                map.put(name, id);

            }
            return map;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Save schedules for each violation to database. The schedule is identified
     * by a unique order.
     *
     * @param violations
     * @return
     */
    public int saveSchedulesToDB(HashSet<IViolation> violations) {

        Iterator<IViolation> violationIt = violations.iterator();

        int i = 0;
        while (violationIt.hasNext()) {

            IViolation violation = violationIt.next();
            List<List<String>> schedules = violation.getSchedules();

            Iterator<List<String>> scheduleIt = schedules.iterator();

            while (scheduleIt.hasNext()) {
                i++;
                List<String> schedule = scheduleIt.next();
                try {
                    prepStmt.setInt(1, i);
                    prepStmt.setString(2, violation.toString());
                    // Array aArray = conn.createArrayOf("VARCHAR",
                    // schedule.toArray());
                    prepStmt.setObject(3, schedule.toArray());

                    prepStmt.execute();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return i;

    }

}
