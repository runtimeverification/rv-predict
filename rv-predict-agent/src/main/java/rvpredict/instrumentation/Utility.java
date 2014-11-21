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

    public static boolean isPrimitiveTypeDesc(String desc) {
        return !desc.startsWith(DESC_ARRAY_PREFIX) && !desc.startsWith(DESC_OBJECT_PREFIX);
    }

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

    public static void addPrimitive2ObjectConv(MethodVisitor mv, int aloadOpCode) {
        String desc;
        switch (aloadOpCode) {
        case IALOAD: case IASTORE:
            desc = DESC_INT;
            break;
        case BALOAD: case BASTORE:
            // TODO(YilongL): is it a latent bug since we cannot tell from the
            // opcode that whether JVM is loading/storing a byte or a boolean
            desc = DESC_BOOL;
            break;
        case CALOAD: case CASTORE:
            desc = DESC_CHAR;
            break;
        case DALOAD: case DASTORE:
            desc = DESC_DOUBLE;
            break;
        case FALOAD: case FASTORE:
            desc = DESC_FLOAT;
            break;
        case LALOAD: case LASTORE:
            desc = DESC_LONG;
            break;
        case SALOAD: case SASTORE:
            desc = DESC_SHORT;
            break;
        default:
            desc = null;
            assert false : "Expected an array load opcode; but found: " + aloadOpCode;
        }

        addPrimitive2ObjectConv(mv, desc);
    }

    public static void addPrimitive2ObjectConv(MethodVisitor mv, String desc) {
        if (desc.startsWith(DESC_INT)) {
            mv.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, METHOD_VALUEOF,
                    DESC_INTEGER_VALUEOF, false);
        } else if (desc.startsWith(DESC_BYTE)) {
            mv.visitMethodInsn(INVOKESTATIC, BYTE_INTERNAL_NAME, METHOD_VALUEOF,
                    DESC_BYTE_VALUEOF, false);
        } else if (desc.startsWith(DESC_SHORT)) {
            mv.visitMethodInsn(INVOKESTATIC, SHORT_INTERNAL_NAME, METHOD_VALUEOF,
                    DESC_SHORT_VALUEOF, false);
        } else if (desc.startsWith(DESC_BOOL)) {
            mv.visitMethodInsn(INVOKESTATIC, BOOLEAN_INTERNAL_NAME, METHOD_VALUEOF,
                    DESC_BOOLEAN_VALUEOF, false);
        } else if (desc.startsWith(DESC_CHAR)) {
            mv.visitMethodInsn(INVOKESTATIC, CHARACTER_INTERNAL_NAME, METHOD_VALUEOF,
                    DESC_CHAR_VALUEOF, false);
        } else if (desc.startsWith(DESC_LONG)) {
            mv.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, METHOD_VALUEOF,
                    DESC_LONG_VALUEOF, false);
        } else if (desc.startsWith(DESC_FLOAT)) {
            mv.visitMethodInsn(INVOKESTATIC, FLOAT_INTERNAL_NAME, METHOD_VALUEOF,
                    DESC_FLOAT_VALUEOF, false);
        } else if (desc.startsWith(DESC_DOUBLE)) {
            mv.visitMethodInsn(INVOKESTATIC, DOUBLE_INTERNAL_NAME, METHOD_VALUEOF,
                    DESC_DOUBLE_VALUEOF, false);
        } else {
            assert false : "Expected primitive type descriptor; but found: " + desc;
        }
    }

    public static boolean isThreadClass(String className) {
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
                e.printStackTrace();
                return false;
            }
        }
        return false;
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
