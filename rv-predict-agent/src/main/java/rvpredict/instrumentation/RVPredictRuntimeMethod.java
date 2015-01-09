package rvpredict.instrumentation;

import java.lang.reflect.Method;

import org.objectweb.asm.Type;

import rvpredict.runtime.RVPredictRuntime;

/**
 * Represents a RV-Predict runtime library method.
 *
 * @author YilongL
 */
public class RVPredictRuntimeMethod {

    /* method name and descriptor are used by ASM to uniquely locate a
     * method in RVPredictRuntime */
    public final String name;

    public final String desc;

    public static RVPredictRuntimeMethod create(String name, Class<?>... parameterTypes) {
        Method method = getMethodHandler(name, parameterTypes);
        return new RVPredictRuntimeMethod(method.getName(), Type.getMethodDescriptor(method));
    }

    static Method getMethodHandler(String name, Class<?>... parameterTypes) {
        Method method = null;
        try {
            method = RVPredictRuntime.class.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        return method;
    }

    protected RVPredictRuntimeMethod(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

}