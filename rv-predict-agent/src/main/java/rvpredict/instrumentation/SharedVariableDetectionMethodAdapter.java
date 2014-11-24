package rvpredict.instrumentation;

import static org.objectweb.asm.Opcodes.*;
import static rvpredict.config.Config.*;
import static rvpredict.instrumentation.Utility.*;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import rvpredict.config.Config;

/**
 * Instruments method to detect shared variables.
 *
 * TODO(YilongL): extract common code with {@code SnoopInstructionMethodAdapter}
 * to an abstract class.
 *
 * @author YilongL
 *
 */
public class SharedVariableDetectionMethodAdapter extends MethodVisitor {

    private final String className;
    private final String source;
    private final String methodName;
    private final String signature;

    /**
     * Specifies whether the visited method is an initialization method.
     */
    private final boolean isInit;

    private final Config config;

    private final GlobalStateForInstrumentation globalState;

    /**
     * current max index of local variables
     */
    private int crntMaxIndex;
    private int crntLineNum;

    public SharedVariableDetectionMethodAdapter(MethodVisitor mv, String source, String className,
            String methodName, String signature, int access, int argSize, Config config,
            GlobalStateForInstrumentation globalState) {
        super(Opcodes.ASM5, mv);
        this.source = source == null ? "Unknown" : source;
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
        this.isInit = "<init>".equals(methodName) || "<clinit>".equals(methodName);
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

                addPushConstInsn(mv, ID);
                // mv.visitInsn(ACONST_NULL);
                addPushConstInsn(mv, SID);
                addPushConstInsn(mv, 0);
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                        DESC_LOG_FIELD_ACCESS_DETECT_SHARING, false);
            }
            break;
        case PUTSTATIC:
            mv.visitFieldInsn(opcode, owner, name, desc);

            if (!isInit) {
                addPushConstInsn(mv, ID);
                // mv.visitInsn(ACONST_NULL);
                addPushConstInsn(mv, SID);

                addPushConstInsn(mv, 1);
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                        DESC_LOG_FIELD_ACCESS_DETECT_SHARING, false);
            }

            break;
        case GETFIELD:
            if (!isInit) {
                mv.visitFieldInsn(opcode, owner, name, desc);

                addPushConstInsn(mv, ID);
                // mv.visitVarInsn(ALOAD, index1);
                addPushConstInsn(mv, SID);

                addPushConstInsn(mv, 0);

                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                        DESC_LOG_FIELD_ACCESS_DETECT_SHARING, false);
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

                addPushConstInsn(mv, ID);
                // mv.visitVarInsn(ALOAD, index2);
                addPushConstInsn(mv, SID);

                addPushConstInsn(mv, 1);
                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_FIELD_ACCESS,
                        DESC_LOG_FIELD_ACCESS_DETECT_SHARING, false);
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
        case AALOAD:
            if (!isInit) {
                String sig_loc = getSignaturePlusLoc();
                int ID = globalState.getLocationId(sig_loc);

                mv.visitInsn(DUP2);
                crntMaxIndex++;
                int index1 = crntMaxIndex;
                mv.visitVarInsn(ISTORE, index1);
                crntMaxIndex++;
                int index2 = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index2);
                mv.visitInsn(opcode);

                addPushConstInsn(mv, ID);
                mv.visitVarInsn(ALOAD, index2);
                mv.visitVarInsn(ILOAD, index1);

                addPushConstInsn(mv, 0);

                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                        DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);

            } else
                mv.visitInsn(opcode);

            break;

        case BALOAD:
        case CALOAD:
        case SALOAD:
        case IALOAD:
            if (!isInit) {
                String sig_loc = getSignaturePlusLoc();
                int ID = globalState.getLocationId(sig_loc);

                mv.visitInsn(DUP2);
                crntMaxIndex++;
                int index1 = crntMaxIndex;
                mv.visitVarInsn(ISTORE, index1);
                crntMaxIndex++;
                int index2 = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index2);
                mv.visitInsn(opcode);

                addPushConstInsn(mv, ID);
                mv.visitVarInsn(ALOAD, index2);
                mv.visitVarInsn(ILOAD, index1);

                addPushConstInsn(mv, 0);

                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                        DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
            } else
                mv.visitInsn(opcode);
            break;
        case FALOAD:
            if (!isInit) {
                String sig_loc = getSignaturePlusLoc();
                int ID = globalState.getLocationId(sig_loc);

                mv.visitInsn(DUP2);
                crntMaxIndex++;
                int index1 = crntMaxIndex;
                mv.visitVarInsn(ISTORE, index1);
                crntMaxIndex++;
                int index2 = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index2);
                mv.visitInsn(opcode);

                addPushConstInsn(mv, ID);
                mv.visitVarInsn(ALOAD, index2);
                mv.visitVarInsn(ILOAD, index1);

                addPushConstInsn(mv, 0);

                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                        DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
            } else
                mv.visitInsn(opcode);

            break;
        case DALOAD:
            if (!isInit) {
                String sig_loc = getSignaturePlusLoc();
                int ID = globalState.getLocationId(sig_loc);

                mv.visitInsn(DUP2);
                crntMaxIndex++;
                int index1 = crntMaxIndex;
                mv.visitVarInsn(ISTORE, index1);
                crntMaxIndex++;
                int index2 = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index2);
                mv.visitInsn(opcode);

                addPushConstInsn(mv, ID);
                mv.visitVarInsn(ALOAD, index2);
                mv.visitVarInsn(ILOAD, index1);

                addPushConstInsn(mv, 0);

                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                        DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
            } else
                mv.visitInsn(opcode);
            break;
        case LALOAD:
            if (!isInit) {
                String sig_loc = getSignaturePlusLoc();
                int ID = globalState.getLocationId(sig_loc);

                mv.visitInsn(DUP2);
                crntMaxIndex++;
                int index1 = crntMaxIndex;
                mv.visitVarInsn(ISTORE, index1);
                crntMaxIndex++;
                int index2 = crntMaxIndex;
                mv.visitVarInsn(ASTORE, index2);
                mv.visitInsn(opcode);

                addPushConstInsn(mv, ID);
                mv.visitVarInsn(ALOAD, index2);
                mv.visitVarInsn(ILOAD, index1);

                addPushConstInsn(mv, 0);

                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                        DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
            } else
                mv.visitInsn(opcode);
            break;
        case AASTORE: {
            String sig_loc = getSignaturePlusLoc();
            int ID = globalState.getLocationId(sig_loc);

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

                addPushConstInsn(mv, ID);
                mv.visitVarInsn(ALOAD, index3);
                mv.visitVarInsn(ILOAD, index2);

                addPushConstInsn(mv, 1);

                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                        DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
            } else
                mv.visitInsn(opcode);
            break;
        }
        case BASTORE:
        case CASTORE:
        case SASTORE:
        case IASTORE: {
            String sig_loc = getSignaturePlusLoc();
            int ID = globalState.getLocationId(sig_loc);

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

                addPushConstInsn(mv, ID);
                mv.visitVarInsn(ALOAD, index3);
                mv.visitVarInsn(ILOAD, index2);

                addPushConstInsn(mv, 1);

                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                        DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
            } else
                mv.visitInsn(opcode);
            break;
        }
        case FASTORE: {
            String sig_loc = getSignaturePlusLoc();
            int ID = globalState.getLocationId(sig_loc);

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

                addPushConstInsn(mv, ID);
                mv.visitVarInsn(ALOAD, index3);
                mv.visitVarInsn(ILOAD, index2);

                addPushConstInsn(mv, 1);

                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                        DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
            } else
                mv.visitInsn(opcode);
            break;
        }
        case DASTORE: {
            String sig_loc = getSignaturePlusLoc();
            int ID = globalState.getLocationId(sig_loc);

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

                addPushConstInsn(mv, ID);
                mv.visitVarInsn(ALOAD, index3);
                mv.visitVarInsn(ILOAD, index2);

                addPushConstInsn(mv, 1);

                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                        DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
            } else
                mv.visitInsn(opcode);
            break;
        }
        case LASTORE: {
            String sig_loc = getSignaturePlusLoc();
            int ID = globalState.getLocationId(sig_loc);

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

                addPushConstInsn(mv, ID);
                mv.visitVarInsn(ALOAD, index3);
                mv.visitVarInsn(ILOAD, index2);

                addPushConstInsn(mv, 1);

                mv.visitMethodInsn(INVOKESTATIC, config.logClass, LOG_ARRAY_ACCESS,
                        DESC_LOG_ARRAY_ACCESS_DETECT_SHARING, false);
            } else
                mv.visitInsn(opcode);
            break;
        }
        case MONITORENTER:
        case MONITOREXIT:
        case IRETURN:
        case LRETURN:
        case FRETURN:
        case DRETURN:
        case ARETURN:
        case RETURN:
        case ATHROW:
        default:
            mv.visitInsn(opcode);
            break;
        }
    }

    private String getSignaturePlusLoc() {
        return source + "|"
                + (className + "|" + signature + "|" + crntLineNum).replace("/", ".");
    }
}
