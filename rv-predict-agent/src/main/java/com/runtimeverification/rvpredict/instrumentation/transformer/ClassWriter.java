package com.runtimeverification.rvpredict.instrumentation.transformer;

import org.objectweb.asm.ClassReader;

import com.runtimeverification.rvpredict.metadata.ClassFile;

public class ClassWriter extends org.objectweb.asm.ClassWriter {

    public ClassWriter(ClassReader classReader, int flags) {
        super(classReader, flags);
    }

    /**
     * The default implementation is fundamentally flawed because its use of
     * reflection to look up the class hierarchy. See <a href=
     * "http://chrononsystems.com/blog/java-7-design-flaw-leads-to-huge-backward-step-for-the-jvm"
     * >Java 7 Bytecode Verifier: Huge backward step for the JVM</a>.
     */
    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        ClassLoader loader = getClass().getClassLoader();
        ClassFile class1 = ClassFile.getInstance(loader, type1);
        ClassFile class2 = ClassFile.getInstance(loader, type2);
        if (class1.isAssignableFrom(class2)) {
            return type1;
        } else if (class2.isAssignableFrom(class1)) {
            return type2;
        }

        if (class1.isInterface() || class2.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                class1 = class1.getSuperclass();
            } while (!class1.isAssignableFrom(class2));
            return class1.getClassName();
        }
    }

}
