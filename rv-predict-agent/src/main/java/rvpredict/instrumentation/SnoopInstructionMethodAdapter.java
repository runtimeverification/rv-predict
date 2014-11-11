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

    private void storeValue(String desc, int index) {
        int opcode = Type.getType(desc).getOpcode(ISTORE);
        if (isSingleWordTypeDesc(desc)) {
            mv.visitInsn(DUP);
            mv.visitVarInsn(opcode, index);
        } else {
            mv.visitInsn(DUP2);
            mv.visitVarInsn(opcode, index);
            crntMaxIndex++;
        }
    }

    private void loadValue(String desc, int index) {
        mv.visitVarInsn(Type.getType(desc).getOpcode(ILOAD), index);
        if (isPrimitiveTypeDesc(desc)) {
            addPrimitive2ObjectConv(mv, desc);
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        crntLineNum = line;
        mv.visitLineNumber(line, start);
    }

    private void prepareLoggingThreadEvents() {
        int sid = getCrntStmtSID();
        crntMaxIndex++;
        int index = crntMaxIndex;
        mv.visitInsn(DUP);
        mv.visitVarInsn(ASTORE, index);
        addPushConstInsn(mv, sid);
        mv.visitVarInsn(ALOAD, index);
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

                        crntMaxIndex++;
                        int index = crntMaxIndex;
                        mv.visitInsn(DUP);
                        mv.visitVarInsn(ASTORE, index);

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

        // signature + line number
        String sig_var = (owner + "." + name).replace("/", ".");
        int SID = globalState.getVariableId(sig_var);
        String sig_loc = source
                + "|"
                + (className + "|" + signature + "|" + sig_var + "|" + crntLineNum).replace("/",
                        ".");
        int ID = globalState.getLocationId(sig_loc);
        switch (opcode) {
        case GETSTATIC:
            mv.visitFieldInsn(opcode, owner, name, desc);
            if (!isInit) {
                if (globalState.isVariableShared(sig_var)) {

                    crntMaxIndex++;

                    int index = crntMaxIndex;
                    storeValue(desc, index);

                    addPushConstInsn(mv, ID);
                    mv.visitInsn(ACONST_NULL);
                    addPushConstInsn(mv, SID);
                    loadValue(desc, index);
                    addPushConstInsn(mv, 0);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                            DESC_LOG_FIELD_ACCESS, false);
                }
            }
            break;
        case PUTSTATIC:
            if (globalState.isVariableShared(sig_var)) {
                crntMaxIndex++;
                int index = crntMaxIndex;
                storeValue(desc, index);

                mv.visitFieldInsn(opcode, owner, name, desc);
                addPushConstInsn(mv, ID);
                mv.visitInsn(ACONST_NULL);
                addPushConstInsn(mv, SID);
                loadValue(desc, index);

                if (isInit)
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                            DESC_LOG_INIT_WRITE_ACCESS, false);
                else {
                    addPushConstInsn(mv, 1);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                            DESC_LOG_FIELD_ACCESS, false);
                }
            } else
                mv.visitFieldInsn(opcode, owner, name, desc);

            break;
        case GETFIELD:
            if (!isInit) {
                if (globalState.isVariableShared(sig_var)) {

                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index1);

                    mv.visitFieldInsn(opcode, owner, name, desc);

                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    storeValue(desc, index2);

                    addPushConstInsn(mv, ID);
                    mv.visitVarInsn(ALOAD, index1);
                    addPushConstInsn(mv, SID);
                    loadValue(desc, index2);

                    addPushConstInsn(mv, 0);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                            DESC_LOG_FIELD_ACCESS, false);
                } else
                    mv.visitFieldInsn(opcode, owner, name, desc);
            } else
                mv.visitFieldInsn(opcode, owner, name, desc);

            break;

        case PUTFIELD:
            if (name.startsWith("this$"))// inner class
            {
                mv.visitFieldInsn(opcode, owner, name, desc);
                break;
            }

            if (className.contains("$") && name.startsWith("val$"))// strange
                                                                   // class
            {
                mv.visitFieldInsn(opcode, owner, name, desc);
                break;
            }

            // if(classname.equals("org/eclipse/osgi/framework/eventmgr/CopyOnWriteIdentityMap$Snapshot$EntrySet")
            // &&methodname.equals("<init>"))
            // {
            // System.out.println(owner+" "+name+" "+desc);
            // mv.visitFieldInsn(opcode, owner, name, desc);break;
            // }

            if (globalState.isVariableShared(sig_var)) {
                crntMaxIndex++;
                int index1 = crntMaxIndex;
                int index2;
                if (desc.startsWith(DESC_DOUBLE)) {
                    mv.visitVarInsn(DSTORE, index1);
                    crntMaxIndex++;// double
                    crntMaxIndex++;
                    index2 = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitVarInsn(DLOAD, index1);
                } else if (desc.startsWith(DESC_LONG)) {
                    mv.visitVarInsn(LSTORE, index1);
                    crntMaxIndex++;// long
                    crntMaxIndex++;
                    index2 = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitVarInsn(LLOAD, index1);
                } else if (desc.startsWith(DESC_FLOAT)) {
                    mv.visitVarInsn(FSTORE, index1);
                    crntMaxIndex++;// float
                    index2 = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitVarInsn(FLOAD, index1);
                } else if (desc.startsWith(DESC_ARRAY_PREFIX)) {
                    mv.visitVarInsn(ASTORE, index1);
                    crntMaxIndex++;// ref or array
                    index2 = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitVarInsn(ALOAD, index1);
                } else if (desc.startsWith(DESC_OBJECT_PREFIX)) {
                    mv.visitVarInsn(ASTORE, index1);
                    crntMaxIndex++;// ref or array
                    index2 = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitVarInsn(ALOAD, index1);

                    // if(classname.equals("org/dacapo/parser/Config$Size")
                    // &&methodname.equals("<init>"))
                    // System.out.println("index1: "+
                    // index1+" index2: "+index2);
                } else {
                    mv.visitVarInsn(ISTORE, index1);
                    crntMaxIndex++;// integer,char,short,boolean
                    index2 = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitVarInsn(ILOAD, index1);
                }

                mv.visitFieldInsn(opcode, owner, name, desc);

                addPushConstInsn(mv, ID);
                mv.visitVarInsn(ALOAD, index2);
                addPushConstInsn(mv, SID);
                loadValue(desc, index1);

                if (isInit)
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                            DESC_LOG_INIT_WRITE_ACCESS, false);
                else {
                    addPushConstInsn(mv, 1);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                            DESC_LOG_FIELD_ACCESS, false);
                }
            } else
                mv.visitFieldInsn(opcode, owner, name, desc);
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
            int ID = getCrntStmtSID();

            mv.visitInsn(DUP);
            crntMaxIndex++;
            int index = crntMaxIndex;
            mv.visitVarInsn(ASTORE, index);// objectref
            mv.visitInsn(opcode);
            addPushConstInsn(mv, ID);
            mv.visitVarInsn(ALOAD, index);
            mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_LOCK,
                    DESC_LOG_LOCK, false);
            break;
        }
        case MONITOREXIT: {
            int ID = getCrntStmtSID();

            mv.visitInsn(DUP);
            crntMaxIndex++;
            int index = crntMaxIndex;
            mv.visitVarInsn(ASTORE, index);// objectref
            addPushConstInsn(mv, ID);
            mv.visitVarInsn(ALOAD, index);
            mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_UNLOCK,
                    DESC_LOG_UNLOCK, false);
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
        int localVarIdx1 = ++crntMaxIndex;
        mv.visitVarInsn(ISTORE, localVarIdx1); // jvm_local_vars[localVarIdx1] = index
        int localVarIdx2 = ++crntMaxIndex;
        mv.visitVarInsn(ASTORE, localVarIdx2); // jvm_local_vars[localVarIdx2] = arrayref
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
        // <stack>... value ID arrayref index value </stack>

        if (arrayLoadOpcode != AALOAD) {
            addPrimitive2ObjectConv(mv, arrayLoadOpcode);
        }
        // <stack>... value ID arrayref index valueObjRef </stack>

        addPushConstInsn(mv, 0);
        // <stack>... value ID arrayref index valueObjRef false </stack>

        mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                DESC_LOG_ARRAY_ACCESS, false);
        // <stack>... value </stack>
    }

    private void instrumentArrayStore(int arrayStoreOpcode) {
        boolean isElemSingleWord = isElementSingleWord(arrayStoreOpcode);

        String sig_loc = getCrntStmtSig();
        int ID = globalState.getArrayLocationId(sig_loc);

        if (globalState.shouldInstrumentArray(sig_loc)) {
            crntMaxIndex++;
            int index1 = crntMaxIndex;
            mv.visitVarInsn(getElementStoreOpcode(arrayStoreOpcode), index1);
            crntMaxIndex++;
            if (!isElemSingleWord) {
                mv.visitInsn(DUP2);
                crntMaxIndex++;
            }
            int index2 = crntMaxIndex;
            mv.visitVarInsn(ISTORE, index2);

            if (isElemSingleWord) {
                mv.visitInsn(DUP);
            }
            crntMaxIndex++;
            int index3 = crntMaxIndex;
            mv.visitVarInsn(ASTORE, index3);// arrayref
            if (isElemSingleWord) {
                mv.visitVarInsn(ILOAD, index2);// index
            }
            mv.visitVarInsn(getElementLoadOpcode(arrayStoreOpcode), index1);// value

            mv.visitInsn(arrayStoreOpcode);

            addPushConstInsn(mv, ID);
            mv.visitVarInsn(ALOAD, index3);
            mv.visitVarInsn(ILOAD, index2);
            mv.visitVarInsn(getElementLoadOpcode(arrayStoreOpcode), index1);
            if (arrayStoreOpcode != AASTORE) {
                addPrimitive2ObjectConv(mv, arrayStoreOpcode);
            }

            if (isInit) {
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                        DESC_LOG_INIT_WRITE_ACCESS, false);
            } else {
                addPushConstInsn(mv, 1);
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                        DESC_LOG_ARRAY_ACCESS, false);
            }
        } else {
            mv.visitInsn(arrayStoreOpcode);
        }
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
