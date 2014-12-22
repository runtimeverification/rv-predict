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
     * Logs events produced by invoking {@code object.wait()}.
     *
     * @param locId
     *            the location identifier of the event
     * @param object
     *            the {@code Object} whose {@code wait()} method is invoked
     */
    public static void rvPredictWait(int locId, Object object) throws InterruptedException {
        rvPredictWait(locId, object, 0);
    }

    /**
     * Logs events produced by invoking {@code object.wait(long)}.
     *
     * @param locId
     *            the location identifier of the event
     * @param object
     *            the {@code Object} whose {@code wait(long)} method is invoked
     * @param timeout
     *            the first argument of {@code object.wait(long)}
     */
    public static void rvPredictWait(int locId, Object object, long timeout)
            throws InterruptedException {
        int objectHashCode = System.identityHashCode(object);
        db.saveEvent(EventType.PRE_WAIT, locId, objectHashCode);
        try {
            object.wait(timeout);
        } catch (InterruptedException e) {
            /* clear interrupted status */
            db.saveEvent(EventType.WRITE, locId, System.identityHashCode(Thread.currentThread()),
                    GlobalStateForInstrumentation.NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
            db.saveEvent(EventType.WAIT_INTERRUPTED, locId, objectHashCode);
            throw e;
        }
        db.saveEvent(timeout > 0 ? EventType.WAIT_MAYBE_TIMEOUT : EventType.WAIT, locId, objectHashCode);
    }

    /**
     * Logs events produced by invoking {@code object.wait(long, int)}.
     *
     * @param locId
     *            the location identifier of the event
     * @param object
     *            the {@code Object} whose {@code wait(long, int)} method is
     *            invoked
     * @param timeout
     *            the first argument of {@code object.wait(long, int)}
     * @param nano
     *            the second argument of {@code object.wait(long, int)}
     */
    public static void rvPredictWait(int locId, Object object, long timeout, int nano)
            throws InterruptedException {
        int objectHashCode = System.identityHashCode(object);
        db.saveEvent(EventType.PRE_WAIT, locId, objectHashCode);
        try {
            object.wait(timeout, nano);
        } catch (InterruptedException e) {
            /* clear interrupted status */
            db.saveEvent(EventType.WRITE, locId, System.identityHashCode(Thread.currentThread()),
                    GlobalStateForInstrumentation.NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
            db.saveEvent(EventType.WAIT_INTERRUPTED, locId, objectHashCode);
            throw e;
        }
        db.saveEvent(timeout > 0 || nano > 0 ? EventType.WAIT_MAYBE_TIMEOUT : EventType.WAIT, locId,
                objectHashCode);
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
    public static void rvPredictNotify(int locId, Object object) {
        db.saveEvent(EventType.NOTIFY, locId, System.identityHashCode(object));
        object.notify();
    }

    /**
     * Logs the {@code NOTIFY_ALL} event produced by invoking
     * {@code object.notifyAll()}.
     *
     * @param locId
     *            the location identifier of the event
     * @param object
     *            the {@code Object} whose {@code notifyAll()} method is invoked
     */
    public static void rvPredictNotifyAll(int locId, Object object) {
        db.saveEvent(EventType.NOTIFY_ALL, locId, System.identityHashCode(object));
        object.notifyAll();
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
     * @param branchModel
     *            specifies if we use branch model
     */
    public static void logFieldAcc(int locId, Object object, int variableId, Object value,
            boolean isWrite, boolean branchModel) {
        db.saveEvent(isWrite ? EventType.WRITE : EventType.READ, locId,
                System.identityHashCode(object), -variableId, objectToLong(value));
        if (!isPrimitiveWrapper(value) && branchModel) {
            // TODO(YilongL): what does it mean?
            // shared object reference variable deference
            // make it as a branch event
            logBranch(-1);
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
     * Logs the {@code INIT} event produced by initializing a field.
     *
     * @param locId
     *            the location identifier of the event
     * @param object
     *            the owner object of the field; {@code null} when initializing
     *            static field
     * @param variableId
     *            the variable identifier
     * @param value
     *            the initial value of the field
     */
    public static void logFieldInit(int locId, Object object, int variableId, Object value) {
        db.saveEvent(EventType.INIT, locId, System.identityHashCode(object), -variableId,
                objectToLong(value));
    }

    /**
     * Logs the {@code INIT} event produced by initializing an array element.
     *
     * @param locId
     *            the location identifier of the event
     * @param object
     *            the array of the field
     * @param index
     *            the array index
     * @param value
     *            the initial value of the element
     */
    public static void logArrayInit(int locId, Object array, int index, Object value) {
        db.saveEvent(EventType.INIT, locId, System.identityHashCode(array), index,
                objectToLong(value));
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
    public static void rvPredictStart(int locId, Thread thread) {
        long crntThreadId = Thread.currentThread().getId();
        long newThreadId = thread.getId();

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

        thread.start();
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
    public static void rvPredictJoin(int locId, Thread thread) throws InterruptedException {
        rvPredictJoin(locId, thread, 0);
    }

    /**
     * Logs the {@code JOIN} event produced by invoking
     * {@code thread.join(long)}.
     *
     * @param locId
     *            the location identifier of the event
     * @param thread
     *            the {@code Thread} object whose {@code join(long)} method is
     *            invoked
     * @param millis
     *            the first argument of {@code thread.join(long)}
     */
    public static void rvPredictJoin(int locId, Thread thread, long millis)
            throws InterruptedException {
        db.saveEvent(EventType.PRE_JOIN, locId, thread.getId());
        try {
            thread.join(millis);
        } catch (InterruptedException e) {
            /* clear interrupted status */
            db.saveEvent(EventType.WRITE, locId, System.identityHashCode(Thread.currentThread()),
                    GlobalStateForInstrumentation.NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
            db.saveEvent(EventType.JOIN_INTERRUPTED, locId, thread.getId());
            throw e;
        }
        db.saveEvent(millis == 0 ? EventType.JOIN : EventType.JOIN_MAYBE_TIMEOUT, locId, thread.getId());
    }

    /**
     * Logs the {@code JOIN} event produced by invoking
     * {@code thread.join(long, int)}.
     *
     * @param locId
     *            the location identifier of the event
     * @param thread
     *            the {@code Thread} object whose {@code join(long, int)} method
     *            is invoked
     * @param millis
     *            the first argument of {@code thread.join(long, int)}
     * @param nanos
     *            the second argument of {@code thread.join(long, int)}
     *
     */
    public static void rvPredictJoin(int locId, Thread thread, long millis, int nanos)
            throws InterruptedException {
        db.saveEvent(EventType.PRE_JOIN, locId, thread.getId());
        try {
            thread.join(millis, nanos);
        } catch (InterruptedException e) {
            /* clear interrupted status */
            db.saveEvent(EventType.WRITE, locId, System.identityHashCode(Thread.currentThread()),
                    GlobalStateForInstrumentation.NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
            db.saveEvent(EventType.JOIN_INTERRUPTED, locId, thread.getId());
            throw e;
        }
        db.saveEvent(millis == 0 && nanos == 0 ? EventType.JOIN : EventType.JOIN_MAYBE_TIMEOUT, locId,
                thread.getId());
    }

    /**
     * Logs the events produced by invoking {@code thread.interrupt()}.
     *
     * @param locId
     *            the location identifier of the event
     * @param thread
     *            the {@code Thread} object whose {@code interrupt()} method is
     *            invoked
     */
    public static void rvPredictInterrupt(int locId, Thread thread) {
        try {
            /* YilongL: call checkAccess first because 1) INTERRUPT event should
             * be logged before calling interrupt() and 2) no INTERRUPT event
             * should be recorded on SecurityException */
            thread.checkAccess();
            /* TODO(YilongL): Interrupting a thread that is not alive need not
             * have any effect; yet I am not sure how to model such case
             * precisely so I just assume interrupted status will be set to true */
            db.saveEvent(EventType.WRITE, locId, System.identityHashCode(thread),
                    GlobalStateForInstrumentation.NATIVE_INTERRUPTED_STATUS_VAR_ID, 1);
            db.saveEvent(EventType.INTERRUPT, locId, thread.getId());
            thread.interrupt();
        } catch (SecurityException e) {
            throw e;
        }
    }

    /**
     * Logs the events produced by invoking {@code thread.isInterrupted()}.
     *
     * @param locId
     *            the location identifier of the event
     * @param thread
     *            the {@code Thread} object whose {@code isInterrupted()} method is
     *            invoked
     */
    public static boolean rvPredictIsInterrupted(int locId, Thread thread) {
        boolean isInterrupted = thread.isInterrupted();
        /* the interrupted status is like an imaginary shared variable so we
         * need to record access to it to preserve soundness */
        db.saveEvent(EventType.READ, locId, System.identityHashCode(thread),
                GlobalStateForInstrumentation.NATIVE_INTERRUPTED_STATUS_VAR_ID,
                isInterrupted ? 1 : 0);
        return isInterrupted;
    }

    /**
     * Logs the events produced by invoking {@code Thread#interrupted()}.
     *
     * @param locId
     *            the location identifier of the event
     */
    public static boolean rvPredictInterrupted(int locId) {
        boolean interrupted = Thread.interrupted();
        db.saveEvent(EventType.READ, locId, 0,
                GlobalStateForInstrumentation.NATIVE_INTERRUPTED_STATUS_VAR_ID, interrupted ? 1 : 0);
        /* clear interrupted status */
        db.saveEvent(EventType.WRITE, locId, System.identityHashCode(Thread.currentThread()),
                GlobalStateForInstrumentation.NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
        return interrupted;
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
