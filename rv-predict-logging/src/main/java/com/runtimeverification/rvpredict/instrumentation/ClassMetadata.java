package com.runtimeverification.rvpredict.instrumentation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ClassMetadata implements Opcodes {

    private final String cname;
    private final String supername;
    private final ImmutableList<String> interfaces;
    private final ImmutableMap<String, Integer> fieldToAccessFlag;

    private ClassMetadata(String cname, String supername, ImmutableList<String> interfaces,
            ImmutableMap<String, Integer> fieldToAccess) {
        this.cname = cname;
        this.supername = supername;
        this.interfaces = interfaces;
        this.fieldToAccessFlag = fieldToAccess;
    }

    public String getClassName() {
        return cname;
    }

    public String getSuperName() {
        return supername;
    }

    public ImmutableList<String> getInterfaces() {
        return interfaces;
    }

    public ImmutableSet<String> getFieldNames() {
        return fieldToAccessFlag.keySet();
    }

    public int getAccess(String fieldName) {
        return fieldToAccessFlag.get(fieldName);
    }

    public static ClassMetadata create(ClassReader cr) {
        final String cname = cr.getClassName();
        String supername = cr.getSuperName();
        ImmutableList<String> interfaces = ImmutableList.copyOf(cr.getInterfaces());
        final ImmutableMap.Builder<String, Integer> mapBuilder = ImmutableMap.builder();

        ClassVisitor cv = new ClassVisitor(ASM5) {
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature,
                    Object value) {
                mapBuilder.put(name, access);
                // TODO(YilongL): this seems adhoc; find a better way
                if ((access & ACC_VOLATILE) != 0) {
                    Metadata.addVolatileVariable(cname, name);
                }
                return null;
            }
        };
        cr.accept(cv, ClassReader.SKIP_CODE);
        return new ClassMetadata(cname, supername, interfaces, mapBuilder.build());
    }

}