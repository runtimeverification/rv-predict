package com.runtimeverification.rvpredict.instrumentation.transformer;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.instrumentation.RVPredictInterceptor;
import com.runtimeverification.rvpredict.instrumentation.RVPredictRuntimeMethod;
import com.runtimeverification.rvpredict.instrumentation.RVPredictRuntimeMethods;
import com.runtimeverification.rvpredict.metadata.ClassMetadata;
import com.runtimeverification.rvpredict.metadata.Metadata;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import static com.runtimeverification.rvpredict.instrumentation.RVPredictRuntimeMethods.*;
import static com.runtimeverification.rvpredict.util.InstrumentationUtils.*;

public class MethodTransformer extends MethodVisitor implements Opcodes {

    private final InstructionAdapter mv;

    private final ClassLoader loader;
    private final String className;
    private final int version;
    private final String source;
    private final String signature;

    private int crntMaxLocals;

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
            String name, String desc, int access, int crntMaxLocals, ClassLoader loader,
            Configuration config) {
        super(Opcodes.ASM5, new InstructionAdapter(mv));
        this.mv = (InstructionAdapter) super.mv;
        this.source = source == null ? "Unknown" : source;
        this.className = className;
        this.version = version;
        this.signature = name + desc;
        this.isSynchronized = (access & ACC_SYNCHRONIZED) != 0;
        this.isStatic = (access & ACC_STATIC) != 0;
        this.crntMaxLocals = crntMaxLocals;
        this.loader = loader;
        this.branchModel = config.branch ? 1 : 0;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        crntLineNum = line;
        mv.visitLineNumber(line, start);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        switch (opcode) {
        case ISTORE: case FSTORE: case ASTORE:
            crntMaxLocals = Math.max(crntMaxLocals, var);
            break;
        case LSTORE: case DSTORE:
            crntMaxLocals = Math.max(crntMaxLocals, var + 1);
            break;
        case LLOAD: case DLOAD: case ILOAD: case FLOAD: case ALOAD: case RET:
            break;
        default:
            assert false : "Unknown var instruction opcode " + opcode;
        }
        mv.visitVarInsn(opcode, var);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        /* Optimization: https://github.com/runtimeverification/rv-predict/issues/314 */
        if (owner.equals(className)) {
            ClassMetadata classMetadata = Metadata.getClassMetadata(owner);
            if (classMetadata.getFieldNames().contains(name)
                    && (Metadata.getClassMetadata(owner).getAccess(name) & ACC_FINAL) != 0) {
                /* YilongL: note that this is not complete because `finalFields'
                 * only contains the final fields of the class we are instrumenting */
                mv.visitFieldInsn(opcode, owner, name, desc);
                return;
            }
        }

        int varId = Metadata.getVariableId(owner, name);
        int locId = getCrntLocId();

        Type valueType = Type.getType(desc);
        switch (opcode) {
        case GETSTATIC:
        case GETFIELD:
            /* read event should be logged after it happens */
            if (opcode == GETSTATIC) {
                // <stack>... </stack>
                mv.aconst(null);
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
                mv.aconst(null);
            }
            int objectref = storeNewLocal(OBJECT_TYPE);
            // <stack>... </stack>
            loadLocal(objectref, OBJECT_TYPE);
            loadLocal(value, valueType);
            // <stack>... objectref value </stack>
            calcLongValue(valueType);
            // <stack>... objectref longValue </stack>
            push(varId, 1, locId);
            // <stack>... objectref longValue varId true locId </stack>
            invokeRTMethod(LOG_FIELD_ACCESS);
            // <stack>... </stack>
            if (opcode == PUTFIELD) {
                loadLocal(objectref, OBJECT_TYPE);
            }
            loadLocal(value, valueType);
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
        RVPredictInterceptor interceptor = RVPredictRuntimeMethods.lookup(opcode, owner, methodSig,
                loader, itf);
        if (interceptor != null) {
            // <stack>... (objectref)? (arg)* </stack>
            push(getCrntLocId());
            // <stack>... (objectref)? (arg)* sid </stack>
            invokeRTMethod(interceptor);
            /* cast the result back to the original return type to pass bytecode
             * verification since an overriding method may specialize the return
             * type */
            if (version >= 51) {
                Type returnType = Type.getType((name + desc).substring(idx + 1));
                if (!interceptor.method.getReturnType().equals(returnType)) {
                    mv.checkcast(returnType);
                }
            }
            return;
        }
        mv.visitMethodInsn(opcode, owner, name, desc, itf);
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
                    loadThis();
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
        loadLocal(value, valueType);
        calcLongValue(valueType);
        // <stack>... arrayref index array index longValue </stack>
        push(1, getCrntLocId());
        // <stack>... arrayref index array index longValue true locId </stack>
        invokeRTMethod(LOG_ARRAY_ACCESS);
        // <stack>... arrayref index </stack>
        loadLocal(value, valueType);
        // <stack>... arrayref index value </stack>
        mv.visitInsn(arrayStoreOpcode); // <--- array store happens
    }

    @Override
    public void visitCode() {
        if (isSynchronized) {
            if (isStatic) {
                loadClassLiteral();
            } else {
                loadThis();
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
            mv.iconst(i);
        }
    }

    private void invokeRTMethod(RVPredictRuntimeMethod rvpredictRTMethod) {
        invokeStatic(RVPREDICT_RUNTIME_TYPE, rvpredictRTMethod.method);
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
     * <p>
     * <b>Note:</b> Unlike {@link GeneratorAdapter#newLocal(Type)}, the local
     * variables created by this method <em>do not</em> interfere with the local
     * variables in the original bytecode. When a local variable in the original
     * bytecode is created in {@link MethodVisitor#visitVarInsn(int, int)} by
     * some {@code xSTORE} instruction, it could simply overwrite the content of
     * our (phantom) local variable. That is to say, we do not affect the
     * ordering number of local variable in the original bytecode. Therefore,
     * there is no need to change the frames in the original bytecode.
     * <p>
     * In order to make this approach work correctly, we have to ensure two
     * things:
     * <li>our phantom local variable does not overwrite the content of some
     * original local variable; this is done by maintaining the correct
     * {@link MethodTransformer#crntMaxLocals} at all time
     * <li>our phantom local variable do not get overwritten before it serves
     * its purpose; this is guaranteed by the way we use phantom local
     * variables, that is, they are only used in a small scope that no
     * {@code xSTORE} instruction in the original bytecode can interfere
     *
     * @param type
     *            the type of the value
     * @return the index of the local variable which holds the value
     */
    private int storeNewLocal(Type type) {
        int local = crntMaxLocals + 1;
        crntMaxLocals += type.getSize();
        mv.store(local, type);
        return local;
    }

    private void loadLocal(int var, Type type) {
        mv.load(var, type);
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
            invokeStatic(JL_SYSTEM_TYPE, Method.getMethod("int identityHashCode(Object)"));
            mv.visitInsn(I2L);
            break;
        case Type.FLOAT:
            invokeStatic(JL_FLOAT_TYPE, Method.getMethod("int floatToIntBits(float)"));
            mv.visitInsn(I2L);
            break;
        case Type.DOUBLE:
            invokeStatic(JL_DOUBLE_TYPE, Method.getMethod("long doubleToLongBits(double)"));
            break;
        default:
            assert false : "Unexpected type: " + type;
        }
    }

    private void invokeStatic(Type owner, Method method) {
        mv.invokestatic(owner.getInternalName(), method.getName(), method.getDescriptor(), false);
    }

    private void loadThis() {
        mv.load(0, OBJECT_TYPE);
    }

    private void loadClassLiteral() {
        /* before Java 5 the bytecode for loading class literal is quite cumbersome */
        Type owner = Type.getObjectType(className);
        if (version < 49) {
            /* `class$` is a special method generated to compute class literal */
            String fieldName = "class$" + className.replace('/', '$');
            mv.getstatic(className, fieldName, CLASS_TYPE.getDescriptor());
            Label l0 = new Label();
            mv.ifnonnull(l0);
            mv.visitLdcInsn(className.replace('/', '.'));
            invokeStatic(owner, Method.getMethod("Class class$(String)"));
            mv.dup();
            mv.putstatic(className, fieldName, CLASS_TYPE.getDescriptor());
            Label l1 = new Label();
            mv.goTo(l1);
            mv.mark(l0);
            mv.getstatic(className, fieldName, CLASS_TYPE.getDescriptor());
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
