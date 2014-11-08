package rvpredict.instrumentation;

import static org.objectweb.asm.Opcodes.*;
import static rvpredict.config.Config.*;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import rvpredict.config.Config;

public class SnoopInstructionMethodAdapter extends MethodVisitor {

    final static String CLASS_INTEGER = "java/lang/Integer";
    final static String CLASS_BOOLEAN = "java/lang/Boolean";
    final static String CLASS_CHAR = "java/lang/Character";
    final static String CLASS_SHORT = "java/lang/Short";
    final static String CLASS_BYTE = "java/lang/Byte";
    final static String CLASS_LONG = "java/lang/Long";
    final static String CLASS_FLOAT = "java/lang/Float";
    final static String CLASS_DOUBLE = "java/lang/Double";

    final static String METHOD_VALUEOF = "valueOf";
    final static String DESC_INTEGER_VALUEOF = "(I)Ljava/lang/Integer;";
    final static String DESC_BOOLEAN_VALUEOF = "(Z)Ljava/lang/Boolean;";
    final static String DESC_BYTE_VALUEOF = "(B)Ljava/lang/Byte;";
    final static String DESC_SHORT_VALUEOF = "(S)Ljava/lang/Short;";
    final static String DESC_CHAR_VALUEOF = "(C)Ljava/lang/Character;";
    final static String DESC_LONG_VALUEOF = "(J)Ljava/lang/Long;";
    final static String DESC_FLOAT_VALUEOF = "(F)Ljava/lang/Float;";
    final static String DESC_DOUBLE_VALUEOF = "(D)Ljava/lang/Double;";

    boolean isInit, isSynchronized, isStatic;
    String classname;
    String source;
    String methodname;
    String methodsignature;
    
    private final Config config;
    
    private final GlobalStateForInstrumentation globalState;
    
    /**
     * current max index of local variables
     */
    private int crntMaxIndex;
    private int crntLineNum;

    public SnoopInstructionMethodAdapter(MethodVisitor mv, String source, String cname,
            String mname, String msignature, boolean isInit, boolean isSynchronized,
            boolean isStatic, int argSize, Config config, GlobalStateForInstrumentation globalState) {
        super(Opcodes.ASM5, mv);
        this.source = source == null ? "Unknown" : source;
        this.classname = cname;
        this.methodname = mname;
        this.methodsignature = msignature;
        this.isInit = isInit;
        this.isSynchronized = isSynchronized;
        this.isStatic = isStatic;
        this.config = config;
        this.globalState = globalState;

        crntMaxIndex = argSize + 1;
        if (config.verbose)
            System.out.println("method: " + methodname);

        // DEBUG
        // if(classname.equals("org/dacapo/harness/CommandLineArgs")
        // &&methodname.equals("<init>"))
        // System.out.println("method: "+methodname);
    }
    
    private static final int[] ICONST_X = {ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 }; 

    /**
     * Private helper method that adds a {@code bipush} instruction which pushes
     * a byte onto the stack as an integer value.
     * 
     * @param value the value to be pushed to the stack
     */
    private void addBipushInsn(int value) {
        // TODO(YilongL): why not `byte value'? bad method name or latent bug?
        if ((0 <= value) && (value <= 5)) {
            mv.visitInsn(ICONST_X[value]);
        } else {
            mv.visitLdcInsn(new Integer(value));
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
        case LSTORE:
        case DSTORE:
        case LLOAD:
        case DLOAD:
            crntMaxIndex = Math.max(crntMaxIndex, localVarIdx + 1);
        case ISTORE:
        case FSTORE:
        case ASTORE:
        case ILOAD:
        case FLOAD:
        case ALOAD:
        case RET:
            mv.visitVarInsn(opcode, localVarIdx);
            break;
        default:
            assert false : "Unknown var instruction opcode " + opcode;
        }
    }

    private void storeValue(String desc, int index) {
        if (desc.startsWith("L") || desc.startsWith("[")) {
            mv.visitInsn(DUP);
            mv.visitVarInsn(ASTORE, index);
        } else if (desc.startsWith("I") || desc.startsWith("B") || desc.startsWith("S")
                || desc.startsWith("Z") || desc.startsWith("C")) {
            mv.visitInsn(DUP);
            mv.visitVarInsn(ISTORE, index);
        } else if (desc.startsWith("J")) {
            mv.visitInsn(DUP2);
            mv.visitVarInsn(LSTORE, index);
            crntMaxIndex++;
        } else if (desc.startsWith("F")) {
            mv.visitInsn(DUP);
            mv.visitVarInsn(FSTORE, index);
        } else if (desc.startsWith("D")) {
            mv.visitInsn(DUP2);
            mv.visitVarInsn(DSTORE, index);
            crntMaxIndex++;
        }

    }

    private void loadValue(String desc, int index) {
        if (desc.startsWith("L") || desc.startsWith("["))
            mv.visitVarInsn(ALOAD, index);
        else if (desc.startsWith("I")) {
            // convert int to object?
            mv.visitVarInsn(ILOAD, index);
            mv.visitMethodInsn(INVOKESTATIC, CLASS_INTEGER, METHOD_VALUEOF, DESC_INTEGER_VALUEOF, false);
        } else if (desc.startsWith("B")) {
            // convert int to object?
            mv.visitVarInsn(ILOAD, index);
            mv.visitMethodInsn(INVOKESTATIC, CLASS_BYTE, METHOD_VALUEOF, DESC_BYTE_VALUEOF, false);
        } else if (desc.startsWith("S")) {
            // convert int to object?
            mv.visitVarInsn(ILOAD, index);
            mv.visitMethodInsn(INVOKESTATIC, CLASS_SHORT, METHOD_VALUEOF, DESC_SHORT_VALUEOF, false);
        } else if (desc.startsWith("Z")) {
            // convert int to object?
            mv.visitVarInsn(ILOAD, index);
            mv.visitMethodInsn(INVOKESTATIC, CLASS_BOOLEAN, METHOD_VALUEOF, DESC_BOOLEAN_VALUEOF, false);
        } else if (desc.startsWith("C")) {
            // convert int to object?
            mv.visitVarInsn(ILOAD, index);
            mv.visitMethodInsn(INVOKESTATIC, CLASS_CHAR, METHOD_VALUEOF, DESC_CHAR_VALUEOF, false);
        } else if (desc.startsWith("J")) {
            // convert int to object?
            mv.visitVarInsn(LLOAD, index);
            mv.visitMethodInsn(INVOKESTATIC, CLASS_LONG, METHOD_VALUEOF, DESC_LONG_VALUEOF, false);
        } else if (desc.startsWith("F")) {
            // convert int to object?
            mv.visitVarInsn(FLOAD, index);
            mv.visitMethodInsn(INVOKESTATIC, CLASS_FLOAT, METHOD_VALUEOF, DESC_FLOAT_VALUEOF, false);
        } else if (desc.startsWith("D")) {
            // convert int to object?
            mv.visitVarInsn(DLOAD, index);
            mv.visitMethodInsn(INVOKESTATIC, CLASS_DOUBLE, METHOD_VALUEOF, DESC_DOUBLE_VALUEOF, false);
        }

    }

    @Override
    public void visitLineNumber(int line, Label start) {
        crntLineNum = line;
        mv.visitLineNumber(line, start);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

        switch (opcode) {
        case INVOKEVIRTUAL:
            if (config.commandLine.agentOnlySharing
                    || !globalState.isThreadClass(owner))
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
            else {
                String sig_loc = source + "|"
                        + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
                int ID = globalState.getLocationId(sig_loc);

                if (name.equals("start") && desc.equals("()V")) {
                    crntMaxIndex++;
                    int index = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index);
                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_THREAD_START,
                            DESC_LOG_THREAD_START, false);

                    mv.visitMethodInsn(opcode, owner, name, desc, itf);
                } else if (name.equals("join") && desc.equals("()V")) {
                    crntMaxIndex++;
                    int index = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index);

                    mv.visitMethodInsn(opcode, owner, name, desc, itf);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_THREAD_JOIN,
                            DESC_LOG_THREAD_JOIN, false);

                } else if (name.equals("wait") && desc.equals("()V")) {
                    crntMaxIndex++;
                    int index = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_WAIT, DESC_LOG_WAIT,
                            false);

                    mv.visitMethodInsn(opcode, owner, name, desc, itf);
                } else if (name.equals("wait") && desc.equals("()V")) {
                    crntMaxIndex++;
                    int index = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_WAIT, DESC_LOG_WAIT,
                            false);

                    mv.visitMethodInsn(opcode, owner, name, desc, itf);
                } else if ((name.equals("notify") || name.equals("notifyAll"))
                        && desc.equals("()V")) {
                    crntMaxIndex++;
                    int index = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_NOTIFY, DESC_LOG_NOTIFY,
                            false);

                    mv.visitMethodInsn(opcode, owner, name, desc, itf);
                } else
                    mv.visitMethodInsn(opcode, owner, name, desc, itf);

            }

            break;
        case INVOKESPECIAL:
        case INVOKESTATIC:
        case INVOKEINTERFACE:
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
            break;
        default:
            System.err.println("Unknown method invocation opcode " + opcode);
            System.exit(1);
        }

    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {

        // signature + line number
        String sig_var = (owner + "." + name).replace("/", ".");
        int SID = globalState.getVariableId(sig_var);
        String sig_loc = source
                + "|"
                + (classname + "|" + methodsignature + "|" + sig_var + "|" + crntLineNum).replace("/",
                        ".");
        int ID = globalState.getLocationId(sig_loc);
        switch (opcode) {
        case GETSTATIC:
            mv.visitFieldInsn(opcode, owner, name, desc);
            if (!isInit) {

                if (config.commandLine.agentOnlySharing) {
                    addBipushInsn(ID);
                    // mv.visitInsn(ACONST_NULL);
                    addBipushInsn(SID);
                    addBipushInsn(0);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                            DESC_LOG_FIELD_ACCESS_DETECT_SHARING, false);
                } else if (globalState.isVariableShared(sig_var)) {

                    crntMaxIndex++;

                    int index = crntMaxIndex;
                    storeValue(desc, index);

                    addBipushInsn(ID);
                    mv.visitInsn(ACONST_NULL);
                    addBipushInsn(SID);
                    loadValue(desc, index);
                    addBipushInsn(0);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                            DESC_LOG_FIELD_ACCESS, false);
                }
            }
            break;
        case PUTSTATIC:
            if (config.commandLine.agentOnlySharing) {
                mv.visitFieldInsn(opcode, owner, name, desc);

                if (!isInit) {
                    addBipushInsn(ID);
                    // mv.visitInsn(ACONST_NULL);
                    addBipushInsn(SID);

                    addBipushInsn(1);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                            DESC_LOG_FIELD_ACCESS_DETECT_SHARING, false);
                }

            } else if (globalState.isVariableShared(sig_var)) {
                crntMaxIndex++;
                int index = crntMaxIndex;
                storeValue(desc, index);

                mv.visitFieldInsn(opcode, owner, name, desc);
                addBipushInsn(ID);
                mv.visitInsn(ACONST_NULL);
                addBipushInsn(SID);
                loadValue(desc, index);

                if (isInit)
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                            DESC_LOG_INIT_WRITE_ACCESS, false);
                else {
                    addBipushInsn(1);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                            DESC_LOG_FIELD_ACCESS, false);
                }
            } else
                mv.visitFieldInsn(opcode, owner, name, desc);

            break;
        case GETFIELD:
            if (!isInit) {
                if (config.commandLine.agentOnlySharing) {
                    // maxindex_cur++;
                    // int index1 = maxindex_cur;
                    // mv.visitInsn(DUP);
                    // mv.visitVarInsn(ASTORE, index1);

                    mv.visitFieldInsn(opcode, owner, name, desc);

                    addBipushInsn(ID);
                    // mv.visitVarInsn(ALOAD, index1);
                    addBipushInsn(SID);

                    addBipushInsn(0);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                            DESC_LOG_FIELD_ACCESS_DETECT_SHARING, false);
                } else if (globalState.isVariableShared(sig_var)) {

                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index1);

                    mv.visitFieldInsn(opcode, owner, name, desc);

                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    storeValue(desc, index2);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index1);
                    addBipushInsn(SID);
                    loadValue(desc, index2);

                    addBipushInsn(0);

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

            if (classname.contains("$") && name.startsWith("val$"))// strange
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

            if (config.commandLine.agentOnlySharing) {
                if (!isInit) {
                    // maxindex_cur++;
                    // int index1 = maxindex_cur;
                    // int index2;
                    // if(desc.startsWith("D"))
                    // {
                    // mv.visitVarInsn(DSTORE, index1);
                    // maxindex_cur++;//double
                    // maxindex_cur++;
                    // index2 = maxindex_cur;
                    // mv.visitInsn(DUP);
                    // mv.visitVarInsn(ASTORE, index2);
                    // mv.visitVarInsn(DLOAD, index1);
                    // }
                    // else if(desc.startsWith("J"))
                    // {
                    // mv.visitVarInsn(LSTORE, index1);
                    // maxindex_cur++;//long
                    // maxindex_cur++;
                    // index2 = maxindex_cur;
                    // mv.visitInsn(DUP);
                    // mv.visitVarInsn(ASTORE, index2);
                    // mv.visitVarInsn(LLOAD, index1);
                    // }
                    // else if(desc.startsWith("F"))
                    // {
                    // mv.visitVarInsn(FSTORE, index1);
                    // maxindex_cur++;//float
                    // index2 = maxindex_cur;
                    // mv.visitInsn(DUP);
                    // mv.visitVarInsn(ASTORE, index2);
                    // mv.visitVarInsn(FLOAD, index1);
                    // }
                    // else if(desc.startsWith("["))
                    // {
                    // mv.visitVarInsn(ASTORE, index1);
                    // maxindex_cur++;//ref or array
                    // index2 = maxindex_cur;
                    // mv.visitInsn(DUP);
                    // mv.visitVarInsn(ASTORE, index2);
                    // mv.visitVarInsn(ALOAD, index1);
                    // }
                    // else if(desc.startsWith("L"))
                    // {
                    // mv.visitVarInsn(ASTORE, index1);
                    // maxindex_cur++;//ref or array
                    // index2 = maxindex_cur;
                    // mv.visitInsn(DUP);
                    // mv.visitVarInsn(ASTORE, index2);
                    // mv.visitVarInsn(ALOAD, index1);
                    //
                    // }
                    // else
                    // {
                    // mv.visitVarInsn(ISTORE, index1);
                    // maxindex_cur++;//integer,char,short,boolean
                    // index2 = maxindex_cur;
                    // mv.visitInsn(DUP);
                    // mv.visitVarInsn(ASTORE, index2);
                    // mv.visitVarInsn(ILOAD, index1);
                    // }

                    mv.visitFieldInsn(opcode, owner, name, desc);

                    addBipushInsn(ID);
                    // mv.visitVarInsn(ALOAD, index2);
                    addBipushInsn(SID);

                    addBipushInsn(1);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                            DESC_LOG_FIELD_ACCESS_DETECT_SHARING, false);
                } else
                    mv.visitFieldInsn(opcode, owner, name, desc);
            } else if (globalState.isVariableShared(sig_var)) {
                crntMaxIndex++;
                int index1 = crntMaxIndex;
                int index2;
                if (desc.startsWith("D")) {
                    mv.visitVarInsn(DSTORE, index1);
                    crntMaxIndex++;// double
                    crntMaxIndex++;
                    index2 = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitVarInsn(DLOAD, index1);
                } else if (desc.startsWith("J")) {
                    mv.visitVarInsn(LSTORE, index1);
                    crntMaxIndex++;// long
                    crntMaxIndex++;
                    index2 = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitVarInsn(LLOAD, index1);
                } else if (desc.startsWith("F")) {
                    mv.visitVarInsn(FSTORE, index1);
                    crntMaxIndex++;// float
                    index2 = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitVarInsn(FLOAD, index1);
                } else if (desc.startsWith("[")) {
                    mv.visitVarInsn(ASTORE, index1);
                    crntMaxIndex++;// ref or array
                    index2 = crntMaxIndex;
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitVarInsn(ALOAD, index1);
                } else if (desc.startsWith("L")) {
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

                addBipushInsn(ID);
                mv.visitVarInsn(ALOAD, index2);
                addBipushInsn(SID);
                loadValue(desc, index1);

                if (isInit)
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                            DESC_LOG_INIT_WRITE_ACCESS, false);
                else {
                    addBipushInsn(1);

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

    private void convertPrimitiveToObject(int opcode) {
        switch (opcode) {
        case IALOAD:
        case IASTORE:
            mv.visitMethodInsn(INVOKESTATIC, CLASS_INTEGER, METHOD_VALUEOF, DESC_INTEGER_VALUEOF, false);
            break;
        case BALOAD:
        case BASTORE:
            mv.visitMethodInsn(INVOKESTATIC, CLASS_BOOLEAN, METHOD_VALUEOF, DESC_BOOLEAN_VALUEOF, false);
            break;
        case CALOAD:
        case CASTORE:
            mv.visitMethodInsn(INVOKESTATIC, CLASS_CHAR, METHOD_VALUEOF, DESC_CHAR_VALUEOF, false);
            break;
        case DALOAD:
        case DASTORE:
            mv.visitMethodInsn(INVOKESTATIC, CLASS_DOUBLE, METHOD_VALUEOF, DESC_DOUBLE_VALUEOF, false);
            break;
        case FALOAD:
        case FASTORE:
            mv.visitMethodInsn(INVOKESTATIC, CLASS_FLOAT, METHOD_VALUEOF, DESC_FLOAT_VALUEOF, false);
            break;
        case LALOAD:
        case LASTORE:
            mv.visitMethodInsn(INVOKESTATIC, CLASS_LONG, METHOD_VALUEOF, DESC_LONG_VALUEOF, false);
            break;
        case SALOAD:
        case SASTORE:
            mv.visitMethodInsn(INVOKESTATIC, CLASS_SHORT, METHOD_VALUEOF, DESC_SHORT_VALUEOF, false);
            break;
        }
    }

    @Override
    public void visitInsn(int opcode) {

        switch (opcode) {
        case AALOAD:
            if (!isInit) {
                String sig_loc = source + "|"
                        + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
                int ID = globalState.getArrayLocationId(sig_loc);

                if (config.commandLine.agentOnlySharing) {
                    mv.visitInsn(DUP2);
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitInsn(opcode);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index2);
                    mv.visitVarInsn(ILOAD, index1);

                    addBipushInsn(0);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
                } else if (globalState.shouldInstrumentArray(sig_loc)) {
                    mv.visitInsn(DUP2);
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitInsn(opcode);
                    mv.visitInsn(DUP);
                    crntMaxIndex++;
                    int index3 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index3);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index2);
                    mv.visitVarInsn(ILOAD, index1);
                    mv.visitVarInsn(ALOAD, index3);

                    addBipushInsn(0);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS, false);
                } else
                    mv.visitInsn(opcode);

            } else
                mv.visitInsn(opcode);

            break;

        case BALOAD:
        case CALOAD:
        case SALOAD:
        case IALOAD:
            if (!isInit) {
                String sig_loc = source + "|"
                        + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
                int ID = globalState.getArrayLocationId(sig_loc);

                if (config.commandLine.agentOnlySharing) {
                    mv.visitInsn(DUP2);
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitInsn(opcode);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index2);
                    mv.visitVarInsn(ILOAD, index1);

                    addBipushInsn(0);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
                } else if (globalState.shouldInstrumentArray(sig_loc)) {
                    mv.visitInsn(DUP2);
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitInsn(opcode);
                    mv.visitInsn(DUP);
                    crntMaxIndex++;
                    int index3 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index3);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index2);
                    mv.visitVarInsn(ILOAD, index1);
                    mv.visitVarInsn(ILOAD, index3);

                    convertPrimitiveToObject(opcode);

                    addBipushInsn(0);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS, false);
                } else
                    mv.visitInsn(opcode);
            } else
                mv.visitInsn(opcode);
            break;
        case FALOAD:
            if (!isInit) {
                String sig_loc = source + "|"
                        + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
                int ID = globalState.getArrayLocationId(sig_loc);

                if (config.commandLine.agentOnlySharing) {
                    mv.visitInsn(DUP2);
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitInsn(opcode);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index2);
                    mv.visitVarInsn(ILOAD, index1);

                    addBipushInsn(0);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
                } else if (globalState.shouldInstrumentArray(sig_loc)) {
                    mv.visitInsn(DUP2);
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitInsn(opcode);
                    mv.visitInsn(DUP);
                    crntMaxIndex++;
                    int index3 = crntMaxIndex;
                    mv.visitVarInsn(FSTORE, index3);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index2);
                    mv.visitVarInsn(ILOAD, index1);
                    mv.visitVarInsn(FLOAD, index3);

                    convertPrimitiveToObject(opcode);

                    addBipushInsn(0);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS, false);
                } else
                    mv.visitInsn(opcode);
            } else
                mv.visitInsn(opcode);

            break;
        case DALOAD:
            if (!isInit) {
                String sig_loc = source + "|"
                        + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
                int ID = globalState.getArrayLocationId(sig_loc);

                if (config.commandLine.agentOnlySharing) {
                    mv.visitInsn(DUP2);
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitInsn(opcode);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index2);
                    mv.visitVarInsn(ILOAD, index1);

                    addBipushInsn(0);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
                } else if (globalState.shouldInstrumentArray(sig_loc)) {
                    mv.visitInsn(DUP2);
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitInsn(opcode);
                    mv.visitInsn(DUP2);// double
                    crntMaxIndex++;
                    int index3 = crntMaxIndex;
                    mv.visitVarInsn(DSTORE, index3);
                    crntMaxIndex++;

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index2);
                    mv.visitVarInsn(ILOAD, index1);
                    mv.visitVarInsn(DLOAD, index3);

                    convertPrimitiveToObject(opcode);

                    addBipushInsn(0);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS, false);
                } else
                    mv.visitInsn(opcode);
            } else
                mv.visitInsn(opcode);
            break;
        case LALOAD:
            if (!isInit) {
                String sig_loc = source + "|"
                        + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
                int ID = globalState.getArrayLocationId(sig_loc);

                if (config.commandLine.agentOnlySharing) {
                    mv.visitInsn(DUP2);
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitInsn(opcode);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index2);
                    mv.visitVarInsn(ILOAD, index1);

                    addBipushInsn(0);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
                } else if (globalState.shouldInstrumentArray(sig_loc)) {
                    mv.visitInsn(DUP2);
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index2);
                    mv.visitInsn(opcode);
                    mv.visitInsn(DUP2);// long
                    crntMaxIndex++;
                    int index3 = crntMaxIndex;
                    mv.visitVarInsn(LSTORE, index3);
                    crntMaxIndex++;

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index2);
                    mv.visitVarInsn(ILOAD, index1);
                    mv.visitVarInsn(LLOAD, index3);

                    convertPrimitiveToObject(opcode);

                    addBipushInsn(0);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS, false);
                } else
                    mv.visitInsn(opcode);
            } else
                mv.visitInsn(opcode);
            break;
        case AASTORE: {
            String sig_loc = source + "|"
                    + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
            int ID = globalState.getArrayLocationId(sig_loc);

            if (config.commandLine.agentOnlySharing) {
                if (!isInit) {
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index2);

                    mv.visitInsn(DUP);
                    crntMaxIndex++;
                    int index3 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index3);// arrayref
                    mv.visitVarInsn(ILOAD, index2);// index
                    mv.visitVarInsn(ALOAD, index1);// value

                    mv.visitInsn(opcode);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index3);
                    mv.visitVarInsn(ILOAD, index2);

                    addBipushInsn(1);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
                } else
                    mv.visitInsn(opcode);

            } else if (globalState.shouldInstrumentArray(sig_loc)) {
                crntMaxIndex++;
                int index1 = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index1);
                crntMaxIndex++;
                int index2 = crntMaxIndex;
                mv.visitVarInsn(ISTORE, index2);

                mv.visitInsn(DUP);
                crntMaxIndex++;
                int index3 = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index3);// arrayref
                mv.visitVarInsn(ILOAD, index2);// index
                mv.visitVarInsn(ALOAD, index1);// value

                mv.visitInsn(opcode);

                addBipushInsn(ID);
                mv.visitVarInsn(ALOAD, index3);
                mv.visitVarInsn(ILOAD, index2);
                mv.visitVarInsn(ALOAD, index1);

                if (isInit) {
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                            DESC_LOG_INIT_WRITE_ACCESS, false);
                } else {
                    addBipushInsn(1);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS, false);
                }
            } else
                mv.visitInsn(opcode);
            break;
        }
        case BASTORE:
        case CASTORE:
        case SASTORE:
        case IASTORE: {
            String sig_loc = source + "|"
                    + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
            int ID = globalState.getArrayLocationId(sig_loc);

            if (config.commandLine.agentOnlySharing) {
                if (!isInit) {
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index2);

                    mv.visitInsn(DUP);
                    crntMaxIndex++;
                    int index3 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index3);// arrayref
                    mv.visitVarInsn(ILOAD, index2);// index
                    mv.visitVarInsn(ILOAD, index1);// value

                    mv.visitInsn(opcode);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index3);
                    mv.visitVarInsn(ILOAD, index2);

                    addBipushInsn(1);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
                } else
                    mv.visitInsn(opcode);
            } else if (globalState.shouldInstrumentArray(sig_loc)) {
                crntMaxIndex++;
                int index1 = crntMaxIndex;
                mv.visitVarInsn(ISTORE, index1);
                crntMaxIndex++;
                int index2 = crntMaxIndex;
                mv.visitVarInsn(ISTORE, index2);

                mv.visitInsn(DUP);
                crntMaxIndex++;
                int index3 = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index3);// arrayref
                mv.visitVarInsn(ILOAD, index2);// index
                mv.visitVarInsn(ILOAD, index1);// value

                mv.visitInsn(opcode);

                addBipushInsn(ID);
                mv.visitVarInsn(ALOAD, index3);
                mv.visitVarInsn(ILOAD, index2);
                mv.visitVarInsn(ILOAD, index1);
                convertPrimitiveToObject(opcode);

                if (isInit) {
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                            DESC_LOG_INIT_WRITE_ACCESS, false);
                } else {
                    addBipushInsn(1);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS, false);
                }
            } else
                mv.visitInsn(opcode);
            break;
        }
        case FASTORE: {
            String sig_loc = source + "|"
                    + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
            int ID = globalState.getArrayLocationId(sig_loc);

            if (config.commandLine.agentOnlySharing) {
                if (!isInit) {
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(FSTORE, index1);
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index2);

                    mv.visitInsn(DUP);
                    crntMaxIndex++;
                    int index3 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index3);// arrayref
                    mv.visitVarInsn(ILOAD, index2);// index
                    mv.visitVarInsn(FLOAD, index1);// value

                    mv.visitInsn(opcode);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index3);
                    mv.visitVarInsn(ILOAD, index2);

                    addBipushInsn(1);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
                } else
                    mv.visitInsn(opcode);
            } else if (globalState.shouldInstrumentArray(sig_loc)) {
                crntMaxIndex++;
                int index1 = crntMaxIndex;
                mv.visitVarInsn(FSTORE, index1);
                crntMaxIndex++;
                int index2 = crntMaxIndex;
                mv.visitVarInsn(ISTORE, index2);

                mv.visitInsn(DUP);
                crntMaxIndex++;
                int index3 = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index3);// arrayref
                mv.visitVarInsn(ILOAD, index2);// index
                mv.visitVarInsn(FLOAD, index1);// value

                mv.visitInsn(opcode);

                addBipushInsn(ID);
                mv.visitVarInsn(ALOAD, index3);
                mv.visitVarInsn(ILOAD, index2);
                mv.visitVarInsn(FLOAD, index1);
                convertPrimitiveToObject(opcode);

                if (isInit) {
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                            DESC_LOG_INIT_WRITE_ACCESS, false);
                } else {
                    addBipushInsn(1);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS, false);
                }
            } else
                mv.visitInsn(opcode);
            break;
        }
        case DASTORE: {
            String sig_loc = source + "|"
                    + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
            int ID = globalState.getArrayLocationId(sig_loc);

            if (config.commandLine.agentOnlySharing) {
                if (!isInit) {
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(DSTORE, index1);
                    crntMaxIndex++;
                    mv.visitInsn(DUP2);// dup arrayref and index
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index2);// index
                    crntMaxIndex++;
                    int index3 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index3);// arrayref

                    mv.visitVarInsn(DLOAD, index1);// double value

                    mv.visitInsn(opcode);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index3);
                    mv.visitVarInsn(ILOAD, index2);

                    addBipushInsn(1);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
                } else
                    mv.visitInsn(opcode);
            } else if (globalState.shouldInstrumentArray(sig_loc)) {
                crntMaxIndex++;
                int index1 = crntMaxIndex;
                mv.visitVarInsn(DSTORE, index1);
                crntMaxIndex++;
                mv.visitInsn(DUP2);// dup arrayref and index
                crntMaxIndex++;
                int index2 = crntMaxIndex;
                mv.visitVarInsn(ISTORE, index2);// index
                crntMaxIndex++;
                int index3 = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index3);// arrayref

                mv.visitVarInsn(DLOAD, index1);// double value

                mv.visitInsn(opcode);

                addBipushInsn(ID);
                mv.visitVarInsn(ALOAD, index3);
                mv.visitVarInsn(ILOAD, index2);
                mv.visitVarInsn(DLOAD, index1);
                convertPrimitiveToObject(opcode);

                if (isInit) {
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                            DESC_LOG_INIT_WRITE_ACCESS, false);
                } else {
                    addBipushInsn(1);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS, false);
                }
            } else
                mv.visitInsn(opcode);
            break;
        }
        case LASTORE: {
            String sig_loc = source + "|"
                    + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
            int ID = globalState.getArrayLocationId(sig_loc);

            if (config.commandLine.agentOnlySharing) {
                if (!isInit) {
                    crntMaxIndex++;
                    int index1 = crntMaxIndex;
                    mv.visitVarInsn(LSTORE, index1);
                    crntMaxIndex++;
                    mv.visitInsn(DUP2);// dup arrayref and index
                    crntMaxIndex++;
                    int index2 = crntMaxIndex;
                    mv.visitVarInsn(ISTORE, index2);// index
                    crntMaxIndex++;
                    int index3 = crntMaxIndex;
                    mv.visitVarInsn(ASTORE, index3);// arrayref

                    mv.visitVarInsn(LLOAD, index1);// double value

                    mv.visitInsn(opcode);

                    addBipushInsn(ID);
                    mv.visitVarInsn(ALOAD, index3);
                    mv.visitVarInsn(ILOAD, index2);

                    addBipushInsn(1);

                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
                } else
                    mv.visitInsn(opcode);
            } else if (globalState.shouldInstrumentArray(sig_loc)) {
                crntMaxIndex++;
                int index1 = crntMaxIndex;
                mv.visitVarInsn(LSTORE, index1);
                crntMaxIndex++;
                mv.visitInsn(DUP2);// dup arrayref and index
                crntMaxIndex++;
                int index2 = crntMaxIndex;
                mv.visitVarInsn(ISTORE, index2);// index
                crntMaxIndex++;
                int index3 = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index3);// arrayref

                mv.visitVarInsn(LLOAD, index1);// double value

                mv.visitInsn(opcode);

                addBipushInsn(ID);
                mv.visitVarInsn(ALOAD, index3);
                mv.visitVarInsn(ILOAD, index2);
                mv.visitVarInsn(LLOAD, index1);
                convertPrimitiveToObject(opcode);

                if (isInit) {
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_INIT_WRITE_ACCESS,
                            DESC_LOG_INIT_WRITE_ACCESS, false);
                } else {
                    addBipushInsn(1);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                            DESC_LOG_ARRAY_ACCESS, false);
                }
            } else
                mv.visitInsn(opcode);
            break;
        }
        case MONITORENTER: {
            if (!config.commandLine.agentOnlySharing) {
                String sig_loc = source + "|"
                        + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
                int ID = globalState.getLocationId(sig_loc);

                mv.visitInsn(DUP);
                crntMaxIndex++;
                int index = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index);// objectref
                mv.visitInsn(opcode);
                addBipushInsn(ID);
                mv.visitVarInsn(ALOAD, index);
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_LOCK_INSTANCE,
                        DESC_LOG_LOCK_INSTANCE, false);
            } else
                mv.visitInsn(opcode);
            break;
        }
        case MONITOREXIT: {
            if (!config.commandLine.agentOnlySharing) {
                String sig_loc = source + "|"
                        + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
                int ID = globalState.getLocationId(sig_loc);

                mv.visitInsn(DUP);
                crntMaxIndex++;
                int index = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index);// objectref
                addBipushInsn(ID);
                mv.visitVarInsn(ALOAD, index);
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_UNLOCK_INSTANCE,
                        DESC_LOG_UNLOCK_INSTANCE, false);
            }
            mv.visitInsn(opcode);
            break;
        }
        case IRETURN:
        case LRETURN:
        case FRETURN:
        case DRETURN:
        case ARETURN:
        case RETURN:
        case ATHROW:
            if (isSynchronized && !config.commandLine.agentOnlySharing) {
                String sig_loc = source + "|"
                        + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
                int ID = globalState.getLocationId(sig_loc);

                addBipushInsn(ID);

                if (isStatic) {
                    // signature + line number
                    String sig_var = (classname + ".0").replace("/", ".");
                    int SID = globalState.getVariableId(sig_var);
                    addBipushInsn(SID);
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_UNLOCK_STATIC,
                            DESC_LOG_UNLOCK_STATIC, false);
                } else {
                    mv.visitVarInsn(ALOAD, 0);// the this objectref
                    mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_UNLOCK_INSTANCE,
                            DESC_LOG_UNLOCK_INSTANCE, false);
                }
            }
            mv.visitInsn(opcode);
            break;
        default:
            mv.visitInsn(opcode);
            break;
        }
    }

    @Override
    public void visitCode() {
        mv.visitCode();

        if (isSynchronized && !config.commandLine.agentOnlySharing) {
            String sig_loc = source + "|"
                    + (classname + "|" + methodsignature + "|" + crntLineNum).replace("/", ".");
            int ID = globalState.getLocationId(sig_loc);

            addBipushInsn(ID);

            if (isStatic) {
                // signature + line number
                String sig_var = (classname + ".0").replace("/", ".");
                int SID = globalState.getVariableId(sig_var);
                addBipushInsn(SID);
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_LOCK_STATIC,
                        DESC_LOG_LOCK_STATIC, false);
            } else {
                mv.visitVarInsn(ALOAD, 0);// the this objectref
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_LOCK_INSTANCE,
                        DESC_LOG_LOCK_INSTANCE, false);
            }
        }

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
}
