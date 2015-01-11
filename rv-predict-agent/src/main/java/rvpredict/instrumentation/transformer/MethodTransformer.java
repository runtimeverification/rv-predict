package rvpredict.instrumentation.transformer;

import static org.objectweb.asm.Opcodes.*;
import static rvpredict.instrumentation.RVPredictRuntimeMethods.*;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import rvpredict.config.Config;
import rvpredict.instrumentation.MetaData;
import rvpredict.instrumentation.RVPredictInterceptor;
import rvpredict.instrumentation.RVPredictRuntimeMethod;
import rvpredict.instrumentation.RVPredictRuntimeMethods;
import rvpredict.runtime.RVPredictRuntime;

public class MethodTransformer extends MethodVisitor {

    private static final Type OBJECT_TYPE    = Type.getObjectType("java/lang/Object");
    private static final Type CLASS_TYPE     = Type.getObjectType("java/lang/Class");
    private static final Type JL_FLOAT_TYPE  = Type.getObjectType("java/lang/Float");
    private static final Type JL_DOUBLE_TYPE = Type.getObjectType("java/lang/Double");
    private static final Type JL_SYSTEM_TYPE = Type.getObjectType("java/lang/System");
    private static final Type RVPREDICT_RUNTIME_TYPE = Type.getType(RVPredictRuntime.class);

    private final GeneratorAdapter mv;

    private final String className;
    private final int version;
    private final String source;
    private final String signature;

    /**
     * Specifies whether the visited method is an initialization method.
     */
    private final boolean isInit;

    /**
     * Specifies whether the visited method is synchronized.
     */
    private final boolean isSynchronized;

    /**
     * Specifies whether the visited method is static.
     */
    private final boolean isStatic;

    private final Set<String> finalFields;

    private int crntLineNum;

    private final int branchModel;

    public MethodTransformer(MethodVisitor mv, String source, String className, int version,
            String name, String desc, int access, Set<String> finalFields, Config config) {
        super(Opcodes.ASM5, new GeneratorAdapter(mv, access, name, desc));
        this.mv = (GeneratorAdapter) super.mv;
        this.source = source == null ? "Unknown" : source;
        this.className = className;
        this.version = version;
        this.signature = name + desc;
        this.isInit = "<init>".equals(name) || "<clinit>".equals(name);
        this.isSynchronized = (access & ACC_SYNCHRONIZED) != 0;
        this.isStatic = (access & ACC_STATIC) != 0;
        this.finalFields = finalFields;
        this.branchModel = config.commandLine.branch ? 1 : 0;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        crntLineNum = line;
        mv.visitLineNumber(line, start);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        /* Optimization: no need to log field access or initialization for final fields */
        if (owner.equals(className) && finalFields.contains(name)) {
            /* YilongL: note that this is not complete because `finalFields'
             * only contains the final fields of the class we are instrumenting */
            mv.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        int varId = MetaData.getVariableId(owner, name);
        int locId = MetaData.getLocationId(getFieldAccLocSig(owner, name));

        Type valueType = Type.getType(desc);
        switch (opcode) {
        case GETSTATIC:
        case GETFIELD:
            /* read event should be logged after it happens */

            if (isInit) {
                mv.visitFieldInsn(opcode, owner, name, desc);
                return;
            }

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
            push(varId, 0, branchModel, locId);
            // <stack>... value objectref longValue varId false branch locId </stack>
            invokeStatic(LOG_FIELD_ACCESS);
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
            mv.loadLocal(objectref);
            mv.loadLocal(value);
            // <stack>... objectref value </stack>
            calcLongValue(valueType);
            // <stack>... objectref longValue </stack>
            if (isInit) {
                push(varId, locId);
                // <stack>... objectref longValue varId locId </stack>
                invokeStatic(LOG_FIELD_INIT);
            } else {
                push(varId, 1, branchModel, locId);
                // <stack>... objectref longValue varId true branch locId </stack>
                invokeStatic(LOG_FIELD_ACCESS);
            }
            // <stack>... </stack>
            if (opcode == PUTFIELD) {
                mv.loadLocal(objectref);
            }
            mv.loadLocal(value);
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
        RVPredictInterceptor interceptor = RVPredictRuntimeMethods.lookup(owner, methodSig, itf);
        if (interceptor != null) {
            // <stack>... (objectref)? (arg)* </stack>
            mv.push(getCrntLocId());
            // <stack>... (objectref)? (arg)* sid </stack>
            invokeStatic(interceptor);
            return;
        }
        mv.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
        case AALOAD: case BALOAD: case CALOAD: case SALOAD:
        case IALOAD: case FALOAD: case DALOAD: case LALOAD:
            if (isInit) {
                mv.visitInsn(opcode);
                return;
            }

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
            mv.push(getCrntLocId());
            // <stack>... objectref locId </stack>
            invokeStatic(LOG_MONITOR_ENTER);
            break;
        case MONITOREXIT: {
            /* moniter exit must logged before it happens */
            // <stack>... objectref </stack>
            mv.dup();
            mv.push(getCrntLocId());
            // <stack>... objectref objectref locId </stack>
            invokeStatic(LOG_MONITOR_EXIT);
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
                mv.push(getCrntLocId());
                invokeStatic(LOG_MONITOR_EXIT);
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
        invokeStatic(LOG_ARRAY_ACCESS);
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
        mv.loadLocal(value);
        calcLongValue(valueType);
        // <stack>... arrayref index array index longValue </stack>
        if (isInit) {
            push(getCrntLocId());
            // <stack>... arrayref index array index longValue locId </stack>
            invokeStatic(LOG_ARRAY_INIT);
        } else {
            push(1, getCrntLocId());
            // <stack>... arrayref index array index longValue true locId </stack>
            invokeStatic(LOG_ARRAY_ACCESS);
        }
        // <stack>... arrayref index </stack>
        mv.loadLocal(value);
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
            mv.push(getCrntLocId());
            invokeStatic(LOG_MONITOR_ENTER);
        }

        mv.visitCode();
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (branchModel == 1) {
            if (opcode != JSR && opcode != GOTO) {
                mv.push(getCrntLocId());
                invokeStatic(LOG_BRANCH);
            }
        }
        mv.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        if (branchModel == 1) {
            mv.push(getCrntLocId());
            invokeStatic(LOG_BRANCH);
        }
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }

    private void push(int... ints) {
        for (int i : ints) {
            mv.push(i);
        }
    }

    private void invokeStatic(RVPredictRuntimeMethod rvpredictRTMethod) {
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
            Label l1 = mv.newLabel();
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
        return MetaData.getLocationId(getCrntStmtSig());
    }

    private String getFieldAccLocSig(String owner, String name) {
        return String.format("%s|%s|%s|%s.%s|%s", source, className, signature, owner, name,
                crntLineNum).replace("/", ".");
    }

    /**
     * @return a unique string representing the signature of the current
     *         statement in the instrumented program
     */
    private String getCrntStmtSig() {
        return String.format("%s|%s|%s|%s", source, className, signature, crntLineNum).replace("/", ".");
    }
}
