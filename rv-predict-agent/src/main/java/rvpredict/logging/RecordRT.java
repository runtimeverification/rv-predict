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

import rvpredict.config.Config;
import rvpredict.instrumentation.GlobalStateForInstrumentation;
import rvpredict.trace.EventType;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class RecordRT {

    private static HashMap<Long, Integer> threadTidIndexMap;
    public static HashSet<Integer> sharedVariableIds;
    public static HashSet<Integer> sharedArrayIds;
    private static HashMap<Integer, Long> writeThreadMap;
    private static HashMap<Integer, long[]> readThreadMap;
    public static HashMap<Integer, HashSet<Integer>> arrayIdsMap;

    private static HashMap<Integer, Long> writeThreadArrayMap;
    private static HashMap<Integer, long[]> readThreadArrayMap;
    private final static String MAIN_NAME = "0";

    static ThreadLocal<HashSet<Integer>> threadLocalIDSet;
    static ThreadLocal<HashSet<Integer>> threadLocalIDSet2;

    // engine for storing events into database
    private static DBEngine db;

    public static void init(DBEngine db) {
        RecordRT.db = db;
        if (Config.instance.commandLine.agentOnlySharing) {
            sharedVariableIds = new HashSet<>();
            writeThreadMap = new HashMap<>();
            readThreadMap = new HashMap<>();

            sharedArrayIds = new HashSet<>();
            arrayIdsMap = new HashMap<>();
            writeThreadArrayMap = new HashMap<>();
            readThreadArrayMap = new HashMap<>();

            threadLocalIDSet = new ThreadLocal<HashSet<Integer>>() {
                @Override
                protected HashSet<Integer> initialValue() {

                    return new HashSet<Integer>();

                }
            };
            threadLocalIDSet2 = new ThreadLocal<HashSet<Integer>>() {
                @Override
                protected HashSet<Integer> initialValue() {

                    return new HashSet<Integer>();

                }
            };
        } else {
            initNonSharing(false);
        }
    }

    /**
     * initialize the database engine
     *
     * @throws Exception
     */
    public static void initNonSharing(boolean newTable) {
        long tid = Thread.currentThread().getId();

        // load sharedvariables and sharedarraylocations
        GlobalStateForInstrumentation.instance.setSharedArrayLocations(db.loadSharedArrayLocs());
        GlobalStateForInstrumentation.instance.setSharedVariables(db.loadSharedVariables());

        GlobalStateForInstrumentation.instance.registerThreadName(tid, MAIN_NAME);

        threadTidIndexMap = new HashMap<Long, Integer>();
        threadTidIndexMap.put(tid, 1);

    }

    public static void saveSharedMetaData(DBEngine db, HashSet<String> sharedVariables,
            HashSet<String> sharedArrayLocations) {

        try {
            if (Config.instance.verbose)
                System.out.println("====================SHARED VARIABLES===================");

            db.createSharedVarSignatureTable(false);
            for (String sig : sharedVariables) {
                db.saveSharedVarSignatureToDB(sig);
                if (Config.instance.verbose)
                    System.out.println(sig);
            }

            if (Config.instance.verbose)
                System.out.println("====================SHARED ARRAY LOCATIONS===================");

            db.createSharedArrayLocTable(false);
            for (String sig : sharedArrayLocations) {
                db.saveSharedArrayLocToDB(sig);
                if (Config.instance.verbose)
                    System.out.println(sig);
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void saveMetaData(DBEngine db, GlobalStateForInstrumentation state) {
        ConcurrentHashMap<Long, String> threadTidMap = state.unsavedThreadTidNameMap;
        ConcurrentHashMap<String, Integer> variableIdMap = state.unsavedVariableIdMap;
        ConcurrentHashMap<String, Boolean> volatileVariables = state.unsavedVolatileVariables;
        ConcurrentHashMap<String, Integer> stmtSigIdMap = state.unsavedStmtSigIdMap;
        // just reuse the connection

        // TODO: if db is null or closed, there must be something wrong
        Iterator<Entry<Long, String>> threadIdNameIter = threadTidMap.entrySet().iterator();
        List<Entry<Long,String>> threadTidList = new ArrayList<>(threadTidMap.size());
        while (threadIdNameIter.hasNext()) {
            Map.Entry<Long,String> entry = threadIdNameIter.next();
            threadTidList.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
        }
        db.saveObject(threadTidList);
        // save variable - id to database
        Iterator<Entry<String, Integer>> variableIdMapIter = variableIdMap.entrySet()
                .iterator();
        List<Entry<String, Integer>> variableIdList = new ArrayList<>(variableIdMap.size());
        while (variableIdMapIter.hasNext()) {
            Map.Entry<String, Integer> entry = variableIdMapIter.next();
            variableIdMapIter.remove();
            variableIdList.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
        }
        db.saveObject(variableIdList);

        // save volatilevariable - id to database

        List<Entry<String, Integer>> volatileVarList = new ArrayList<>(volatileVariables.size());
        Iterator<Entry<String, Boolean>> volatileIt = volatileVariables.entrySet().iterator();
        while (volatileIt.hasNext()) {
            String sig = volatileIt.next().getKey();
            volatileIt.remove();
            Integer id = GlobalStateForInstrumentation.instance.variableIdMap.get(sig);
            volatileVarList.add(new AbstractMap.SimpleEntry<>(sig,id));
        }
        db.saveObject(volatileVarList);
        // save stmt - id to database

        List<Entry<String, Integer>> stmtSigIdList = new ArrayList<>(stmtSigIdMap.size());
        Iterator<Entry<String, Integer>> stmtSigIdMapIter = stmtSigIdMap.entrySet().iterator();
        while (stmtSigIdMapIter.hasNext()) {
            Entry<String, Integer> entry = stmtSigIdMapIter.next();
            stmtSigIdList.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            stmtSigIdMapIter.remove();
            // System.out.println("* ["+id+"] "+sig+" *");
        }
        db.saveObject(stmtSigIdList);
    }

    public static void logBranch(int ID) {
        db.saveEvent(EventType.BRANCH, ID, 0, 0, 0);
    }

    public static void logBasicBlock(int ID) {
        db.saveEvent(EventType.BASIC_BLOCK, ID, 0, 0, 0);
    }

    public static void logWait(int ID, final Object o) {
        db.saveEvent(EventType.WAIT, ID, System.identityHashCode(o), 0, 0);

    }

    public static void logNotify(int ID, final Object o) {
        db.saveEvent(EventType.NOTIFY, ID, System.identityHashCode(o), 0, 0);
    }

    public static void logLock(int ID, final Object lock) {
        db.saveEvent(EventType.LOCK, ID, System.identityHashCode(lock), 0, 0);
    }

    public static void logUnlock(int ID, final Object lock) {
        db.saveEvent(EventType.UNLOCK, ID, System.identityHashCode(lock), 0, 0);
    }

    /**
     * detect shared variables -- two conditions 1. the address is accessed by
     * more than two threads 2. at least one of them is a write
     *
     * @param ID
     *            -- shared variable id
     * @param SID
     *            -- field id
     * @param write
     *            or read
     */
    public static void logFieldAcc(int ID, int SID, final boolean write) {
        long tid = Thread.currentThread().getId();
        if (!threadLocalIDSet.get().contains(ID)) {
            if (threadLocalIDSet2.get().contains(ID))
                threadLocalIDSet.get().add(ID);
            else
                threadLocalIDSet2.get().add(ID);

            // o is not used...

            // instance-based approach consumes too much memory

            // String sig =
            // o==null?"."+SID:System.identityHashCode(o)+"."+SID;

            if (Config.instance.verbose) {
                String readOrWrite = (write ? " write" : " read");
                System.out.println("Thread " + tid + " " + readOrWrite + " variable " + SID);
            }
            if (!sharedVariableIds.contains(SID)) {
                if (writeThreadMap.containsKey(SID)) {
                    if (writeThreadMap.get(SID) != tid) {
                        sharedVariableIds.add(SID);
                        return;
                    }
                }

                if (write) {
                    if (readThreadMap.containsKey(SID)) {
                        long[] readThreads = readThreadMap.get(SID);
                        if (readThreads != null
                                && (readThreads[0] != tid || (readThreads[1] > 0 && readThreads[1] != tid))) {
                            sharedVariableIds.add(SID);
                            return;
                        }
                    }

                    writeThreadMap.put(SID, tid);
                } else {
                    long[] readThreads = readThreadMap.get(SID);

                    if (readThreads == null) {
                        readThreads = new long[2];
                        readThreads[0] = tid;
                        readThreadMap.put(SID, readThreads);
                    } else {
                        if (readThreads[0] != tid)
                            readThreads[1] = tid;

                    }
                }
            }
        }
    }

    public static void logFieldAcc(int ID, final Object o, int SID, final Object v,
            final boolean write) {
        // shared object reference variable deference
        // make it as a branch event

        int hashcode_o = System.identityHashCode(o);
        db.saveEvent(write ? EventType.WRITE : EventType.READ, ID, o == null ? 0 : hashcode_o, -SID,
                longOfObject(v));
        if (!isPrim(v)) {
            logBranch(-1);
        }
    }

    public static void logInitialWrite(int ID, final Object o, int index, final Object v) {
        db.saveEvent(EventType.INIT, ID, o == null ? 0 : System.identityHashCode(o),
                index, longOfObject(v));
    }

    public static void logArrayAcc(int ID, final Object o, int index, final boolean write) {
        long tid = Thread.currentThread().getId();

        // StringBuilder builder = new StringBuilder(20);
        // builder.append(ID).append('.').append(sig);
        // String identifier = builder.toString();

        // String identifier = ID+"."+sig;
        // System.out.println(identifier);
        if (!threadLocalIDSet.get().contains(ID)) {
            if (threadLocalIDSet2.get().contains(ID))
                threadLocalIDSet.get().add(ID);
            else
                threadLocalIDSet2.get().add(ID);

            Integer sig = System.identityHashCode(o);// +"_"+index;//array

            HashSet<Integer> ids = arrayIdsMap.get(sig);
            if (ids == null) {
                ids = new HashSet<Integer>();
                arrayIdsMap.put(sig, ids);
            }
            ids.add(ID);
            if (Config.instance.verbose) {
                String readOrWrite = (write ? " write" : " read");
                System.out.println("Thread " + tid + " " + readOrWrite + " array "
                        + GlobalStateForInstrumentation.instance.getArrayLocationSig(ID));
            }
            if (!sharedArrayIds.contains(sig)) {
                if (writeThreadArrayMap.containsKey(sig)) {
                    if (writeThreadArrayMap.get(sig) != tid) {
                        sharedArrayIds.add(sig);
                        return;
                    }
                }

                if (write)// write
                {
                    if (readThreadArrayMap.containsKey(sig)) {
                        long[] readThreads = readThreadArrayMap.get(sig);
                        if (readThreads != null
                                && (readThreads[0] != tid || (readThreads[1] > 0 && readThreads[1] != tid))) {
                            sharedArrayIds.add(sig);
                            return;
                        }
                    }

                    writeThreadArrayMap.put(sig, tid);
                } else// read
                {
                    long[] readThreads = readThreadArrayMap.get(sig);

                    if (readThreads == null) {
                        readThreads = new long[2];
                        readThreads[0] = tid;
                        readThreadArrayMap.put(sig, readThreads);
                    } else {
                        if (readThreads[0] != tid)
                            readThreads[1] = tid;

                    }
                }
            }
        }
    }

    public static void logArrayAcc(int ID, final Object o, int index, final Object v,
            final boolean write) {
        db.saveEvent(write ? EventType.WRITE : EventType.READ, ID, System.identityHashCode(o), index, longOfObject(v));
    }

    private static boolean isPrim(Object o) {
        if (o instanceof Integer || o instanceof Long || o instanceof Byte || o instanceof Boolean
                || o instanceof Float || o instanceof Double || o instanceof Short
                || o instanceof Character)
            return true;

        return false;
    }

    private static long longOfObject(Object o) {
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof Long) return (Long) o;
        if (o instanceof Byte) return (Byte) o;
        if (o instanceof Boolean) return ((Boolean) o).booleanValue() ? 1 : 0;
        if (o instanceof Float) return Float.floatToRawIntBits((Float) o);
        if (o instanceof Double) return Double.doubleToRawLongBits((Double) o);
        if (o instanceof Short) return (Short) o;
        if (o instanceof Character) return ((Character) o);
        return System.identityHashCode(o);
    }

    /**
     * When starting a new thread, a consistent unique identifier of the thread
     * is created, and stored into a map with the thread id as the key. The
     * unique identifier, i.e, name, is a concatenation of the name of the
     * parent thread with the order of children threads forked by the parent
     * thread.
     *
     * @param ID
     * @param o
     */
    public static void logStart(int ID, final Object o) {
        long tid = Thread.currentThread().getId();
        Thread t = (Thread) o;
        long tid_t = t.getId();

        String name = GlobalStateForInstrumentation.instance.threadTidNameMap.get(tid);
        // it's possible that name is NULL, because this thread is started from
        // library: e.g., AWT-EventQueue-0
        if (name == null) {
            name = Thread.currentThread().getName();
            threadTidIndexMap.put(tid, 1);
            GlobalStateForInstrumentation.instance.registerThreadName(tid, name);
        }

        int index = threadTidIndexMap.get(tid);

        if (name.equals(MAIN_NAME))
            name = "" + index;
        else
            name = name + "." + index;

        GlobalStateForInstrumentation.instance.registerThreadName(tid_t, name);
        threadTidIndexMap.put(tid_t, 1);

        index++;
        threadTidIndexMap.put(tid, index);

        db.saveEvent(EventType.START, ID, tid_t, 0, 0);
    }

    public static void logJoin(int ID, final Object o) {
        db.saveEvent(EventType.JOIN, ID, ((Thread) o).getId(), 0, 0);
    }

}
