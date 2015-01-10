package rvpredict.instrumentation;

import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class Utility {

    public static final String DESC_INT    =   Type.INT_TYPE.getDescriptor();
    public static final String DESC_LONG   =   Type.LONG_TYPE.getDescriptor();
    public static final String DESC_BOOL   =   Type.BOOLEAN_TYPE.getDescriptor();
    public static final String DESC_BYTE   =   Type.BYTE_TYPE.getDescriptor();
    public static final String DESC_SHORT  =   Type.SHORT_TYPE.getDescriptor();
    public static final String DESC_CHAR   =   Type.CHAR_TYPE.getDescriptor();
    public static final String DESC_FLOAT  =   Type.FLOAT_TYPE.getDescriptor();
    public static final String DESC_DOUBLE =   Type.DOUBLE_TYPE.getDescriptor();
    public static final String DESC_STRING =   Type.getDescriptor(String.class);
    public static final String DESC_CLASS  =   "Ljava/lang/Class;";

    public static final String DESC_ARRAY_PREFIX    =   "[";
    public static final String DESC_OBJECT_PREFIX   =   "L";

    public static final String INTEGER_INTERNAL_NAME   =   Type.getInternalName(Integer.class);
    public static final String BOOLEAN_INTERNAL_NAME   =   Type.getInternalName(Boolean.class);
    public static final String CHARACTER_INTERNAL_NAME =   Type.getInternalName(Character.class);
    public static final String SHORT_INTERNAL_NAME     =   Type.getInternalName(Short.class);
    public static final String BYTE_INTERNAL_NAME      =   Type.getInternalName(Byte.class);
    public static final String LONG_INTERNAL_NAME      =   Type.getInternalName(Long.class);
    public static final String FLOAT_INTERNAL_NAME     =   Type.getInternalName(Float.class);
    public static final String DOUBLE_INTERNAL_NAME    =   Type.getInternalName(Double.class);

    public static final String METHOD_VALUEOF          =  "valueOf";
    public static final String DESC_INTEGER_VALUEOF    =   "(I)Ljava/lang/Integer;";
    public static final String DESC_BOOLEAN_VALUEOF    =   "(Z)Ljava/lang/Boolean;";
    public static final String DESC_BYTE_VALUEOF       =   "(B)Ljava/lang/Byte;";
    public static final String DESC_SHORT_VALUEOF      =   "(S)Ljava/lang/Short;";
    public static final String DESC_CHAR_VALUEOF       =   "(C)Ljava/lang/Character;";
    public static final String DESC_LONG_VALUEOF       =   "(J)Ljava/lang/Long;";
    public static final String DESC_FLOAT_VALUEOF      =   "(F)Ljava/lang/Float;";
    public static final String DESC_DOUBLE_VALUEOF     =   "(D)Ljava/lang/Double;";

    public static final int[] ICONST_X = {ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 };

    public static boolean isSingleWordTypeDesc(String desc) {
        return !desc.startsWith(DESC_LONG) && !desc.startsWith(DESC_DOUBLE);
    }

    public static boolean isDoubleWordTypeDesc(String desc) {
        return desc.startsWith(DESC_LONG) || desc.startsWith(DESC_DOUBLE);
    }

    public static int getElementLoadOpcode(int arrayLoadOrStoreOpcode) {
        switch (arrayLoadOrStoreOpcode) {
        case AALOAD: case AASTORE:
            return ALOAD;
        case BALOAD: case CALOAD: case SALOAD: case IALOAD:
        case BASTORE: case CASTORE: case SASTORE: case IASTORE:
            return ILOAD;
        case LALOAD: case LASTORE:
            return LLOAD;
        case FALOAD: case FASTORE:
            return FLOAD;
        case DALOAD: case DASTORE:
            return DLOAD;
        default:
            assert false : "Expected xALOAD or xASTORE opcode; but found: " + arrayLoadOrStoreOpcode;
            return -1;
        }
    }

    public static int getElementStoreOpcode(int arrayLoadOrStoreOpcode) {
        switch (arrayLoadOrStoreOpcode) {
        case AALOAD: case AASTORE:
            return ASTORE;
        case BALOAD: case CALOAD: case SALOAD: case IALOAD:
        case BASTORE: case CASTORE: case SASTORE: case IASTORE:
            return ISTORE;
        case LALOAD: case LASTORE:
            return LSTORE;
        case FALOAD: case FASTORE:
            return FSTORE;
        case DALOAD: case DASTORE:
            return DSTORE;
        default:
            assert false : "Expected xALOAD or xASTORE opcode; but found: " + arrayLoadOrStoreOpcode;
            return -1;
        }
    }

    public static boolean isElementSingleWord(int arrayLoadOrStoreOpcode) {
        switch (arrayLoadOrStoreOpcode) {
        case LALOAD: case LASTORE:
        case DALOAD: case DASTORE:
            return false;
        default:
            return true;
        }
    }

    public static void calcLongValue(MethodVisitor mv, int arrayLoadOrStoreOpcode) {
        switch (arrayLoadOrStoreOpcode) {
        case BALOAD: case BASTORE:
        case CALOAD: case CASTORE:
        case SALOAD: case SASTORE:
        case IALOAD: case IASTORE:
            mv.visitInsn(I2L);
            break;
        case LALOAD: case LASTORE:
            break; // do nothing
        case AALOAD: case AASTORE:
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                    "(Ljava/lang/Object;)I", false);
            mv.visitInsn(I2L);
            break;
        case DALOAD:
        case DASTORE:
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "doubleToLongBits", "(D)J",
                    false);
            break;
        case FALOAD:
        case FASTORE:
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "floatToIntBits", "(F)I", false);
            mv.visitInsn(I2L);
            break;
        default:
            assert false : "Expected an array load/store opcode; but found: " + arrayLoadOrStoreOpcode;
        }
    }

    public static void calcLongValue(MethodVisitor mv, String desc) {
        calcLongValue(mv, Type.getType(desc).getOpcode(IALOAD));
    }

    public static boolean isSubclassOf(String class0, String class1) {
        if (class0.equals(class1)) {
            return true;
        }

        switch (class1) {
        case "java/lang/Object":
            return true;
        case "java/lang/Thread":
            return isThreadClass(class0);
        case "java/util/concurrent/locks/Lock":
            return isLockClass(class0);
        case "java/util/concurrent/locks/Condition":
            return isConditionClass(class0);
        case "java/util/concurrent/locks/ReadWriteLock":
            return isReadWriteLockClass(class0);
        case "java/util/concurrent/locks/AbstractQueuedSynchronizer":
            return isAQSClass(class0);
        case "java/util/concurrent/atomic/AtomicBoolean":
            return false;
        default:
            System.err.println("[Warning]: unexpected case isSubclassOf(" + class0 + ", " + class1
                    + ")");
            return false;
        }
    }

    private static boolean isThreadClass(String className) {
        if (className.startsWith(DESC_ARRAY_PREFIX)) {
            return false;
        }

        while (className != null && !className.equals("java/lang/Object")) {
            if (className.equals("java/lang/Thread")) {
                return true;
            }

            try {
                className = new ClassReader(className).getSuperName();
            } catch (IOException e) {
                System.err.println("ASM ClassReader: unable to read " + className);
                return false;
            }
        }
        return false;
    }

    private static boolean isLockClass(String className) {
        // TODO(YilongL): avoid hard-coding like this
        return "java/util/concurrent/locks/Lock".equals(className)
            || "java/util/concurrent/locks/ReentrantLock".equals(className)
            || "java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock".equals(className)
            || "java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock".equals(className);
    }

    private static boolean isConditionClass(String className) {
        // TODO(YilongL): avoid hard-coding like this
        return "java/util/concurrent/locks/Condition".equals(className)
            || "java/util/concurrent/locks/AbstractQueuedSynchronizer$ConditionObject".equals(className)
            || "java/util/concurrent/locks/AbstractQueuedLongSynchronizer$ConditionObject".equals(className);
    }

    private static boolean isReadWriteLockClass(String className) {
        // TODO(YilongL): avoid hard-coding like this
        return "java/util/concurrent/locks/ReadWriteLock".equals(className)
            || "java/util/concurrent/locks/ReentrantReadWriteLock".equals(className);
    }

    private static boolean isAQSClass(String class0) {
        // TODO(YilongL): avoid hard-coding like this
        return "java/util/concurrent/Semaphore$Sync".equals(class0)
            || "java/util/concurrent/Semaphore$FairSync".equals(class0)
            || "java/util/concurrent/Semaphore$NonfairSync".equals(class0)
            || "java/util/concurrent/CountDownLatch$Sync".equals(class0);
    }

    /**
     * Helper method that adds a instruction which pushes a constant value onto
     * the operand stack.
     *
     * @param value
     *            the constant value to be pushed to the stack
     */
    public static void addPushConstInsn(MethodVisitor mv, int value) {
        if ((0 <= value) && (value <= 5)) {
            mv.visitInsn(ICONST_X[value]);
        } else if ((Byte.MIN_VALUE <= value) && (value <= Byte.MAX_VALUE)) {
            mv.visitIntInsn(BIPUSH, value);
        } else if ((Short.MIN_VALUE <= value) && (value <= Short.MAX_VALUE)) {
            mv.visitIntInsn(SIPUSH, value);
        } else {
            mv.visitLdcInsn(new Integer(value));
        }
    }

}
