package com.runtimeverification.rvpredict.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * Package private helper class for {@link RVPredictRuntime}.
 *
 * @author YilongL
 */
class Helper {

    static MethodHandle getFieldGetter(Class<?> klass, String name) {
        try {
            Field field = klass.getDeclaredField(name);
            field.setAccessible(true);
            return MethodHandles.lookup().unreflectGetter(field);
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static MethodHandle getMethodHandle(Class<?> klass, String name, Class<?>... parameterTypes) {
        try {
            java.lang.reflect.Method method = klass.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return MethodHandles.lookup().unreflect(method);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
