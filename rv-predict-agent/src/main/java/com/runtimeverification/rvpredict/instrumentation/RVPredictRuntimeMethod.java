package com.runtimeverification.rvpredict.instrumentation;

import com.runtimeverification.rvpredict.runtime.RVPredictRuntime;
import org.objectweb.asm.commons.Method;

/**
 * Represents a RV-Predict runtime library method.
 *
 * @author YilongL
 */
public class RVPredictRuntimeMethod {

    /**
     * ASM method descriptor.
     */
    public final Method method;

    public static RVPredictRuntimeMethod create(String name, Class<?>... parameterTypes) {
        Method method = getAsmMethod(name, parameterTypes);
        return new RVPredictRuntimeMethod(method);
    }

    static Method getAsmMethod(String name, Class<?>... parameterTypes) {
        Method method = null;
        try {
            method = Method.getMethod(RVPredictRuntime.class.getMethod(name, parameterTypes));
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        return method;
    }

    protected RVPredictRuntimeMethod(Method method) {
        this.method = method;
    }

}