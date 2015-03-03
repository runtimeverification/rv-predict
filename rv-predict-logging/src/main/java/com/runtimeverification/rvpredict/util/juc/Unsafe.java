package com.runtimeverification.rvpredict.util.juc;

/**
 * Workaround to get an instance of {@link sun.misc.Unsafe} via reflection.
 *
 * @author YilongL
 */
class Unsafe {

    static sun.misc.Unsafe getUnsafe() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }

}
