package com.runtimeverification.rvpredict.instrument.transformer;

import org.objectweb.asm.ClassReader;

import com.runtimeverification.rvpredict.metadata.ClassFile;
import com.runtimeverification.rvpredict.util.Logger;

public class ClassWriter extends org.objectweb.asm.ClassWriter {

    private final ClassLoader loader;

    private final String className;

    private final Logger logger;

    public ClassWriter(ClassReader classReader, ClassLoader loader, String className, Logger logger) {
        super(classReader, org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        this.loader = loader == null ? ClassLoader.getSystemClassLoader() : loader;
        this.className = className;
        this.logger = logger;
    }

    /**
     * The default implementation is fundamentally flawed because its use of
     * reflection to look up the class hierarchy. See <a href=
     * "http://chrononsystems.com/blog/java-7-design-flaw-leads-to-huge-backward-step-for-the-jvm"
     * >Java 7 Bytecode Verifier: Huge backward step for the JVM</a>.
     */
    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        if (type1.equals(type2)) {
            return type1;
        } else if (type1.equals("java/lang/Object") || type2.equals("java/lang/Object")) {
            return "java/lang/Object";
        }

        ClassFile class1 = ClassFile.getInstance(loader, type1);
        ClassFile class2 = ClassFile.getInstance(loader, type2);

        if (class1 != null && class1.isInterface() || class2 != null && class2.isInterface()) {
            return "java/lang/Object";
        }

        if (class1 == null || class2 == null) {
            logger.debug(String.format(
                    "Unable to find the common superclass of %s and %s while transforming %s",
                    type1, type2, className));
            /* since we couldn't find the class files, this part of the code is
             * probably dead; just return something that won't crash the JVM */
            return "java/lang/Object";
        }

        if (class1.isAssignableFrom(class2)) {
            return type1;
        } else if (class2.isAssignableFrom(class1)) {
            return type2;
        }

        do {
            class1 = class1.getSuperclass();
        } while (!class1.isAssignableFrom(class2));
        return class1.getClassName();
    }

}
