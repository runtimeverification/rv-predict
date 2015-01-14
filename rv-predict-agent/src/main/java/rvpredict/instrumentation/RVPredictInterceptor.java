package rvpredict.instrumentation;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import rvpredict.runtime.RVPredictRuntime;

import com.google.common.collect.ImmutableList;

/**
 * Represents a special kind of RV-Predict runtime library method, i.e.
 * interceptor, that is bound to some corresponding Java method and can be
 * used to replace it during bytecode transformation.
 * <p>
 * For example, {@link RVPredictRuntime#rvPredictWait(Object, long, int)} is
 * associated with {@link Object#wait(long)}.
 *
 * @author YilongL
 */
public class RVPredictInterceptor extends RVPredictRuntimeMethod {

    /**
     * Method type of the associated Java method. Can be
     * {@link RVPredictRuntimeMethods#STATIC}, {@link RVPredictRuntimeMethods#VIRTUAL}, or
     * {@link RVPredictRuntimeMethods#SPECIAL}.
     */
    final int methodType;

    /**
     * Represents the class or interface in which the associated Java method
     * is declared.
     */
    final String classOrInterface;

    /**
     * The associated Java method's name.
     */
    final String name;

    /**
     * The parameter type descriptors of the associated Java method.
     */
    public final ImmutableList<String> paramTypeDescs;

    public static RVPredictInterceptor create(int methodType, String classOrInterface,
            String name, String interceptorName, Class<?>... parameterTypes)
            throws ClassNotFoundException {
        Class<?>[] interceptorParamTypes;
        int length = parameterTypes.length;
        if (methodType == RVPredictRuntimeMethods.STATIC) {
            interceptorParamTypes = new Class<?>[length + 1];
            System.arraycopy(parameterTypes, 0, interceptorParamTypes, 0, length);
            interceptorParamTypes[length] = int.class;
        } else {
            interceptorParamTypes = new Class<?>[length + 2];
            interceptorParamTypes[0] = Class.forName(classOrInterface.replace("/", "."));
            System.arraycopy(parameterTypes, 0, interceptorParamTypes, 1, length);
            interceptorParamTypes[length + 1] = int.class;
        }

        Method method = RVPredictRuntimeMethod.getAsmMethod(interceptorName, interceptorParamTypes);
        return new RVPredictInterceptor(method, methodType, classOrInterface, name,
                parameterTypes);
    }

    private RVPredictInterceptor(Method method, int opcode, String classOrInterface,
            String name, Class<?>[] parameterTypes) {
        super(method);
        this.methodType = opcode;
        this.classOrInterface = classOrInterface;
        this.name = name;
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (Class<?> cls : parameterTypes) {
            builder.add(Type.getDescriptor(cls));
        }
        paramTypeDescs = builder.build();
    }

    public String getOriginalMethodSig() {
        StringBuilder sb = new StringBuilder(name);
        sb.append("(");
        for (String paramTypeDesc : paramTypeDescs) {
            sb.append(paramTypeDesc);
        }
        sb.append(")");
        return sb.toString();
    }

}