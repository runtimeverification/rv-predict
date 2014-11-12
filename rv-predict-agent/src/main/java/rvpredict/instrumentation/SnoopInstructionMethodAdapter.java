package rvpredict.instrumentation;

import static org.objectweb.asm.Opcodes.*;
import static rvpredict.config.Config.*;
import static rvpredict.instrumentation.Utility.*;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import rvpredict.config.Config;

public class SnoopInstructionMethodAdapter extends MethodVisitor {

    private final String className;
    private final String source;
    private final String methodName;
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

    private final Config config;

    private final GlobalStateForInstrumentation globalState;

    /**
     * current max index of local variables
     */
    private int crntMaxIndex;
    private int crntLineNum;

    public SnoopInstructionMethodAdapter(MethodVisitor mv, String source, String className,
            String methodName, String signature, int access, int argSize, Config config,
            GlobalStateForInstrumentation globalState) {
        super(Opcodes.ASM5, mv);
        this.source = source == null ? "Unknown" : source;
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
        this.isInit = "<init>".equals(methodName) || "<clinit>".equals(methodName);
        this.isSynchronized = (access & ACC_SYNCHRONIZED) != 0;
        this.isStatic = (access & ACC_STATIC) != 0;
        this.config = config;
        this.globalState = globalState;

        crntMaxIndex = argSize + 1;
        if (config.verbose) {
            System.out.println("method: " + this.methodName);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack + 5, crntMaxIndex + 2);// may change to ...
    }

    @Override
    public void visitVarInsn(int opcode, int localVarIdx) {
        crntMaxIndex = Math.max(localVarIdx, crntMaxIndex);

        switch (opcode) {
        case LSTORE: case DSTORE: case LLOAD: case DLOAD:
            /* double words load/store opcodes */
            crntMaxIndex = Math.max(crntMaxIndex, localVarIdx + 1);
        case ISTORE: case FSTORE: case ASTORE: case ILOAD: case FLOAD: case ALOAD:
        case RET:
            /* single word load/store and ret opcodes */
            mv.visitVarInsn(opcode, localVarIdx);
            break;
        default:
            assert false : "Unknown var instruction opcode " + opcode;
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        crntLineNum = line;
        mv.visitLineNumber(line, start);
    }

    private void prepareLoggingThreadEvents() {
        /* Precondition: the next instruction must be `invokevirtual` and there
         * is no argument for the method */
        // TODO(YilongL): this method is quite restricted since it requires
        // the virtual function we are logging to have zero arguments

        // <stack>... objectref </stack>
        int index = dupThenAStore(); // jvm_local_vars[index] = objectref
        addPushConstInsn(mv, getCrntStmtSID());
        mv.visitVarInsn(ALOAD, index);
        // <stack>... objectref sid objectref </stack>
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (opcode == INVOKEVIRTUAL) {
            if (isThreadClass(owner)) {
                if (desc.equals("()V")) {
                    switch (name) {
                    case "start":
                        prepareLoggingThreadEvents();
                        mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_THREAD_START,
                                DESC_LOG_THREAD_START, false);
                        break;
                    case "join":
                        /* TODO(YilongL): Since calls to thread join must be
                         * logged after they return, the code here is kind of
                         * ad-hoc. We definitely need more general way to handle
                         * such case. */
                        int sid = getCrntStmtSID();

                        int index = dupThenAStore();

                        mv.visitMethodInsn(opcode, owner, name, desc, itf);

                        addPushConstInsn(mv, sid);
                        mv.visitVarInsn(ALOAD, index);
                        mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_THREAD_JOIN,
                                DESC_LOG_THREAD_JOIN, false);
                        return;
                    case "wait":
                        prepareLoggingThreadEvents();
                        mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_WAIT, DESC_LOG_WAIT,
                                false);
                        break;
                    case "notify":
                    case "notifyAll":
                        prepareLoggingThreadEvents();
                        mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_NOTIFY,
                                DESC_LOG_NOTIFY, false);
                    }
                }
            }
        }
        mv.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        String varSig = (owner + "." + name).replace("/", ".");
        if (!globalState.isVariableShared(varSig)) {
            mv.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        int sid = globalState.getVariableId(varSig);
        String sig_loc = source
                + "|"
                + (className + "|" + signature + "|" + varSig + "|" + crntLineNum).replace("/",
                        ".");
        int ID = globalState.getLocationId(sig_loc);

        int localVarIdx;
        int localVarIdx2;
        switch (opcode) {
        case GETSTATIC:
            if (isInit) {
                mv.visitFieldInsn(opcode, owner, name, desc);
                return;
            }

            // <stack>... </stack>
            mv.visitFieldInsn(opcode, owner, name, desc);
            // <stack>... value </stack>
            localVarIdx = dupThenStoreValue(desc); // jvm_local_vars[localVarIdx] = value
            // <stack>... value </stack>
            addPushConstInsn(mv, ID);
            mv.visitInsn(ACONST_NULL);
            addPushConstInsn(mv, sid);
            loadThenBoxValue(desc, localVarIdx);
            addPushConstInsn(mv, 0);
            // <stack>... value ID null sid value false </stack>
            mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                    DESC_LOG_FIELD_ACCESS, false);
            // <stack>... value </stack>
            break;
        case PUTSTATIC:
            // <stack>... value </stack>
            localVarIdx = dupThenStoreValue(desc); // jvm_local_vars[localVarIdx] = value
            // <stack>... value </stack>
            mv.visitFieldInsn(opcode, owner, name, desc);
            // <stack>... </stack>
            addPushConstInsn(mv, ID);
            mv.visitInsn(ACONST_NULL);
            addPushConstInsn(mv, sid);
            loadThenBoxValue(desc, localVarIdx);
            // <stack>... ID null sid value </stack>

            if (isInit)
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                        DESC_LOG_INIT_WRITE_ACCESS, false);
            else {
                addPushConstInsn(mv, 1);
                // <stack>... ID null sid value false </stack>
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                        DESC_LOG_FIELD_ACCESS, false);
            }
            break;
        case GETFIELD:
            if (isInit) {
                mv.visitFieldInsn(opcode, owner, name, desc);
                return;
            }

            // <stack>... objectref </stack>
            localVarIdx = dupThenAStore(); // jvm_local_vars[localVarIdx] = objectref
            mv.visitFieldInsn(opcode, owner, name, desc);
            // <stack>... value </stack>
            localVarIdx2 = dupThenStoreValue(desc); // jvm_local_vars[localVarIdx2] = value
            // <stack>... value </stack>
            addPushConstInsn(mv, ID);
            mv.visitVarInsn(ALOAD, localVarIdx);
            addPushConstInsn(mv, sid);
            loadThenBoxValue(desc, localVarIdx2);
            addPushConstInsn(mv, 0);
            // <stack>... value ID objectref sid value false </stack>
            mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                    DESC_LOG_FIELD_ACCESS, false);
            // <stack>... value </stack>
            break;
        case PUTFIELD:
            // TODO(YilongL): why don't we instrument inner class fields?
            if (name.startsWith("this$")) { // inner class
                mv.visitFieldInsn(opcode, owner, name, desc);
                break;
            }

            // TODO(YilongL): what is this?
            if (className.contains("$") && name.startsWith("val$")) { // strange class
                mv.visitFieldInsn(opcode, owner, name, desc);
                break;
            }

            // <stack>... objectref value </stack>
            localVarIdx = storeValue(desc); // jvm_local_vars[localVarIdx] = value
            // <stack>... objectref </stack>
            localVarIdx2 = dupThenAStore(); // jvm_local_vars[localVarIdx2] = objectref
            // <stack>... objectref </stack>
            loadValue(desc, localVarIdx);
            // <stack>... objectref value </stack>
            mv.visitFieldInsn(opcode, owner, name, desc);
            // <stack>... </stack>
            addPushConstInsn(mv, ID);
            mv.visitVarInsn(ALOAD, localVarIdx2);
            addPushConstInsn(mv, sid);
            loadThenBoxValue(desc, localVarIdx);
            // <stack>... ID objectref sid value </stack>
            if (isInit)
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                        DESC_LOG_INIT_WRITE_ACCESS, false);
            else {
                addPushConstInsn(mv, 1);
                // <stack>... ID objectref sid value true </stack>
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                        DESC_LOG_FIELD_ACCESS, false);
            }
            // <stack>... </stack>
            break;
        default:
            System.err.println("Unknown field access opcode " + opcode);
            System.exit(1);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
        case AALOAD: case BALOAD: case CALOAD: case SALOAD:
        case IALOAD: case FALOAD: case DALOAD: case LALOAD:
            instrumentArrayLoad(opcode);
            break;
        case AASTORE: case BASTORE: case CASTORE: case SASTORE:
        case IASTORE: case FASTORE: case DASTORE: case LASTORE:
            instrumentArrayStore(opcode);
            break;
        case MONITORENTER: {
            int sid = getCrntStmtSID();
            // <stack>... objectref </stack>
            int index = dupThenAStore(); // jvm_local_vars[index] = objectref
            // <stack>... objectref </stack>
            mv.visitInsn(opcode);
            // <stack>... </stack>
            addPushConstInsn(mv, sid);
            mv.visitVarInsn(ALOAD, index);
            // <stack>... sid objectref </stack>
            mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_LOCK,
                    DESC_LOG_LOCK, false);
            break;
        }
        case MONITOREXIT: {
            int sid = getCrntStmtSID();
            // <stack>... objectref </stack>
            int index = dupThenAStore(); // jvm_local_vars[index] = objectref
            // <stack>... objectref </stack>
            addPushConstInsn(mv, sid);
            mv.visitVarInsn(ALOAD, index);
            // <stack>... objectref sid objectref </stack>
            mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_UNLOCK,
                    DESC_LOG_UNLOCK, false);
            // <stack>... objectref </stack>
            mv.visitInsn(opcode);
            break;
        }
        case IRETURN: case LRETURN: case FRETURN: case DRETURN:
        case ARETURN: case RETURN: case ATHROW:
            if (isSynchronized) {
                /* Add a runtime library callback to log {@code UNLOCK} event for synchronized method. */
                addPushConstInsn(mv, getCrntStmtSID());
                if (isStatic) {
                    mv.visitLdcInsn(Type.getObjectType(className));
                } else {
                    mv.visitVarInsn(ALOAD, 0);
                }
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_UNLOCK,
                        DESC_LOG_UNLOCK, false);
            }
            mv.visitInsn(opcode);
            break;
        default:
            mv.visitInsn(opcode);
            break;
        }
    }

    private void instrumentArrayLoad(int arrayLoadOpcode) {
        String stmtSig = getCrntStmtSig();
        if (isInit || !globalState.shouldInstrumentArray(stmtSig)) {
            mv.visitInsn(arrayLoadOpcode);
            return;
        }

        boolean isElemSingleWord = isElementSingleWord(arrayLoadOpcode);
        int sid = getArrayLocSID(stmtSig);

        // <stack>... arrayref index </stack>
        mv.visitInsn(DUP2);
        // <stack>... arrayref index arrayref index </stack>
        int localVarIdx1 = istore(); // jvm_local_vars[localVarIdx1] = index
        int localVarIdx2 = astore(); // jvm_local_vars[localVarIdx2] = arrayref
        // <stack>... arrayref index </stack>
        mv.visitInsn(arrayLoadOpcode);
        // <stack>... value </stack>, where `value` could be one word or two words
        if (isElemSingleWord) {
            mv.visitInsn(DUP);
        } else {
            mv.visitInsn(DUP2);
        }
        // <stack>... value value </stack>
        int localVarIdx3 = ++crntMaxIndex;
        if (!isElemSingleWord) {
            crntMaxIndex++;
        }
        mv.visitVarInsn(getElementStoreOpcode(arrayLoadOpcode), localVarIdx3); // jvm_local_vars[localVarIdx3] = value
        // <stack>... value </stack>

        addPushConstInsn(mv, sid);
        mv.visitVarInsn(ALOAD, localVarIdx2);
        mv.visitVarInsn(ILOAD, localVarIdx1);
        mv.visitVarInsn(getElementLoadOpcode(arrayLoadOpcode), localVarIdx3);
        // <stack>... value sid arrayref index value </stack>

        if (arrayLoadOpcode != AALOAD) {
            addPrimitive2ObjectConv(mv, arrayLoadOpcode);
        }
        // <stack>... value sid arrayref index valueObjRef </stack>

        addPushConstInsn(mv, 0);
        // <stack>... value sid arrayref index valueObjRef false </stack>

        mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                DESC_LOG_ARRAY_ACCESS, false);
        // <stack>... value </stack>
    }

    private void instrumentArrayStore(int arrayStoreOpcode) {
        String stmtSig = getCrntStmtSig();
        if (!globalState.shouldInstrumentArray(stmtSig)) {
            mv.visitInsn(arrayStoreOpcode);
            return;
        }

        boolean isElemSingleWord = isElementSingleWord(arrayStoreOpcode);
        int sid = getArrayLocSID(stmtSig);

        // <stack>... arrayref index value </stack>, where value could be one word or two words
        int localVarIdx1 = ++crntMaxIndex;
        mv.visitVarInsn(getElementStoreOpcode(arrayStoreOpcode), localVarIdx1); // jvm_local_vars[localVarIdx1] = value
        if (!isElemSingleWord) {
            crntMaxIndex++;
        }
        // <stack>... arrayref index </stack>
        mv.visitInsn(DUP2);
        // <stack>... arrayref index arrayref index </stack>
        int localVarIdx2 = istore(); // jvm_local_vars[localVarIdx2] = index
        // <stack>... arrayref index arrayref </stack>
        int localVarIdx3 = astore(); // jvm_local_vars[localVarIdx3] = arrayref
        // <stack>... arrayref index </stack>
        mv.visitVarInsn(getElementLoadOpcode(arrayStoreOpcode), localVarIdx1);
        // <stack>... arrayref index value </stack>
        mv.visitInsn(arrayStoreOpcode);
        // <stack>... </stack>
        addPushConstInsn(mv, sid);
        mv.visitVarInsn(ALOAD, localVarIdx3);
        mv.visitVarInsn(ILOAD, localVarIdx2);
        mv.visitVarInsn(getElementLoadOpcode(arrayStoreOpcode), localVarIdx1);
        // <stack>... sid arrayref index value </stack>
        if (arrayStoreOpcode != AASTORE) {
            addPrimitive2ObjectConv(mv, arrayStoreOpcode);
        }
        // <stack>... sid arrayref index valueObjRef </stack>
        if (isInit) {
            mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                    DESC_LOG_INIT_WRITE_ACCESS, false);
        } else {
            addPushConstInsn(mv, 1);
            // <stack>... sid arrayref index valueObjRef true </stack>
            mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                    DESC_LOG_ARRAY_ACCESS, false);
        }
        // <stack>... </stack>
    }

    @Override
    public void visitCode() {
        if (isSynchronized) {
            /* Add a runtime library callback to log {@code LOCK} event for synchronized method. */
            addPushConstInsn(mv, getCrntStmtSID());
            if (isStatic) {
                mv.visitLdcInsn(Type.getObjectType(className));
            } else {
                mv.visitVarInsn(ALOAD, 0);
            }
            mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_LOCK,
                    DESC_LOG_LOCK, false);
        }

        mv.visitCode();
    }

    // no branch
    /*
     * public void visitJumpInsn(int opcode, Label label) { String sig_loc =
     * (classname+"|"+methodsignature+"|"+line_cur).replace("/", "."); int ID =
     * globalState.getLocationId(sig_loc);
     *
     * switch (opcode) { case IFEQ://branch case IFNE: case IFLT: case IFGE:
     * case IFGT: case IFLE: case IF_ICMPEQ: case IF_ICMPNE: case IF_ICMPLT:
     * case IF_ICMPGE: case IF_ICMPGT: case IF_ICMPLE: case IF_ACMPEQ: case
     * IF_ACMPNE: case IFNULL: case IFNONNULL: addBipushInsn(mv,ID);
     * mv.visitMethodInsn(INVOKESTATIC, config.logClass,
     * config.LOG_BRANCH, config.DESC_LOG_BRANCH); default:
     * mv.visitJumpInsn(opcode, label);break; } }
     *
     * public void visitTableSwitchInsn(int min, int max, Label dflt, Label...
     * labels) { String sig_loc =
     * (classname+"|"+methodsignature+"|"+line_cur).replace("/", "."); int ID =
     * globalState.getLocationId(sig_loc);
     * addBipushInsn(mv,ID); mv.visitMethodInsn(INVOKESTATIC,
     * config.logClass, config.LOG_BRANCH,
     * config.DESC_LOG_BRANCH);
     *
     * mv.visitTableSwitchInsn(min, max, dflt, labels); }
     */

    /**
     * Stores the top value from the operand stack to the local variable array.
     *
     * @param desc
     *            the type descriptor of the value to store
     * @return the local variable index that stores the value
     */
    private int storeValue(String desc) {
        int localVarIdx = ++crntMaxIndex;
        int opcode = Type.getType(desc).getOpcode(ISTORE);
        mv.visitVarInsn(opcode, localVarIdx);
        if (isDoubleWordTypeDesc(desc)) {
            crntMaxIndex++;
        }
        return localVarIdx;
    }

    /**
     * Duplicates and then stores the top value from the operand stack to the
     * local variable array.
     *
     * @param desc
     *            the type descriptor of the value to store
     * @return the local variable index that stores the value
     */
    private int dupThenStoreValue(String desc) {
        int localVarIdx = ++crntMaxIndex;
        int opcode = Type.getType(desc).getOpcode(ISTORE);
        if (isSingleWordTypeDesc(desc)) {
            mv.visitInsn(DUP);
            mv.visitVarInsn(opcode, localVarIdx);
        } else {
            mv.visitInsn(DUP2);
            mv.visitVarInsn(opcode, localVarIdx);
            crntMaxIndex++;
        }
        return localVarIdx;
    }

    private int dupThenAStore() {
        int localVarIdx = ++crntMaxIndex;
        mv.visitInsn(DUP);
        mv.visitVarInsn(ASTORE, localVarIdx);
        return localVarIdx;
    }

    private int astore() {
        int localVarIdx = ++crntMaxIndex;
        mv.visitVarInsn(ASTORE, localVarIdx);
        return localVarIdx;
    }

    private int istore() {
        int localVarIdx = ++crntMaxIndex;
        mv.visitVarInsn(ISTORE, localVarIdx);
        return localVarIdx;
    }

    /**
     * Loads the value from the local variable array, boxes it, and puts it on
     * top of the operand stack.
     *
     * @param desc
     *            the type descriptor of the value to load
     * @param index
     *            the local variable index that has the value
     */
    private void loadThenBoxValue(String desc, int index) {
        loadValue(desc, index);
        if (isPrimitiveTypeDesc(desc)) {
            addPrimitive2ObjectConv(mv, desc);
        }
    }

    /**
     * Loads the value from the local variable array and puts it on top of the
     * operand stack.
     *
     * @param desc
     *            the type descriptor of the value to load
     * @param index
     *            the local variable index that has the value
     */
    private void loadValue(String desc, int index) {
        mv.visitVarInsn(Type.getType(desc).getOpcode(ILOAD), index);
    }

    /**
     * @return a unique integer representing the syntactic identifier of the
     *         current statement in the instrumented program
     */
    private int getCrntStmtSID() {
        return globalState.getLocationId(getCrntStmtSig());
    }

    /**
     * TODO(YilongL):
     * {@link GlobalStateForInstrumentation#getArrayLocationId(String)} doesn't
     * look right to me because it calls {@code getLocationId} inside. A poor
     * design of API at least.
     */
    @Deprecated
    private int getArrayLocSID(String stmtSig) {
        return globalState.getArrayLocationId(stmtSig);
    }

    /**
     * @return a unique string representing the signature of the current
     *         statement in the instrumented program
     */
    private String getCrntStmtSig() {
        // TODO(YilongL): is the replace really necessary?
        return String.format("%s|%s|%s|%s", source, className, signature, crntLineNum).replace("/", ".");
    }
}
