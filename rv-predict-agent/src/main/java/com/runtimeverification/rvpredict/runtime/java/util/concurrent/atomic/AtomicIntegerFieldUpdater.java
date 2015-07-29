/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.runtimeverification.rvpredict.runtime.java.util.concurrent.atomic;
import java.util.function.IntUnaryOperator;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.runtime.RVPredictRuntime;

import java.util.function.IntBinaryOperator;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

/**
 * A reflection-based utility that enables atomic updates to
 * designated {@code volatile int} fields of designated classes.
 * This class is designed for use in atomic data structures in which
 * several fields of the same node are independently subject to atomic
 * updates.
 *
 * <p>Note that the guarantees of the {@code compareAndSet}
 * method in this class are weaker than in other atomic classes.
 * Because this class cannot ensure that all uses of the field
 * are appropriate for purposes of atomic access, it can
 * guarantee atomicity only with respect to other invocations of
 * {@code compareAndSet} and {@code set} on the same updater.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <T> The type of the object holding the updatable field
 */
public abstract class AtomicIntegerFieldUpdater<T> {
    /**
     * Creates and returns an updater for objects with the given field.
     * The Class argument is needed to check that reflective types and
     * generic types match.
     *
     * @param tclass the class of the objects holding the field
     * @param fieldName the name of the field to be updated
     * @param <U> the type of instances of tclass
     * @return the updater
     * @throws IllegalArgumentException if the field is not a
     * volatile integer type
     * @throws RuntimeException with a nested reflection-based
     * exception if the class does not hold field or is the wrong type,
     * or the field is inaccessible to the caller according to Java language
     * access control
     */
    @CallerSensitive
    public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> tclass,
                                                              String fieldName) {
        return new AtomicIntegerFieldUpdaterImpl<U>
            (tclass, fieldName, Reflection.getCallerClass());
    }

    /**
     * Protected do-nothing constructor for use by subclasses.
     */
    protected AtomicIntegerFieldUpdater() {
    }

    /**
     * Atomically sets the field of the given object managed by this updater
     * to the given updated value if the current value {@code ==} the
     * expected value. This method is guaranteed to be atomic with respect to
     * other calls to {@code compareAndSet} and {@code set}, but not
     * necessarily with respect to other changes in the field.
     *
     * @param obj An object whose field to conditionally set
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful
     * @throws ClassCastException if {@code obj} is not an instance
     * of the class possessing the field established in the constructor
     */
    public abstract boolean compareAndSet(T obj, int expect, int update);

    /**
     * Atomically sets the field of the given object managed by this updater
     * to the given updated value if the current value {@code ==} the
     * expected value. This method is guaranteed to be atomic with respect to
     * other calls to {@code compareAndSet} and {@code set}, but not
     * necessarily with respect to other changes in the field.
     *
     * <p><a href="package-summary.html#weakCompareAndSet">May fail
     * spuriously and does not provide ordering guarantees</a>, so is
     * only rarely an appropriate alternative to {@code compareAndSet}.
     *
     * @param obj An object whose field to conditionally set
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful
     * @throws ClassCastException if {@code obj} is not an instance
     * of the class possessing the field established in the constructor
     */
    public abstract boolean weakCompareAndSet(T obj, int expect, int update);

    /**
     * Sets the field of the given object managed by this updater to the
     * given updated value. This operation is guaranteed to act as a volatile
     * store with respect to subsequent invocations of {@code compareAndSet}.
     *
     * @param obj An object whose field to set
     * @param newValue the new value
     */
    public abstract void set(T obj, int newValue);

    /**
     * Eventually sets the field of the given object managed by this
     * updater to the given updated value.
     *
     * @param obj An object whose field to set
     * @param newValue the new value
     * @since 1.6
     */
    public abstract void lazySet(T obj, int newValue);

    /**
     * Gets the current value held in the field of the given object managed
     * by this updater.
     *
     * @param obj An object whose field to get
     * @return the current value
     */
    public abstract int get(T obj);

    /**
     * Atomically sets the field of the given object managed by this updater
     * to the given value and returns the old value.
     *
     * @param obj An object whose field to get and set
     * @param newValue the new value
     * @return the previous value
     */
    public int getAndSet(T obj, int newValue) {
        int prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    /**
     * Atomically increments by one the current value of the field of the
     * given object managed by this updater.
     *
     * @param obj An object whose field to get and set
     * @return the previous value
     */
    public int getAndIncrement(T obj) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev + 1;
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * Atomically decrements by one the current value of the field of the
     * given object managed by this updater.
     *
     * @param obj An object whose field to get and set
     * @return the previous value
     */
    public int getAndDecrement(T obj) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev - 1;
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * Atomically adds the given value to the current value of the field of
     * the given object managed by this updater.
     *
     * @param obj An object whose field to get and set
     * @param delta the value to add
     * @return the previous value
     */
    public int getAndAdd(T obj, int delta) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev + delta;
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * Atomically increments by one the current value of the field of the
     * given object managed by this updater.
     *
     * @param obj An object whose field to get and set
     * @return the updated value
     */
    public int incrementAndGet(T obj) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev + 1;
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * Atomically decrements by one the current value of the field of the
     * given object managed by this updater.
     *
     * @param obj An object whose field to get and set
     * @return the updated value
     */
    public int decrementAndGet(T obj) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev - 1;
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * Atomically adds the given value to the current value of the field of
     * the given object managed by this updater.
     *
     * @param obj An object whose field to get and set
     * @param delta the value to add
     * @return the updated value
     */
    public int addAndGet(T obj, int delta) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev + delta;
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * Atomically updates the field of the given object managed by this updater
     * with the results of applying the given function, returning the previous
     * value. The function should be side-effect-free, since it may be
     * re-applied when attempted updates fail due to contention among threads.
     *
     * @param obj An object whose field to get and set
     * @param updateFunction a side-effect-free function
     * @return the previous value
     * @since 1.8
     */
    public final int getAndUpdate(T obj, IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get(obj);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * Atomically updates the field of the given object managed by this updater
     * with the results of applying the given function, returning the updated
     * value. The function should be side-effect-free, since it may be
     * re-applied when attempted updates fail due to contention among threads.
     *
     * @param obj An object whose field to get and set
     * @param updateFunction a side-effect-free function
     * @return the updated value
     * @since 1.8
     */
    public final int updateAndGet(T obj, IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get(obj);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * Atomically updates the field of the given object managed by this
     * updater with the results of applying the given function to the
     * current and given values, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.  The
     * function is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param obj An object whose field to get and set
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @since 1.8
     */
    public final int getAndAccumulate(T obj, int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * Atomically updates the field of the given object managed by this
     * updater with the results of applying the given function to the
     * current and given values, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.  The
     * function is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param obj An object whose field to get and set
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     * @since 1.8
     */
    public final int accumulateAndGet(T obj, int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * Standard hotspot implementation using intrinsics
     */
    private static class AtomicIntegerFieldUpdaterImpl<T>
            extends AtomicIntegerFieldUpdater<T> {
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        private final long offset;
        private final Class<T> tclass;
        private final Class<?> cclass;

        private static final int RVPREDICT_ATOMIC_INTEGER_FIELD_UPDATER_LOC_ID = RVPredictRuntime.metadata
                .getLocationId("java.util.concurrent.atomic.AtomicIntegerFieldUpdater(AtomicIntegerFieldUpdater.java:n/a)");

        private final int _rvpredict_atom_int_field_id;

        AtomicIntegerFieldUpdaterImpl(final Class<T> tclass,
                                      final String fieldName,
                                      final Class<?> caller) {
            final Field field;
            final int modifiers;
            try {
                field = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Field>() {
                        @Override
                        public Field run() throws NoSuchFieldException {
                            return tclass.getDeclaredField(fieldName);
                        }
                    });
                modifiers = field.getModifiers();
                sun.reflect.misc.ReflectUtil.ensureMemberAccess(
                    caller, tclass, null, modifiers);
                ClassLoader cl = tclass.getClassLoader();
                ClassLoader ccl = caller.getClassLoader();
                if ((ccl != null) && (ccl != cl) &&
                    ((cl == null) || !isAncestor(cl, ccl))) {
                  sun.reflect.misc.ReflectUtil.checkPackageAccess(tclass);
                }
            } catch (PrivilegedActionException pae) {
                throw new RuntimeException(pae.getException());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            Class<?> fieldt = field.getType();
            if (fieldt != int.class)
                throw new IllegalArgumentException("Must be integer type");

            if (!Modifier.isVolatile(modifiers))
                throw new IllegalArgumentException("Must be volatile type");

            this.cclass = (Modifier.isProtected(modifiers) &&
                           caller != tclass) ? caller : null;
            this.tclass = tclass;
            offset = unsafe.objectFieldOffset(field);
            _rvpredict_atom_int_field_id = RVPredictRuntime.metadata.getVariableId(tclass.getName(),
                    fieldName);
        }

        /**
         * Returns true if the second classloader can be found in the first
         * classloader's delegation chain.
         * Equivalent to the inaccessible: first.isAncestor(second).
         */
        private static boolean isAncestor(ClassLoader first, ClassLoader second) {
            ClassLoader acl = first;
            do {
                acl = acl.getParent();
                if (second == acl) {
                    return true;
                }
            } while (acl != null);
            return false;
        }

        private void fullCheck(T obj) {
            System.out.println(tclass + " vs. " + obj);

            if (!tclass.isInstance(obj))
                throw new ClassCastException();
            if (cclass != null)
                ensureProtectedAccess(obj);
        }

        // RV-Predict logging methods

        private final Object _rvpredict_sync = new Object();

        private void _rvpredict_atomic_read(T obj, int value) {
            RVPredictRuntime.saveAtomicEvent(EventType.ATOMIC_READ,
                    RVPREDICT_ATOMIC_INTEGER_FIELD_UPDATER_LOC_ID, System.identityHashCode(obj),
                    -_rvpredict_atom_int_field_id, value, 0);
        }

        private void _rvpredict_atomic_write(T obj, int value) {
            RVPredictRuntime.saveAtomicEvent(EventType.ATOMIC_WRITE,
                    RVPREDICT_ATOMIC_INTEGER_FIELD_UPDATER_LOC_ID, System.identityHashCode(obj),
                    -_rvpredict_atom_int_field_id, value, 0);
        }

        private void _rvpredict_atomic_read_then_write(T obj, int oldValue, int newValue) {
            RVPredictRuntime.saveAtomicEvent(EventType.ATOMIC_READ_THEN_WRITE,
                    RVPREDICT_ATOMIC_INTEGER_FIELD_UPDATER_LOC_ID, System.identityHashCode(obj),
                    -_rvpredict_atom_int_field_id, oldValue, newValue);
        }

        private int _rvpredict_get_value(T obj) {
            synchronized (_rvpredict_sync) {
                int value = unsafe.getIntVolatile(obj, offset);
                _rvpredict_atomic_read(obj, value);
                return value;
            }
        }

        private void _rvpredict_set_value(T obj, int newValue) {
            _rvpredict_atomic_write(obj, newValue);
            unsafe.putIntVolatile(obj, offset, newValue);
        }

        private void _rvpredict_lazy_set_value(T obj, int newValue) {
            _rvpredict_atomic_write(obj, newValue);
            unsafe.putOrderedInt(obj, offset, newValue);
        }

        private boolean _rvpredict_compare_and_set_value(T obj, int expect, int update) {
            for (;;) {
                synchronized (_rvpredict_sync) {
                    if (unsafe.compareAndSwapInt(obj, offset, expect, update)) {
                        _rvpredict_atomic_read_then_write(obj, expect, update);
                        return true;
                    }
                }

                int actual = unsafe.getIntVolatile(obj, offset);
                if (expect != actual) {
                    _rvpredict_atomic_read(obj, actual);
                    return false;
                }
            }
        }

        private int _rvpredict_get_and_set_value(T obj, int newValue) {
            synchronized (_rvpredict_sync) {
                int oldValue = unsafe.getAndSetInt(obj, offset, newValue);
                _rvpredict_atomic_read_then_write(obj, oldValue, newValue);
                return oldValue;
            }
        }

        private int _rvpredict_get_and_add_value(T obj, int delta) {
            synchronized (_rvpredict_sync) {
                int oldValue = unsafe.getAndAddInt(obj, offset, delta);
                _rvpredict_atomic_read_then_write(obj, oldValue, oldValue + delta);
                return oldValue;
            }
        }

        @Override
        public boolean compareAndSet(T obj, int expect, int update) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return _rvpredict_compare_and_set_value(obj, expect, update);
        }

        @Override
        public boolean weakCompareAndSet(T obj, int expect, int update) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return _rvpredict_compare_and_set_value(obj, expect, update);
        }

        @Override
        public void set(T obj, int newValue) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            _rvpredict_set_value(obj, newValue);
        }

        @Override
        public void lazySet(T obj, int newValue) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            _rvpredict_lazy_set_value(obj, newValue);
        }

        @Override
        public final int get(T obj) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return _rvpredict_get_value(obj);
        }

        @Override
        public int getAndSet(T obj, int newValue) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return _rvpredict_get_and_set_value(obj, newValue);
        }

        @Override
        public int getAndIncrement(T obj) {
            return getAndAdd(obj, 1);
        }

        @Override
        public int getAndDecrement(T obj) {
            return getAndAdd(obj, -1);
        }

        @Override
        public int getAndAdd(T obj, int delta) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return _rvpredict_get_and_add_value(obj, delta);
        }

        @Override
        public int incrementAndGet(T obj) {
            return getAndAdd(obj, 1) + 1;
        }

        @Override
        public int decrementAndGet(T obj) {
             return getAndAdd(obj, -1) - 1;
        }

        @Override
        public int addAndGet(T obj, int delta) {
            return getAndAdd(obj, delta) + delta;
        }

        private void ensureProtectedAccess(T obj) {
            if (cclass.isInstance(obj)) {
                return;
            }
            throw new RuntimeException(
                new IllegalAccessException("Class " +
                    cclass.getName() +
                    " can not access a protected member of class " +
                    tclass.getName() +
                    " using an instance of " +
                    obj.getClass().getName()
                )
            );
        }
    }
}
