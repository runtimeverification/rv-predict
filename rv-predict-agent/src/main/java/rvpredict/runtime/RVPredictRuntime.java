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
package rvpredict.runtime;

import static rvpredict.instrumentation.MetaData.NATIVE_INTERRUPTED_STATUS_VAR_ID;

import java.util.Set;

import rvpredict.instrumentation.MetaData;
import rvpredict.logging.DBEngine;
import rvpredict.trace.EventType;

public final class RVPredictRuntime {

    private static DBEngine db;

    // TODO(YilongL): move this method out of the runtime library
    public static void init(DBEngine db) {
        RVPredictRuntime.db = db;
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
        Thread crntThread = Thread.currentThread();
        int objectHashCode = System.identityHashCode(object);
        db.saveEvent(EventType.PRE_WAIT, locId, objectHashCode);
        try {
            object.wait(timeout);
        } catch (InterruptedException e) {
            synchronized (crntThread) {
                /* require interrupted status to be true at the moment */
                db.saveEvent(EventType.READ, locId, System.identityHashCode(crntThread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 1);
                /* clear interrupted status */
                db.saveEvent(EventType.WRITE, locId, System.identityHashCode(crntThread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
                db.saveEvent(EventType.WAIT_INTERRUPTED, locId, objectHashCode);
            }
            throw e;
        }

        db.saveEvent(EventType.READ, locId, System.identityHashCode(crntThread),
                -NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
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
        Thread crntThread = Thread.currentThread();
        int objectHashCode = System.identityHashCode(object);
        db.saveEvent(EventType.PRE_WAIT, locId, objectHashCode);
        try {
            object.wait(timeout, nano);
        } catch (InterruptedException e) {
            synchronized (crntThread) {
                /* require interrupted status to be true at the moment */
                db.saveEvent(EventType.READ, locId, System.identityHashCode(crntThread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 1);
                /* clear interrupted status */
                db.saveEvent(EventType.WRITE, locId, System.identityHashCode(crntThread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
                db.saveEvent(EventType.WAIT_INTERRUPTED, locId, objectHashCode);
            }
            throw e;
        }

        db.saveEvent(EventType.READ, locId, System.identityHashCode(crntThread),
                -NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
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
        variableId = RVPredictRuntime.resolveVariableId(variableId);
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
        variableId = RVPredictRuntime.resolveVariableId(variableId);
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
        db.saveEvent(EventType.INIT, locId, System.identityHashCode(thread),
                -NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
        db.saveEvent(EventType.START, locId, thread.getId());
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
        Thread crntThread = Thread.currentThread();
        db.saveEvent(EventType.PRE_JOIN, locId, thread.getId());
        try {
            thread.join(millis);
        } catch (InterruptedException e) {
            synchronized (crntThread) {
                /* require interrupted status to be true at the moment */
                db.saveEvent(EventType.READ, locId, System.identityHashCode(crntThread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 1);
                /* clear interrupted status */
                db.saveEvent(EventType.WRITE, locId, System.identityHashCode(crntThread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
                db.saveEvent(EventType.JOIN_INTERRUPTED, locId, thread.getId());
            }
            throw e;
        }

        db.saveEvent(EventType.READ, locId, System.identityHashCode(crntThread),
                -NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
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
        Thread crntThread = Thread.currentThread();
        db.saveEvent(EventType.PRE_JOIN, locId, thread.getId());
        try {
            thread.join(millis, nanos);
        } catch (InterruptedException e) {
            synchronized (crntThread) {
                /* require interrupted status to be true at the moment */
                db.saveEvent(EventType.READ, locId, System.identityHashCode(crntThread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 1);
                /* clear interrupted status */
                db.saveEvent(EventType.WRITE, locId, System.identityHashCode(crntThread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
                db.saveEvent(EventType.JOIN_INTERRUPTED, locId, thread.getId());
            }
            throw e;
        }

        db.saveEvent(EventType.READ, locId, System.identityHashCode(crntThread),
                -NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
        db.saveEvent(millis == 0 && nanos == 0 ? EventType.JOIN : EventType.JOIN_MAYBE_TIMEOUT, locId,
                thread.getId());
    }

    /**
     * Logs the events produced by invoking {@code Thread#sleep(long)}.
     *
     * @param locId
     *            the location identifier of the event
     * @param millis
     *            the first argument of {@code Thread#sleep(long)}
     */
    public static void rvPredictSleep(int locId, long millis) throws InterruptedException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread crntThread = Thread.currentThread();
            synchronized (crntThread) {
                /* require interrupted status to be true at the moment */
                db.saveEvent(EventType.READ, locId, System.identityHashCode(crntThread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 1);
                /* clear interrupted status */
                db.saveEvent(EventType.WRITE, locId, System.identityHashCode(crntThread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
            }
            throw e;
        }
    }

    /**
     * Logs the events produced by invoking {@code Thread#sleep(long, int)}.
     *
     * @param locId
     *            the location identifier of the event
     * @param millis
     *            the first argument of {@code Thread#sleep(long, int)}
     * @param nanos
     *            the second argument of {@code Thread#sleep(long, int)}
     */
    public static void rvPredictSleep(int locId, long millis, int nanos)
            throws InterruptedException {
        try {
            Thread.sleep(millis, nanos);
        } catch (InterruptedException e) {
            Thread crntThread = Thread.currentThread();
            synchronized (crntThread) {
                /* require interrupted status to be true at the moment */
                db.saveEvent(EventType.READ, locId, System.identityHashCode(crntThread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 1);
                /* clear interrupted status */
                db.saveEvent(EventType.WRITE, locId, System.identityHashCode(crntThread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
            }
            throw e;
        }
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
            /* YilongL: the synchronization block here is not trying to
             * (hopelessly) ensure read-write consistency or data-race w.r.t the
             * interrupted status; it's meant to at least preserve the more
             * important constraint that interrupt should happen before the
             * blocking method throwing an InterruptedException */

            /* make sure the write on interrupted status is logged before the
             * read generated by blocking method */
            synchronized (thread) {
                /* YilongL: conceptually speaking, INTERRUPT should be logged before
                 * calling thread.interrupt(); however, it is also required that no
                 * event is logged on SecurityException; so we make the logging of
                 * all interrupt-related events synchronize on the interrupted
                 * thread object */
                thread.interrupt();
                /* TODO(YilongL): Interrupting a thread that is not alive need not
                 * have any effect; yet I am not sure how to model such case
                 * precisely so I just assume interrupted status will be set to true */
                db.saveEvent(EventType.WRITE, locId, System.identityHashCode(thread),
                        -NATIVE_INTERRUPTED_STATUS_VAR_ID, 1);
            }
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
                -NATIVE_INTERRUPTED_STATUS_VAR_ID, isInterrupted ? 1 : 0);
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
        db.saveEvent(EventType.READ, locId, 0, -NATIVE_INTERRUPTED_STATUS_VAR_ID,
                interrupted ? 1 : 0);
        /* clear interrupted status */
        db.saveEvent(EventType.WRITE, locId, System.identityHashCode(Thread.currentThread()),
                -NATIVE_INTERRUPTED_STATUS_VAR_ID, 0);
        return interrupted;
    }

    /**
     * Logs the events produced by invoking {@code System#arraycopy(Object, int, Object, int, int)}.
     *
     * @param locId
     *            the location identifier of the event
     */
    public static void rvPredictSystemArraycopy(int locId, Object src, int srcPos, Object dest,
            int destPos, int length) {
        // 8 primitive types: boolean, byte, char, short, int, long, float, and double

        if (srcPos >= 0 && destPos >=0 && length > 0) {
            if (src instanceof Object[]) {
                if (dest instanceof Object[]) {
                    if (srcPos + length <= ((Object[]) src).length
                            && destPos + length <= ((Object[]) dest).length) {
                        int k = length;
                        for (int i = 0; i < length; i++) {
                            Object srcObj = ((Object[]) src)[i + srcPos];
                            if (srcObj == null
                                    || dest.getClass().getComponentType()
                                            .isAssignableFrom(srcObj.getClass())) {
                                logArrayAcc(locId, src, srcPos + i, srcObj, false);
                            } else {
                                k = i;
                                break;
                            }
                        }
                        for (int i = 0; i < k; i++) {
                            logArrayAcc(locId, dest, destPos + i, ((Object[]) src)[i + srcPos], true);
                        }
                    }
                }
            } else if (src instanceof boolean[]) {
                if (dest instanceof boolean[]) {
                    if (srcPos + length <= ((boolean[]) src).length
                            && destPos + length <= ((boolean[]) dest).length) {
                        for (int i = srcPos; i < srcPos + length; i++) {
                            logArrayAcc(locId, src, i, ((boolean[]) src)[i], false);
                        }
                        for (int i = destPos; i < destPos + length; i++) {
                            logArrayAcc(locId, dest, i, ((boolean[]) src)[i - destPos + srcPos], true);
                        }
                    }
                }
            } else if (src instanceof byte[]) {
                if (dest instanceof byte[]) {
                    if (srcPos + length <= ((byte[]) src).length
                            && destPos + length <= ((byte[]) dest).length) {
                        for (int i = srcPos; i < srcPos + length; i++) {
                            logArrayAcc(locId, src, i, ((byte[]) src)[i], false);
                        }
                        for (int i = destPos; i < destPos + length; i++) {
                            logArrayAcc(locId, dest, i, ((byte[]) src)[i - destPos + srcPos], true);
                        }
                    }
                }
            } else if (src instanceof char[]) {
                if (dest instanceof char[]) {
                    if (srcPos + length <= ((char[]) src).length
                            && destPos + length <= ((char[]) dest).length) {
                        for (int i = srcPos; i < srcPos + length; i++) {
                            logArrayAcc(locId, src, i, ((char[]) src)[i], false);
                        }
                        for (int i = destPos; i < destPos + length; i++) {
                            logArrayAcc(locId, dest, i, ((char[]) src)[i - destPos + srcPos], true);
                        }
                    }
                }
            } else if (src instanceof short[]) {
                if (dest instanceof short[]) {
                    if (srcPos + length <= ((short[]) src).length
                            && destPos + length <= ((short[]) dest).length) {
                        for (int i = srcPos; i < srcPos + length; i++) {
                            logArrayAcc(locId, src, i, ((short[]) src)[i], false);
                        }
                        for (int i = destPos; i < destPos + length; i++) {
                            logArrayAcc(locId, dest, i, ((short[]) src)[i - destPos + srcPos], true);
                        }
                    }
                }
            } else if (dest instanceof int[]) {
                if (srcPos + length <= ((int[]) src).length
                        && destPos + length <= ((int[]) dest).length) {
                    for (int i = srcPos; i < srcPos + length; i++) {
                        logArrayAcc(locId, src, i, ((int[]) src)[i], false);
                    }
                    for (int i = destPos; i < destPos + length; i++) {
                        logArrayAcc(locId, dest, i, ((int[]) src)[i - destPos + srcPos], true);
                    }
                }
            } else if (src instanceof long[]) {
                if (dest instanceof long[]) {
                    if (srcPos + length <= ((long[]) src).length
                            && destPos + length <= ((long[]) dest).length) {
                        for (int i = srcPos; i < srcPos + length; i++) {
                            logArrayAcc(locId, src, i, ((long[]) src)[i], false);
                        }
                        for (int i = destPos; i < destPos + length; i++) {
                            logArrayAcc(locId, dest, i, ((long[]) src)[i - destPos + srcPos], true);
                        }
                    }
                }
            } else if (src instanceof float[]) {
                if (dest instanceof float[]) {
                    if (srcPos + length <= ((float[]) src).length
                            && destPos + length <= ((float[]) dest).length) {
                        for (int i = srcPos; i < srcPos + length; i++) {
                            logArrayAcc(locId, src, i, ((float[]) src)[i], false);
                        }
                        for (int i = destPos; i < destPos + length; i++) {
                            logArrayAcc(locId, dest, i, ((float[]) src)[i - destPos + srcPos], true);
                        }
                    }
                }
            } else if (src instanceof double[]) {
                if (dest instanceof double[]) {
                    if (srcPos + length <= ((double[]) src).length
                            && destPos + length <= ((double[]) dest).length) {
                        for (int i = srcPos; i < srcPos + length; i++) {
                            logArrayAcc(locId, src, i, ((double[]) src)[i], false);
                        }
                        for (int i = destPos; i < destPos + length; i++) {
                            logArrayAcc(locId, dest, i, ((double[]) src)[i - destPos + srcPos], true);
                        }
                    }
                }
            }
        }

        System.arraycopy(src, srcPos, dest, destPos, length);
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

    /**
     * TODO(YilongL): doing name mangling at runtime introduce unnecessary
     * dependency on ConcurrentHashMap and Collections.newSetFromMap
     */
    private static int resolveVariableId(int variableId) {
        String varSig = MetaData.varSigs[variableId];
        int idx = varSig.lastIndexOf(".");
        String className = varSig.substring(0, idx);
        String fieldName = varSig.substring(idx + 1);
        Set<String> fieldNames = MetaData.classNameToFieldNames.get(className);
        while (fieldNames != null && !fieldNames.contains(fieldName)) {
            className = MetaData.classNameToSuperclassName.get(className);
            if (className == null) {
                fieldNames = null;
                break;
            }

            fieldNames = MetaData.classNameToFieldNames.get(className);
        }

        if (fieldNames == null) {
            /* failed to resolve this variable Id */
            // TODO(YilongL): make sure this doesn't happen

//            System.out.println("[Warning]: unable to retrieve field information of class "
//                    + className + "; resolving field " + fieldName);

            return variableId;
        } else {
            assert fieldNames.contains(fieldName);
            return MetaData.getVariableId(className, fieldName);
        }
    }

}
