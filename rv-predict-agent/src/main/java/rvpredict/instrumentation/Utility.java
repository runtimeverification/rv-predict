package rvpredict.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

public class Utility {

    /**
     * Checks if one class or interface extends or implements another class or
     * interface.
     *
     * @param class0
     *            the name of the first class or interface
     * @param class1
     *            the name of the second class of interface
     * @return {@code true} if {@code class1} is assignable from {@code class0}
     */
    public static boolean isSubclassOf(String class0, String class1) {
        boolean itf = (getClassReader(class1).getAccess() & Opcodes.ACC_INTERFACE) != 0;
        return isSubclassOf(class0, class1, itf);
    }

    /**
     * Checks if one class or interface extends or implements another class or
     * interface.
     *
     * @param class0
     *            the name of the first class or interface
     * @param class1
     *            the name of the second class of interface
     * @param itf
     *            if {@code class1} represents an interface
     * @return {@code true} if {@code class1} is assignable from {@code class0}
     */
    public static boolean isSubclassOf(String class0, String class1, boolean itf) {
        assert !class1.startsWith("[");

        if (class0.startsWith("[")) {
            return class1.equals("java/lang/Object");
        }
        if (class0.equals(class1)) {
            return true;
        }

        List<String> superclasses = getSuperclasses(class0);
        if (!itf) {
            return superclasses.contains(class1);
        } else {
            boolean result = getInterfaces(class0).contains(class1);
            for (String superclass : superclasses) {
                result = result || getInterfaces(superclass).contains(class1);
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
     * @return the {@link ClassReader}
     */
    private static ClassReader getClassReader(String className) {
        try {
            return new ClassReader(className);
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
     * @return set of superclasses
     */
    private static List<String> getSuperclasses(String className) {
        List<String> result = new ArrayList<>();
        while (className != null) {
            className = getClassReader(className).getSuperName();
            if (className != null) {
                result.add(className);
            }
        }
        return result;
    }

    /**
     * Gets all implemented interfaces (including parent interfaces) of a class
     * or all parent interfaces of an interface.
     *
     * @param className
     *            the internal name of a class or interface
     * @return set of interfaces
     */
    private static Set<String> getInterfaces(String className) {
        Set<String> interfaces = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(className);
        while (!queue.isEmpty()) {
            String cls = queue.poll();
            for (String itf : getClassReader(cls).getInterfaces()) {
                if (interfaces.add(itf)) {
                    queue.add(itf);
                }
            }
        }
        return interfaces;
    }

}
