package rvpredict.instrumentation;

import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.Type;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class Utility {

    public static final String DESC_INT    =   Type.INT_TYPE.getDescriptor();
    public static final String DESC_LONG   =   Type.LONG_TYPE.getDescriptor();
    public static final String DESC_BOOL   =   Type.BOOLEAN_TYPE.getDescriptor();
    public static final String DESC_BYTE   =   Type.BYTE_TYPE.getDescriptor();
    public static final String DESC_SHORT  =   Type.SHORT_TYPE.getDescriptor();
    public static final String DESC_CHAR   =   Type.CHAR_TYPE.getDescriptor();
    public static final String DESC_FLOAT  =   Type.FLOAT_TYPE.getDescriptor();
    public static final String DESC_DOUBLE =   Type.DOUBLE_TYPE.getDescriptor();
    public static final String DESC_ARRAY  =   "[";
    public static final String DESC_OBJECT =   "L";

    public static final String INTEGER_INTERNAL_NAME   =   Type.getInternalName(Integer.class);
    public static final String BOOLEAN_INTERNAL_NAME   =   Type.getInternalName(Boolean.class);
    public static final String CHARACTER_INTERNAL_NAME =   Type.getInternalName(Character.class);
    public static final String SHORT_INTERNAL_NAME     =   Type.getInternalName(Short.class);
    public static final String BYTE_INTERNAL_NAME      =   Type.getInternalName(Byte.class);
    public static final String LONG_INTERNAL_NAME      =   Type.getInternalName(Long.class);
    public static final String FLOAT_INTERNAL_NAME     =   Type.getInternalName(Float.class);
    public static final String DOUBLE_INTERNAL_NAME    =   Type.getInternalName(Double.class);

    public static final String METHOD_VALUEOF           =  "valueOf";
    public static final String DESC_INTEGER_VALUEOF    =   "(I)Ljava/lang/Integer;";
    public static final String DESC_BOOLEAN_VALUEOF    =   "(Z)Ljava/lang/Boolean;";
    public static final String DESC_BYTE_VALUEOF       =   "(B)Ljava/lang/Byte;";
    public static final String DESC_SHORT_VALUEOF      =   "(S)Ljava/lang/Short;";
    public static final String DESC_CHAR_VALUEOF       =   "(C)Ljava/lang/Character;";
    public static final String DESC_LONG_VALUEOF       =   "(J)Ljava/lang/Long;";
    public static final String DESC_FLOAT_VALUEOF      =   "(F)Ljava/lang/Float;";
    public static final String DESC_DOUBLE_VALUEOF     =   "(D)Ljava/lang/Double;";

    public static final int[] ICONST_X = {ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 }; 

    public static final ImmutableSet<String> SINGLE_WORD_TYPE_DESCS = ImmutableSet.of(
            Utility.DESC_INT, DESC_BYTE, DESC_SHORT, DESC_BOOL, DESC_CHAR, DESC_FLOAT, DESC_OBJECT, DESC_ARRAY);
    
    public static final ImmutableSet<String> DOUBLE_WORDS_TYPE_DESCS = ImmutableSet.of(
            DESC_DOUBLE, DESC_LONG);
    
    public static final ImmutableMap<String, Integer> STORE_OPCODES = ImmutableMap.<String, Integer>builder()
            .put(Utility.DESC_INT,    ISTORE)
            .put(DESC_BYTE,   ISTORE)
            .put(DESC_SHORT,  ISTORE)
            .put(DESC_BOOL,   ISTORE)
            .put(DESC_CHAR,   ISTORE)
            .put(DESC_OBJECT, ASTORE)
            .put(DESC_ARRAY,  ASTORE)
            .put(DESC_LONG,   LSTORE)
            .put(DESC_FLOAT,  FSTORE)
            .put(DESC_DOUBLE, DSTORE)
            .build();

    public static final ImmutableMap<String, Integer> LOAD_OPCODES = ImmutableMap.<String, Integer>builder()
            .put(Utility.DESC_INT,    ILOAD)
            .put(DESC_BYTE,   ILOAD)
            .put(DESC_SHORT,  ILOAD)
            .put(DESC_BOOL,   ILOAD)
            .put(DESC_CHAR,   ILOAD)
            .put(DESC_OBJECT, ALOAD)
            .put(DESC_ARRAY,  ALOAD)
            .put(DESC_LONG,   LLOAD)
            .put(DESC_FLOAT,  FLOAD)
            .put(DESC_DOUBLE, DLOAD)
            .build();

}
