package com.runtimeverification.rvpredict.instrumentation.transformer;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.instrumentation.RVPredictInterceptor;
import com.runtimeverification.rvpredict.instrumentation.RVPredictRuntimeMethod;
import com.runtimeverification.rvpredict.metadata.ClassFile;
import com.runtimeverification.rvpredict.metadata.Metadata;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import static com.runtimeverification.rvpredict.instrumentation.InstrumentUtils.*;
import static com.runtimeverification.rvpredict.instrumentation.RVPredictRuntimeMethods.*;

public class MethodTransformer extends MethodVisitor implements Opcodes {

    private final GeneratorAdapter mv;

    private final ClassLoader loader;
    private final String className;
    private final int version;
    private final String source;
    private final String signature;

    /**
     * Specifies whether the visited method is synchronized.
     */
    private final boolean isSynchronized;

    /**
     * Specifies whether the visited method is static.
     */
    private final boolean isStatic;

    private int crntLineNum;

    private final int branchModel;

    public MethodTransformer(MethodVisitor mv, String source, String className, int version,
            String name, String desc, int access, ClassLoader loader, Configuration config) {
        super(Opcodes.ASM5, new GeneratorAdapter(mv, access, name, desc));
        this.mv = (GeneratorAdapter) super.mv;
        this.source = source == null ? "Unknown" : source;
        this.className = className;
        this.version = version;
        this.signature = name + desc;
        this.isSynchronized = (access & ACC_SYNCHRONIZED) != 0;
        this.isStatic = (access & ACC_STATIC) != 0;
        this.loader = loader;
        this.branchModel = config.branch ? 1 : 0;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        crntLineNum = line;
        mv.visitLineNumber(line, start);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        ClassFile classFile = Metadata.resolveDeclaringClass(loader, owner, name);
        if (classFile == null) {
            System.err.printf("[Warning] field resolution failure; "
                    + "skipped instrumentation of field access %s.%s in class %s%n",
                    owner, name, className);
            mv.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        /* Optimization: https://github.com/runtimeverification/rv-predict/issues/314 */
        if ((classFile.getFieldAccess(name) & ACC_FINAL) != 0
                || !needToInstrument(classFile)) {
            mv.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        int varId = Metadata.getVariableId(classFile.getClassName(), name);
        int locId = getCrntLocId();

        Type valueType = Type.getType(desc);
        switch (opcode) {
        case GETSTATIC:
        case GETFIELD:
            /* read event should be logged after it happens */
            if (opcode == GETSTATIC) {
                // <stack>... </stack>
                mv.push((String) null);
                // <stack>... null </stack>
            } else {
                // <stack>... objectref </stack>
                mv.dup();
                // <stack>... objectref objectref </stack>
            }
            mv.visitFieldInsn(opcode, owner, name, desc); // read happens
            // <stack>... objectref value </stack>
            if (valueType.getSize() == 1) {
                mv.dupX1();
            } else {
                mv.dup2X1();
            }
            // <stack>... value objectref value </stack>
            calcLongValue(valueType);
            // <stack>... value objectref longValue </stack>
            push(varId, 0, locId);
            // <stack>... value objectref longValue varId false locId </stack>
            invokeRTMethod(LOG_FIELD_ACCESS);
            // <stack>... value </stack>
            break;
        case PUTSTATIC:
        case PUTFIELD:
            /* write event should be logged before it happens */

            // <stack>... (objectref)? value </stack>
            int value = storeNewLocal(valueType);
            if (opcode == PUTSTATIC) {
                mv.push((String) null);
            }
            int objectref = storeNewLocal(OBJECT_TYPE);
            // <stack>... </stack>
            mv.loadLocal(objectref, OBJECT_TYPE);
            mv.loadLocal(value, valueType);
            // <stack>... objectref value </stack>
            calcLongValue(valueType);
            // <stack>... objectref longValue </stack>
            push(varId, 1, locId);
            // <stack>... objectref longValue varId true locId </stack>
            invokeRTMethod(LOG_FIELD_ACCESS);
            // <stack>... </stack>
            if (opcode == PUTFIELD) {
                mv.loadLocal(objectref, OBJECT_TYPE);
            }
            mv.loadLocal(value, valueType);
            // <stack>... (objectref)? value </stack>
            mv.visitFieldInsn(opcode, owner, name, desc); // write happens
            // <stack>... </stack>
            break;
        default:
            System.err.println("Unknown field access opcode " + opcode);
            System.exit(1);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        int idx = (name + desc).lastIndexOf(')');
        String methodSig = (name + desc).substring(0, idx + 1);
        RVPredictInterceptor interceptor = lookup(opcode, owner, methodSig, loader, itf);
        int locId = getCrntLocId();
        if (interceptor != null) {
            // <stack>... (objectref)? (arg)* </stack>
            push(locId);
            // <stack>... (objectref)? (arg)* locId </stack>
            invokeRTMethod(interceptor);
            /* cast the result back to the original return type to pass bytecode
             * verification since an overriding method may specialize the return
             * type */
            if (version >= 51) {
                Type returnType = Type.getType((name + desc).substring(idx + 1));
                if (!interceptor.method.getReturnType().equals(returnType)) {
                    mv.checkCast(returnType);
                }
            }
        } else {
            ClassFile classFile = ClassFile.getInstance(loader, owner);
            if (!needToInstrument(classFile)) {
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            /* Wrap the method call instruction into a try-finally block with logging code.
             *
             * tryLabel:
             *     LOG_INVOKE_METHOD
             *     visitMethodInsn(opcode, owner, name, desc, itf)
             *     LOG_FINISH_METHOD
             *     goto afterLabel
             * finallyLabel:
             *     LOG_FINISH_METHOD
             *     throw exception
             * afterLabel:
             *     ...
             */
            Label tryLabel = mv.mark();
            push(locId);
            invokeRTMethod(LOG_INVOKE_METHOD);
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
            push(locId);
            invokeRTMethod(LOG_FINISH_METHOD);
            Label afterLabel = mv.newLabel();
            mv.goTo(afterLabel);
            Label finallyLabel = mv.newLabel();
            mv.visitTryCatchBlock(tryLabel, finallyLabel, finallyLabel, null);
            mv.mark(finallyLabel);
            push(locId);
            invokeRTMethod(LOG_FINISH_METHOD);
            mv.throwException();
            mv.mark(afterLabel);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
        case AALOAD: case BALOAD: case CALOAD: case SALOAD:
        case IALOAD: case FALOAD: case DALOAD: case LALOAD:
            logArrayLoad(opcode);
            break;
        case AASTORE: case BASTORE: case CASTORE: case SASTORE:
        case IASTORE: case FASTORE: case DASTORE: case LASTORE:
            logArrayStore(opcode);
            break;
        case MONITORENTER:
            /* moniter enter must logged after it happens */
            // <stack>... objectref </stack>
            mv.dup();
            mv.visitInsn(opcode);
            push(getCrntLocId());
            // <stack>... objectref locId </stack>
            invokeRTMethod(LOG_MONITOR_ENTER);
            break;
        case MONITOREXIT: {
            /* moniter exit must logged before it happens */
            // <stack>... objectref </stack>
            mv.dup();
            push(getCrntLocId());
            // <stack>... objectref objectref locId </stack>
            invokeRTMethod(LOG_MONITOR_EXIT);
            mv.visitInsn(opcode);
            break;
        }
        case IRETURN: case LRETURN: case FRETURN: case DRETURN:
        case ARETURN: case RETURN: case ATHROW:
            if (isSynchronized) {
                if (isStatic) {
                    loadClassLiteral();
                } else {
                    mv.loadThis();
                }
                push(getCrntLocId());
                invokeRTMethod(LOG_MONITOR_EXIT);
            }
            mv.visitInsn(opcode);
            break;
        default:
            mv.visitInsn(opcode);
            break;
        }
    }

    /**
     * Array load must be logged after it happens.
     */
    private void logArrayLoad(int arrayLoadOpcode) {
        Type valueType = getValueType(arrayLoadOpcode);
        // <stack>... arrayref index </stack>
        mv.dup2();
        // <stack>... arrayref index arrayref index </stack>
        mv.visitInsn(arrayLoadOpcode); // <--- array load happens
        // <stack>... arrayref index value </stack>
        if (valueType.getSize() == 1) {
            mv.dupX2();
        } else {
            mv.dup2X2();
        }
        // <stack>... value arrayref index value </stack>
        calcLongValue(valueType);
        // <stack>... value arrayref index longValue </stack>
        push(0, getCrntLocId());
        // <stack>... value arrayref index longValue false locId </stack>
        invokeRTMethod(LOG_ARRAY_ACCESS);
        // <stack>... value </stack>
    }

    /**
     * Array store must be logged before it happens.
     */
    private void logArrayStore(int arrayStoreOpcode) {
        Type valueType = getValueType(arrayStoreOpcode);
        // <stack>... arrayref index value </stack>
        int value = storeNewLocal(valueType);
        // <stack>... arrayref index </stack>
        mv.dup2();
        mv.loadLocal(value, valueType);
        calcLongValue(valueType);
        // <stack>... arrayref index array index longValue </stack>
        push(1, getCrntLocId());
        // <stack>... arrayref index array index longValue true locId </stack>
        invokeRTMethod(LOG_ARRAY_ACCESS);
        // <stack>... arrayref index </stack>
        mv.loadLocal(value, valueType);
        // <stack>... arrayref index value </stack>
        mv.visitInsn(arrayStoreOpcode); // <--- array store happens
    }

    @Override
    public void visitCode() {
        if (isSynchronized) {
            if (isStatic) {
                loadClassLiteral();
            } else {
                mv.loadThis();
            }
            push(getCrntLocId());
            invokeRTMethod(LOG_MONITOR_ENTER);
        }

        mv.visitCode();
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (branchModel == 1) {
            if (opcode != JSR && opcode != GOTO) {
                push(getCrntLocId());
                invokeRTMethod(LOG_BRANCH);
            }
        }
        mv.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        if (branchModel == 1) {
            push(getCrntLocId());
            invokeRTMethod(LOG_BRANCH);
        }
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }

    private void push(int... ints) {
        for (int i : ints) {
            mv.push(i);
        }
    }

    private void invokeRTMethod(RVPredictRuntimeMethod rvpredictRTMethod) {
        mv.invokeStatic(RVPREDICT_RUNTIME_TYPE, rvpredictRTMethod.method);
    }

    private Type getValueType(int arrayLoadOrStoreOpcode) {
        switch (arrayLoadOrStoreOpcode) {
        case BALOAD: case BASTORE:
            /* YilongL: see JVM Specification $2.11.1. Types and the Java Virtual Machine
             * for the reason why we return BYTE_TYPE instead of BOOLEAN_TYPE:
             * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-2.html#jvms-2.11.1 */
            return Type.BYTE_TYPE;
        case CALOAD: case CASTORE:
            return Type.CHAR_TYPE;
        case SALOAD: case SASTORE:
            return Type.SHORT_TYPE;
        case IALOAD: case IASTORE:
            return Type.INT_TYPE;
        case LALOAD: case LASTORE:
            return Type.LONG_TYPE;
        case AALOAD: case AASTORE:
            return OBJECT_TYPE;
        case FALOAD: case FASTORE:
            return Type.FLOAT_TYPE;
        case DALOAD: case DASTORE:
            return Type.DOUBLE_TYPE;
        default:
            assert false : "Expected an array load/store opcode; but found: " + arrayLoadOrStoreOpcode;
            return null;
        }
    }

    /**
     * Stores the top value on the operand stack to local variable array.
     *
     * @param type
     *            the type of the local variable to be created
     * @return the identifier of the newly created local variable
     */
    private int storeNewLocal(Type type) {
        int local = mv.newLocal(type);
        mv.storeLocal(local, type);
        return local;
    }

    public void calcLongValue(Type type) {
        switch (type.getSort()) {
        case Type.BOOLEAN:
        case Type.BYTE:
        case Type.CHAR:
        case Type.SHORT:
        case Type.INT:
            mv.cast(type, Type.LONG_TYPE);
        case Type.LONG:
            break;
        case Type.OBJECT:
        case Type.ARRAY:
            mv.invokeStatic(JL_SYSTEM_TYPE, Method.getMethod("int identityHashCode(Object)"));
            mv.visitInsn(I2L);
            break;
        case Type.FLOAT:
            mv.invokeStatic(JL_FLOAT_TYPE, Method.getMethod("int floatToIntBits(float)"));
            mv.visitInsn(I2L);
            break;
        case Type.DOUBLE:
            mv.invokeStatic(JL_DOUBLE_TYPE, Method.getMethod("long doubleToLongBits(double)"));
            break;
        default:
            assert false : "Unexpected type: " + type;
        }
    }

    private void loadClassLiteral() {
        /* before Java 5 the bytecode for loading class literal is quite cumbersome */
        Type owner = Type.getObjectType(className);
        if (version < 49) {
            /* `class$` is a special method generated to compute class literal */
            String fieldName = "class$" + className.replace('/', '$');
            mv.getStatic(owner, fieldName, CLASS_TYPE);
            Label l0 = mv.newLabel();
            mv.ifNonNull(l0);
            mv.visitLdcInsn(className.replace('/', '.'));
            mv.invokeStatic(owner, Method.getMethod("Class class$(String)"));
            mv.dup();
            mv.putStatic(owner, fieldName, CLASS_TYPE);
            Label l1 = new Label();
            mv.goTo(l1);
            mv.mark(l0);
            mv.getStatic(owner, fieldName, CLASS_TYPE);
            mv.mark(l1);
        } else {
            mv.visitLdcInsn(owner);
        }
    }

    /**
     * @return a unique integer representing the location identifier of the
     *         current statement in the instrumented program
     */
    private int getCrntLocId() {
        return Metadata.getLocationId(getCrntStmtSig());
    }

    /**
     * @return a unique string representing the signature of the current
     *         statement in the instrumented program
     */
    private String getCrntStmtSig() {
        return String.format("%s|%s|%s|%s", source, className, signature, crntLineNum);
    }
}
