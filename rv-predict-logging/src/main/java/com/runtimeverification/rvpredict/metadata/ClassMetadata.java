package com.runtimeverification.rvpredict.metadata;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ClassMetadata implements Opcodes {

    private final ClassLoader loader;
    private final String cname;
    private final String supername;
    private final ImmutableList<String> interfaces;
    private final ImmutableMap<String, Integer> fieldToAccessFlag;

    private ClassMetadata(ClassLoader loader, String cname, String supername,
            ImmutableList<String> interfaces, ImmutableMap<String, Integer> fieldToAccess) {
        this.loader = loader;
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

    public ClassLoader getClassLoader() {
        return loader;
    }

    public ImmutableSet<String> getFieldNames() {
        return fieldToAccessFlag.keySet();
    }

    public int getAccess(String fieldName) {
        return fieldToAccessFlag.get(fieldName);
    }

    @Override
    public int hashCode() {
        return cname.hashCode() * 17 + loader.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ClassMetadata) {
            ClassMetadata otherClassMetadata = (ClassMetadata) object;
            return cname.equals(otherClassMetadata) && loader.equals(otherClassMetadata.loader);
        }
        return false;
    }

    public static ClassMetadata create(ClassLoader loader, ClassReader cr) {
        final String cname = cr.getClassName();
        String supername = cr.getSuperName();
        ImmutableList<String> interfaces = ImmutableList.copyOf(cr.getInterfaces());
        final ImmutableMap.Builder<String, Integer> mapBuilder = ImmutableMap.builder();

        ClassVisitor cv = new ClassVisitor(ASM5) {
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature,
                    Object value) {
                mapBuilder.put(name, access);
                return null;
            }
        };
        cr.accept(cv, ClassReader.SKIP_CODE);
        return new ClassMetadata(loader, cname, supername, interfaces, mapBuilder.build());
    }

}