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

import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import rvpredict.config.Config;
import rvpredict.instrumentation.GlobalStateForInstrumentation;
import rvpredict.trace.EventType;

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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveMetaData(DBEngine db, GlobalStateForInstrumentation state) {
        ConcurrentHashMap<Long, String> threadTidMap = state.threadIdToName;
        ConcurrentHashMap<String, Integer> variableIdMap = state.varSigToId;
        Set<String> volatileVariables = state.volatileVariables;
        ConcurrentHashMap<String, Integer> stmtSigIdMap = state.stmtSigIdMap;
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
        Iterator<String> volatileIt = volatileVariables.iterator();
        while (volatileIt.hasNext()) {
            String sig = volatileIt.next();
            volatileIt.remove();
            Integer id = GlobalStateForInstrumentation.instance.varSigToId.get(sig);
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

    /**
     * Logs the {@code BRANCH} event produced by jmp or tableswitch
     * instructions.
     *
     * @param locId
     *            the location identifier of the event
     */
    public static void logBranch(int locId) {
        db.saveEvent(EventType.BRANCH, locId);
    }

    /**
     * Logs the {@code WAIT} event produced by invoking {@code object.wait()}.
     *
     * @param locId
     *            the location identifier of the event
     * @param object
     *            the {@code Object} whose {@code wait()} method is invoked
     */
    public static void logWait(int locId, Object object) {
        db.saveEvent(EventType.WAIT, locId, System.identityHashCode(object));
    }

    /**
     * Logs the {@code NOTIFY} event produced by invoking
     * {@code object.notify()}.
     *
     * @param locId
     *            the location identifier of the event
     * @param object
     *            the {@code Object} whose {@code notify()} method is invoked
     */
    public static void logNotify(int locId, Object object) {
        db.saveEvent(EventType.NOTIFY, locId, System.identityHashCode(object));
    }

    /**
     * Logs the {@code LOCK} event produced by entering block synchronized with
     * {@code object}'s intrinsic lock.
     *
     * @param locId
     *            the location identifier of the event
     * @param object
     *            the {@code Object} whose intrinsic lock is acquired
     */
    public static void logLock(int locId, Object object) {
        db.saveEvent(EventType.LOCK, locId, System.identityHashCode(object));
    }

    /**
     * Logs the {@code UNLOCK} event produced by exiting block synchronized with
     * {@code object}'s intrinsic lock.
     *
     * @param locId
     *            the location identifier of the event
     * @param object
     *            the {@code Object} whose intrinsic lock is released
     */
    public static void logUnlock(int locId, Object object) {
        db.saveEvent(EventType.UNLOCK, locId, System.identityHashCode(object));
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

    /**
     * Logs the {@code READ/WRITE} event produced by field access.
     *
     * @param locId
     *            the location identifier of the event
     * @param object
     *            the owner object of the field; {@code null} when accessing static fields
     * @param variableId
     *            the variable identifier of the field
     * @param value
     *            the value written by the write access or the value read by the
     *            read access
     * @param isWrite
     *            specifies if it is a write access
     */
    public static void logFieldAcc(int locId, Object object, int variableId, Object value,
            boolean isWrite) {
        db.saveEvent(isWrite ? EventType.WRITE : EventType.READ, locId,
                System.identityHashCode(object), -variableId, objectToLong(value));
        if (!isPrimitiveWrapper(value)) {
            // TODO(YilongL): what does it mean?
            // shared object reference variable deference
            // make it as a branch event
            logBranch(-1);
        }
    }

    /**
     * Logs the {@code INIT} event produced by initializing a field or an array
     * element.
     *
     * @param locId
     *            the location identifier of the event
     * @param object
     *            the array or the owner object of the field; {@code null} when
     *            initializing static field
     * @param arrayIndexOrVarId
     *            the array index or the variable identifier
     * @param value
     *            the initial value of the field or the element
     */
    public static void logInitialWrite(int locId, Object object, int arrayIndexOrVarId, Object value) {
        db.saveEvent(EventType.INIT, locId, System.identityHashCode(object), arrayIndexOrVarId,
                objectToLong(value));
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

    /**
     * Logs the {@code READ/WRITE} event produced by array access.
     *
     * @param locId
     *            the location identifier of the event
     * @param array
     *            the array to access
     * @param index
     *            the array index
     * @param value
     *            the value written by the write access or the value read by the
     *            read access
     * @param isWrite
     *            specifies if it is a write access
     */
    public static void logArrayAcc(int locId, Object array, int index, Object value, boolean isWrite) {
        db.saveEvent(isWrite ? EventType.WRITE : EventType.READ, locId,
                System.identityHashCode(array), index, objectToLong(value));
    }

    /**
     * Logs the {@code START} event produced by invoking {@code thread.start()}.
     *
     * When starting a new thread, a consistent unique identifier of the thread
     * is created, and stored into a map with the thread id as the key. The
     * unique identifier, i.e, name, is a concatenation of the name of the
     * parent thread with the order of children threads forked by the parent
     * thread.
     *
     * @param locId
     *            the location identifier of the event
     * @param thread
     *            the {@code Thread} object whose {@code start()} method is
     *            invoked
     */
    public static void logStart(int locId, Object thread) {
        long crntThreadId = Thread.currentThread().getId();
        long newThreadId = ((Thread) thread).getId();

        String name = GlobalStateForInstrumentation.instance.threadIdToName.get(crntThreadId);
        // it's possible that name is NULL, because this thread is started from
        // library: e.g., AWT-EventQueue-0
        if (name == null) {
            name = Thread.currentThread().getName();
            threadTidIndexMap.put(crntThreadId, 1);
            GlobalStateForInstrumentation.instance.registerThreadName(crntThreadId, name);
        }

        int index = threadTidIndexMap.get(crntThreadId);

        if (name.equals(MAIN_NAME))
            name = "" + index;
        else
            name = name + "." + index;

        GlobalStateForInstrumentation.instance.registerThreadName(newThreadId, name);
        threadTidIndexMap.put(newThreadId, 1);

        index++;
        threadTidIndexMap.put(crntThreadId, index);

        db.saveEvent(EventType.START, locId, newThreadId);
    }

    /**
     * Logs the {@code JOIN} event produced by invoking {@code thread.join()}.
     *
     * @param locId
     *            the location identifier of the event
     * @param thread
     *            the {@code Thread} object whose {@code join()} method is
     *            invoked
     */
    public static void logJoin(int locId, Object thread) {
        db.saveEvent(EventType.JOIN, locId, ((Thread) thread).getId());
    }

    private static boolean isPrimitiveWrapper(Object o) {
        /* YilongL: we do not use guava's `Primitives.isWrapperType' because o could be null */
        return o instanceof Integer || o instanceof Long || o instanceof Byte
                || o instanceof Boolean || o instanceof Float || o instanceof Double
                || o instanceof Short || o instanceof Character;
    }

    private static long objectToLong(Object o) {
        if (o instanceof Boolean) return ((Boolean) o).booleanValue() ? 1 : 0;
        if (o instanceof Byte) return (Byte) o;
        if (o instanceof Character) return ((Character) o);
        if (o instanceof Short) return (Short) o;
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof Long) return (Long) o;
        if (o instanceof Float) return Float.floatToRawIntBits((Float) o);
        if (o instanceof Double) return Double.doubleToRawLongBits((Double) o);
        return System.identityHashCode(o);
    }

}
