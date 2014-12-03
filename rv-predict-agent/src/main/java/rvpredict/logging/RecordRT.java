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

import java.util.HashMap;
import rvpredict.instrumentation.GlobalStateForInstrumentation;
import rvpredict.trace.EventType;

public final class RecordRT {

    private static HashMap<Long, Integer> threadTidIndexMap;
    private final static String MAIN_NAME = "0";

    private static DBEngine db;

    // TODO(YilongL): move this method out of the runtime library
    public static void init(DBEngine db) {
        RecordRT.db = db;
        initNonSharing();
    }

    // TODO(YilongL): move this method out of the runtime library
    public static void initNonSharing() {
        long tid = Thread.currentThread().getId();

        GlobalStateForInstrumentation.instance.registerThreadName(tid, MAIN_NAME);

        threadTidIndexMap = new HashMap<>();
        threadTidIndexMap.put(tid, 1);
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
