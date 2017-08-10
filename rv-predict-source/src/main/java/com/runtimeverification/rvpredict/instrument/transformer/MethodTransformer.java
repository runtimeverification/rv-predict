package com.runtimeverification.rvpredict.instrument.transformer;

import com.runtimeverification.rvpredict.instrument.InstrumentUtils;
import com.runtimeverification.rvpredict.instrument.RVPredictInterceptor;
import com.runtimeverification.rvpredict.instrument.RVPredictRuntimeMethod;
import com.runtimeverification.rvpredict.metadata.ClassFile;
import com.runtimeverification.rvpredict.runtime.RVPredictRuntime;
import com.runtimeverification.rvpredict.util.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.Stack;

import static com.runtimeverification.rvpredict.instrument.InstrumentUtils.*;
import static com.runtimeverification.rvpredict.instrument.RVPredictRuntimeMethods.*;

public class MethodTransformer extends MethodVisitor implements Opcodes {

    private final GeneratorAdapter mv;

    private final ClassLoader loader;
    private final String className;
    private final String methodName;
    private final String methodDesc;
    private final int version;
    private final String locIdPrefix;

    private final Logger logger;

    private final TransformStrategy strategy;

    /**
     * Specifies whether the visited method is synchronized.
     */
    private final boolean isSynchronized;

    /**
     * Specifies whether the visited method is static.
     */
    private final boolean isStatic;

    private int crntLineNum;

    /**
     * Contains the type of each object which was allocated before the super/this constructor call, but whose
     * constructor was not called yet.
     * <p>
     * Present only when the method being transformed is a constructor.
     */
    private Optional<Stack<String>> constructorHeaderUninitializedObjects = Optional.empty();

    public MethodTransformer(MethodVisitor mv, String source, String className, int version,
            String name, String desc, int access, ClassLoader loader, Logger logger,
            TransformStrategy strategy) {
        super(Opcodes.ASM5, new GeneratorAdapter(mv, access, name, desc));
        this.mv = (GeneratorAdapter) super.mv;
        this.className = className;
        this.methodName = name;
        this.methodDesc = desc;
        this.version = version;
        this.isSynchronized = (access & ACC_SYNCHRONIZED) != 0;
        this.isStatic = (access & ACC_STATIC) != 0;
        this.loader = loader;
        this.logger = logger;
        this.strategy = strategy;
        this.locIdPrefix = String.format("%s(%s:", className.replace("/", ".") + "." + name,
                source == null ? "Unknown" : source);
        if ("<init>".equals(name)) {
            constructorHeaderUninitializedObjects = Optional.of(new Stack<>());
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        crntLineNum = line;
        mv.visitLineNumber(line, start);
    }

    private Label methodStart;

    @Override
    public void visitCode() {
        mv.visitCode();

        if (isSynchronized && strategy.logMonitorEvent()) {
            methodStart = mv.mark();
            /* log a MONITOR_ENTER at the start of a synchronized method */
            onSyncMethodEnterOrExit(true);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (isSynchronized && strategy.logMonitorEvent()) {
            /* if the synchronized method ends abruptly because of ATHROW
             * instruction or uncaught exception thrown from nested method call,
             * we need to catch this Throwable, log a MONITOR_EXIT, and re-throw
             * the Throwable */
            Label origMethodEnd = mv.mark();
            mv.catchException(methodStart, origMethodEnd, null);
            onSyncMethodEnterOrExit(false);
            mv.throwException();
        }

        mv.visitMaxs(maxStack, maxLocals);
    }

    private void onSyncMethodEnterOrExit(boolean isEnter) {
        if (isStatic) {
            loadClassLiteral(className);
        } else {
            mv.loadThis();
        }
        push(getCrntLocId());
        invokeRtnMethod(isEnter ? LOG_MONITOR_ENTER : LOG_MONITOR_EXIT);
    }

    private String replaceStandardLibraryClass(String literal) {
        return strategy.replaceStandardLibraryClass() ?
                InstrumentUtils.replaceStandardLibraryClass(className, literal) : literal;
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start,
            Label end, int index) {
        mv.visitLocalVariable(name, replaceStandardLibraryClass(desc),
                replaceStandardLibraryClass(signature), start, end, index);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (constructorHeaderUninitializedObjects.isPresent() && opcode == NEW) {
            constructorHeaderUninitializedObjects.get().push(type);
        }
        mv.visitTypeInsn(opcode, replaceStandardLibraryClass(type));
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
            Object... bsmArgs) {
        mv.visitInvokeDynamicInsn(name, replaceStandardLibraryClass(desc), bsm, bsmArgs);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        mv.visitMultiANewArrayInsn(replaceStandardLibraryClass(desc), dims);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        owner = replaceStandardLibraryClass(owner);
        desc = replaceStandardLibraryClass(desc);
        ClassFile classFile = resolveDeclaringClass(loader, owner, name);
        if (classFile == null) {
            logger.debug(String.format("Unable to resolve field %s.%s in class %s", owner, name,
                    className));
            mv.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        /* Optimization: https://github.com/runtimeverification/rv-predict/issues/314 */
        if (!strategy.logMemoryAccess() || (classFile.getFieldAccess(name) & ACC_FINAL) != 0
                || !needToInstrument(classFile)) {
            mv.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        /* simple but imperfect fix for issue#513; neither sound nor complete */
        if ("finalize()V".equals(methodName + methodDesc)
            && opcode != GETSTATIC && opcode != PUTSTATIC) {
            /* Limitations:
             *   - miss read/write events on instance fields of other objects;
             *   - read/write events on the instance field of this object may
             *     still be logged from other methods called inside finalize()
             */
            mv.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        /* fix issue: https://github.com/runtimeverification/rv-predict/issues/458 */
        /*
           Having a PUTFIELD before the super/this constructor call is valid.
           From the Java bytecode specification, 2015-02-13, section 4.9.2, Structural Constraints:

           "Each instance initialization method, except for the instance initialization method
           derived from the constructor of class Object, must call either another instance
           initialization method of this or an instance initialization method of its direct
           superclass super before its instance members are accessed.
           However, instance fields of this that are declared in the current class may be
           assigned before calling any instance initialization method."

           However, it seems that passing an uninitialized (this) object to the invokeRtnMethod(LOG_FIELD_ACCESS) call,
           which is actually a call to RVPredictRuntime.logFieldAcc, will crash the Java verifier because there is an
           uninitialized object on the stack. Note that this will crash even if the logFieldAcc method has an empty
           body, so the issue is the method call itself.

           I (virgil.serbanuta) didn't find a reference in the bytecode specification about why this shouldn't work.
           Properly fixing this issue will likely involve postponing all the putfield logging until after the
           super/this constructor finished.
         */
        if (opcode == PUTFIELD && constructorHeaderUninitializedObjects.isPresent()) {
            mv.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        int varId = RVPredictRuntime.metadata.getVariableId(classFile.getClassName(), name);
        int locId = getCrntLocId();

        Type valueType = Type.getType(desc);
        switch (opcode) {
        case GETSTATIC:
        case GETFIELD:
            /* read event should be logged after it happens */
            if (opcode == GETSTATIC) {
                // <stack>... </stack>
                loadClassLiteral(owner);
                // <stack>... classltr </stack>
            } else {
                // <stack>... objectref </stack>
                mv.dup();
                // <stack>... objectref objectref </stack>
            }
            mv.visitFieldInsn(opcode, owner, name, desc); // read happens
            // <stack>... (objectref|classltr) value </stack>
            if (valueType.getSize() == 1) {
                mv.dupX1();
            } else {
                mv.dup2X1();
            }
            // <stack>... value (objectref|classltr) value </stack>
            calcLongValue(valueType);
            // <stack>... value (objectref|classltr) longValue </stack>
            push(varId, 0, locId);
            // <stack>... value (objectref|classltr) longValue varId false locId </stack>
            invokeRtnMethod(LOG_FIELD_ACCESS);
            // <stack>... value </stack>
            break;
        case PUTSTATIC:
        case PUTFIELD:
            /* write event should be logged before it happens */

            // <stack>... (objectref)? value </stack>
            int value = storeNewLocal(valueType);
            if (opcode == PUTSTATIC) {
                loadClassLiteral(owner);
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
            invokeRtnMethod(LOG_FIELD_ACCESS);
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
        owner = replaceStandardLibraryClass(owner);
        desc = replaceStandardLibraryClass(desc);

        // We need to do some special instrumentation in two cases which make the jvm crash when verifying the code:
        // base/super constructor calls and this() constructor calls (in Java terms). This crash is actually reasonable,
        // because in Java the super/this call must be the first statement in the method, which means that it can't
        // have a try block around it, although the bytecode specification may allow try-catch blocks in some cases.
        // Also, it is less obvious whether the other method calls before the super/this call can have a try block, but
        // we'll assume that they can.
        //
        // One may think that the super/this call is the first constructor call in the current constructor, but that's
        // not true, i.e. consider the following:
        // class Test : public A {
        //   Test(String s) {
        //     super(new String(s));
        //   }
        //   Test() {
        //     this(new String());
        //   }
        // }
        // In each of these constructors, the string constructor call will occur before the base/self constructor call,
        // since we need to build the String in order to pass it to the constructor.
        //
        // One may guess that the first call to one of the current class constructor or the base class constructor is
        // the one that matters, but that may not be true if we also have a Test(Test t) constructor and, in another
        // constructor we call self(new Test()).
        //
        // To solve this, we should note that at the beginning of a constructor we may have a sequence of paired new
        // and <init> operations, followed by an <init> without any new, which is the super or this constructor call.
        boolean isThisOrSuperCtorCall = false;
        if (constructorHeaderUninitializedObjects.isPresent() && "<init>".equals(name)) {
            if (constructorHeaderUninitializedObjects.get().isEmpty()) {
                isThisOrSuperCtorCall = true;
                constructorHeaderUninitializedObjects = Optional.empty();
            } else {
                String allocatedClassName = constructorHeaderUninitializedObjects.get().pop();
                assert allocatedClassName.equals(owner);
            }
        }

        int idx = (name + desc).lastIndexOf(')');
        String methodSig = (name + desc).substring(0, idx + 1);
        RVPredictInterceptor interceptor;
        int locId = getCrntLocId();
        if (strategy.interceptMethodCall(name)
                && (interceptor = lookup(opcode, owner, methodSig, loader, itf)) != null) {
            // <stack>... (objectref)? (arg)* </stack>
            push(locId);
            // <stack>... (objectref)? (arg)* locId </stack>
            invokeRtnMethod(interceptor);
            /* cast the result back to the original return type to pass bytecode
             * verification since an overriding method may specialize the return
             * type */
            if ((version & 0xFFFF) >= Opcodes.V1_5) {
                Type returnType = Type.getType((name + desc).substring(idx + 1));
                if (!interceptor.method.getReturnType().equals(returnType)) {
                    mv.checkCast(returnType);
                }
            }
        } else {
            if (owner.startsWith("[") || isThisOrSuperCtorCall || !strategy.logCallStackEvent()) {
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            /* Wrap the method call instruction into a try-finally block with logging code.
             *
             *     LOG_INVOKE_METHOD
             * L0:
             *     visitMethodInsn(opcode, owner, name, desc, itf)
             *     LOG_FINISH_METHOD
             *     goto L2
             * L1:
             *     LOG_FINISH_METHOD
             *     throw exception
             * L2:
             *     ...
             */
            push(locId);
            invokeRtnMethod(LOG_INVOKE_METHOD);
            Label l0 = mv.mark();
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
            push(locId);
            invokeRtnMethod(LOG_FINISH_METHOD);
            Label l2 = mv.newLabel();
            mv.goTo(l2);
            Label l1 = mv.mark();
            mv.catchException(l0, l1, null);
            push(locId);
            invokeRtnMethod(LOG_FINISH_METHOD);
            mv.throwException();
            mv.mark(l2);
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
            if (strategy.logMonitorEvent()) {
                /* moniter enter must logged after it happens */
                // <stack>... objectref </stack>
                mv.dup();
                mv.visitInsn(opcode);
                push(getCrntLocId());
                // <stack>... objectref locId </stack>
                invokeRtnMethod(LOG_MONITOR_ENTER);
            } else {
                mv.visitInsn(opcode);
            }
            break;
        case MONITOREXIT: {
            if (strategy.logMonitorEvent()) {
                /* moniter exit must logged before it happens */
                // <stack>... objectref </stack>
                mv.dup();
                push(getCrntLocId());
                // <stack>... objectref objectref locId </stack>
                invokeRtnMethod(LOG_MONITOR_EXIT);
                mv.visitInsn(opcode);
            } else {
                mv.visitInsn(opcode);
            }
            break;
        }
        case IRETURN: case LRETURN: case FRETURN: case DRETURN:
        case ARETURN: case RETURN:
            if (isSynchronized && strategy.logMonitorEvent()) {
                /* xRETURN instructions always cause the method to return;
                 * finally block is copied and placed before xRETURN instruction
                 * by the Java compiler */
                onSyncMethodEnterOrExit(false);
            }
            mv.visitInsn(opcode);
            break;
        case ATHROW:
            /* do not log a MONITOR_EXIT here because, unlike xRETURN family
             * instructions, ATHROW does not necessarily result in the exit of
             * the method; it can be caught later by some exception handler and
             * leads to multiple MONITOR_EXIT events, so the safe place to log a
             * MONITOR_EXIT for exceptions thrown inside a synchronized method
             * is in the exception handler that covers the whole method body */
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
        if (strategy.logMemoryAccess()) {
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
            invokeRtnMethod(LOG_ARRAY_ACCESS);
            // <stack>... value </stack>
        } else {
            mv.visitInsn(arrayLoadOpcode);
        }
    }

    /**
     * Array store must be logged before it happens.
     */
    private void logArrayStore(int arrayStoreOpcode) {
        if (strategy.logMemoryAccess()) {
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
            invokeRtnMethod(LOG_ARRAY_ACCESS);
            // <stack>... arrayref index </stack>
            mv.loadLocal(value, valueType);
            // <stack>... arrayref index value </stack>
        }
        mv.visitInsn(arrayStoreOpcode); // <--- array store happens
    }

    private void push(int... ints) {
        for (int i : ints) {
            mv.push(i);
        }
    }

    private void invokeRtnMethod(RVPredictRuntimeMethod rvpredictRTMethod) {
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

    private void loadClassLiteral(String cname) {
        /* before Java 5 the bytecode for loading class literal is quite cumbersome */
        if ((version & 0xFFFF) < Opcodes.V1_5) {
            mv.visitLdcInsn(cname.replace('/', '.'));
            mv.invokeStatic(CLASS_TYPE, Method.getMethod("Class forName(String)"));
        } else {
            mv.visitLdcInsn(Type.getObjectType(cname));
        }
    }

    /**
     * @return a unique integer representing the location identifier of the
     *         current statement in the instrumented program
     */
    private int getCrntLocId() {
        return RVPredictRuntime.metadata.getLocationId(locIdPrefix
                + (crntLineNum == 0 ? "n/a" : crntLineNum) + ")");
    }

    /**
     * Resolves the declaring class of a given field.
     *
     * @param loader
     *            the loader that can be used to locate the owner class of the
     *            field
     * @param cname
     *            the field's owner class name
     * @param fname
     *            the field's name
     * @return the {@link ClassFile} of the declaring class or {@code null} if
     *         the resolution fails
     */
    private ClassFile resolveDeclaringClass(ClassLoader loader, String cname, String fname) {
        Deque<String> deque = new ArrayDeque<>();
        deque.add(cname);
        while (!deque.isEmpty()) {
            cname = deque.removeFirst();
            ClassFile classFile = ClassFile.getInstance(loader, cname);
            if (classFile != null) {
                if (classFile.getFieldNames().contains(fname)) {
                    return classFile;
                } else {
                    String superName = classFile.getSuperName();
                    // the superName of any interface is Object
                    if (superName != null && !superName.equals("java/lang/Object")) {
                        deque.addLast(superName);
                    }
                    deque.addAll(classFile.getInterfaces());
                }
            }
        }
        return null;
    }

}
