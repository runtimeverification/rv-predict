package com.runtimeverification.rvpredict.instrumentation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.runtimeverification.rvpredict.runtime.RVPredictRuntime;

public class InstrumentationUtils {

    public static final Type OBJECT_TYPE    = Type.getObjectType("java/lang/Object");
    public static final Type CLASS_TYPE     = Type.getObjectType("java/lang/Class");
    public static final Type JL_FLOAT_TYPE  = Type.getObjectType("java/lang/Float");
    public static final Type JL_DOUBLE_TYPE = Type.getObjectType("java/lang/Double");
    public static final Type JL_SYSTEM_TYPE = Type.getObjectType("java/lang/System");
    public static final Type RVPREDICT_RUNTIME_TYPE = Type.getType(RVPredictRuntime.class);

    /**
     * Checks if one class or interface extends or implements another class or
     * interface.
     *
     * @param loader
     *            the defining loader of {@code class0}, may be null if it is
     *            the bootstrap class loader or unknown
     * @param class0
     *            the name of the first class or interface
     * @param class1
     *            the name of the second class of interface
     * @return {@code true} if {@code class1} is assignable from {@code class0}
     */
    public static boolean isSubclassOf(ClassLoader loader, String class0, String class1) {
        assert !class1.startsWith("[");

        if (class0.startsWith("[")) {
            return class1.equals("java/lang/Object");
        }
        if (class0.equals(class1)) {
            return true;
        }

        boolean itf = (getClassReader(class1, loader).getAccess() & Opcodes.ACC_INTERFACE) != 0;
        Set<String> superclasses = getSuperclasses(class0, loader);
        if (!itf) {
            return superclasses.contains(class1);
        } else {
            boolean result = getInterfaces(class0, loader).contains(class1);
            for (String superclass : superclasses) {
                result = result || getInterfaces(superclass, loader).contains(class1);
            }
            return result;
        }
    }

    /**
     * Obtains the {@link ClassReader} used to retrieve the superclass and
     * interfaces information of a class or interface.
     * <p>
     * This method is meant to avoid class loading when checking inheritance
     * relation because class loading during
     * {@link ClassFileTransformer#transform(ClassLoader, String, Class, java.security.ProtectionDomain, byte[])}
     * can not be properly intercepted by the java agent.
     *
     * @param className
     *            the class or interface to read
     * @param loader
     *            the defining loader of the class, may be null if it is the
     *            bootstrap class loader or unknown
     * @return the {@link ClassReader}
     */
    private static ClassReader getClassReader(String className, ClassLoader loader) {
        try {
            return loader == null ? new ClassReader(className) : new ClassReader(
                    loader.getResourceAsStream(className + ".class"));
        } catch (IOException e) {
            System.err.println("ASM ClassReader: unable to read " + className);
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets all superclasses of a class or interface.
     * <p>
     * The superclass of an interface will be the {@code Object}.
     *
     * @param className
     *            the internal name of a class or interface
     * @param loader
     *            the defining loader of the class, may be null if it is the
     *            bootstrap class loader or unknown
     * @return set of superclasses
     */
    private static Set<String> getSuperclasses(String className, ClassLoader loader) {
        Set<String> result = new HashSet<>();
        String superclassName;
        while (className != null) {
            superclassName = Metadata.classNameToSuperclassName.get(className);
            if (superclassName == null) {
                superclassName = getClassReader(className, loader).getSuperName();
                Metadata.setSuperclass(className, superclassName);
            }

            if (superclassName != null) {
                result.add(superclassName);
            }
            className = superclassName;
        }
        return result;
    }

    /**
     * Gets all implemented interfaces (including parent interfaces) of a class
     * or all parent interfaces of an interface.
     *
     * @param className
     *            the internal name of a class or interface
     * @param loader
     *            the defining loader of the class, may be null if it is the
     *            bootstrap class loader or unknown
     * @return set of interfaces
     */
    private static Set<String> getInterfaces(String className, ClassLoader loader) {
        Set<String> interfaces = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(className);
        while (!queue.isEmpty()) {
            String cls = queue.poll();
            String[] itfs = Metadata.classNameToInterfaceNames.get(cls);
            if (itfs == null) {
                itfs = getClassReader(cls, loader).getInterfaces();
                Metadata.setInterfaces(cls, itfs);
            }

            for (String itf : itfs) {
                if (interfaces.add(itf)) {
                    queue.add(itf);
                }
            }
        }
        return interfaces;
    }

    public static void printTransformedClassToFile(String cname, byte[] cbuf, String dir) {
        String fileName = dir + "/" + cname.substring(cname.lastIndexOf("/") + 1) + ".class";
        File f = new File(fileName);

        try {
            OutputStream out = new FileOutputStream(f);
            out.write(cbuf);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
