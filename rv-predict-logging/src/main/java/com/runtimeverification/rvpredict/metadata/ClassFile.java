package com.runtimeverification.rvpredict.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;

/**
 * Stores information of a (non-array) class.
 *
 * @author YilongL
 *
 */
public class ClassFile implements Opcodes {

    /**
     * Global cache table of all created class files. The table is indexed by
     * {@link #urlString} and {@link #cname} respectively.
     */
    private static final Table<String, String, ClassFile> classFileTable = HashBasedTable.create();

    private static final Metadata metadata = Metadata.singleton();

    /**
     * {@code String} representation of the {@link URL} used to locate this class file.
     */
    private final String urlString;

    private final int access;
    private final String cname;
    private final String supername;
    private final ImmutableList<String> interfaces;
    private final ImmutableMap<String, Integer> fieldToAccessFlag;

    /**
     * Some initiating classloader that is used to locate this class file.
     * <p>
     * TODO(YilongL): should we attempt to get the defining classloader?
     */
    private final ClassLoader loader;

    private ClassFile(ClassLoader loader, String urlString, int access, String cname,
            String supername, ImmutableList<String> interfaces,
            ImmutableMap<String, Integer> fieldToAccess) {
        this.loader = loader;
        this.urlString = urlString;
        this.access = access;
        this.cname = cname;
        this.supername = supername;
        this.interfaces = interfaces;
        this.fieldToAccessFlag = fieldToAccess;
        if (cname.startsWith("[")) {
            throw new UnsupportedOperationException("Unexpected array class name: " + cname);
        }
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public int getAccess() {
        return access;
    }

    public boolean isInterface() {
        return (access & ACC_INTERFACE) != 0;
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

    public String getURLString() {
        return urlString;
    }

    public ImmutableSet<String> getFieldNames() {
        return fieldToAccessFlag.keySet();
    }

    public int getFieldAccess(String fieldName) {
        return fieldToAccessFlag.get(fieldName);
    }

    public ClassFile getSuperclass() {
        return getInstance(loader, supername);
    }

    /**
     * {@code ClassFile}'s counterpart of {@link Class#isAssignableFrom(Class)}.
     */
    public boolean isAssignableFrom(ClassFile classFile) {
        if (cname.equals(classFile.cname)) {
            return true;
        } else if (classFile.cname.equals("java/lang/Object")) {
            return false;
        }

        if (!classFile.isInterface()) {
            ClassFile superclassFile = ClassFile.getInstance(classFile.loader, classFile.supername);
            if (superclassFile != null) {
                if (isAssignableFrom(superclassFile)) {
                    return true;
                }
            }
        }

        for (String itf : classFile.interfaces) {
            ClassFile interfaceFile = getInstance(classFile.loader, itf);
            if (interfaceFile != null) {
                if (isAssignableFrom(interfaceFile)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return cname.hashCode() * 17 + urlString.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ClassFile) {
            ClassFile otherClassFile = (ClassFile) object;
            return cname.equals(otherClassFile) && urlString.equals(otherClassFile.urlString);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(urlString).append("\n")
            .append(cname).append(" {\n")
            .append("\tsupername: ").append(supername).append("\n")
            .append("\tinterfaces: ").append(interfaces).append("\n")
            .append("\tfields: ").append(fieldToAccessFlag).append("\n")
            .append("}\n");
        return sb.toString();
    }

    private static ClassFile create(ClassLoader loader, String urlString, ClassReader cr) {
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
        return new ClassFile(loader,
                urlString,
                cr.getAccess(),
                cr.getClassName(),
                cr.getSuperName(),
                ImmutableList.copyOf(cr.getInterfaces()),
                mapBuilder.build());
    }

    /**
     * Returns the class file instance of a class.
     *
     * @param loader
     *            the defining loader of the class, may be null if the bootstrap
     *            loader
     * @param cname
     *            the name of the class in the internal form of fully qualified
     *            class and interface names
     * @param cbuf
     *            the input byte buffer in class file format - must not be
     *            modified
     * @return the class metadata
     */
    public static ClassFile getInstance(ClassLoader loader, String cname, byte[] cbuf) {
        try {
            return getInstance0(loader, cname, cbuf);
        } catch (IOException e) {
            // should never happen
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the class file instance of a class when the bytecode of the class
     * is not available as in {@link #getInstance(ClassLoader, String, byte[])}.
     * <p>
     * Since the bytecode of the class is not given, we need to know how to
     * locate it. And that is what the {@code loader} is for.
     *
     * @param loader
     *            the initiating loader of the class, may be {@code null} if it
     *            is the bootstrap loader
     * @param cname
     *            the name of the class in the internal form of fully qualified
     *            class and interface names
     * @return the class file or {@code null} if the given class loader is
     *         unable to locate the class file
     */
    public static ClassFile getInstance(ClassLoader loader, String cname) {
        try {
            return getInstance0(loader, cname, null);
        } catch (IOException e) {
            return null;
        }
    }

    private static ClassFile getInstance0(ClassLoader loader, String cname, byte[] cbuf)
            throws IOException {
        /* compute URL used to locate the class file */
        URL url = getResource(loader, cname);
        String urlString = url != null ? url.toString() : ""; // generated class may not have a URL

        /* check if we already have the class file created */
        ClassFile classFile;
        synchronized (classFileTable) {
            classFile = classFileTable.get(urlString, cname);
        }
        if (classFile != null) {
            return classFile;
        }

        /* constructs the class reader to read the class */
        ClassReader cr;
        if (cbuf != null) {
            cr = new ClassReader(cbuf);
        } else {
            InputStream is = url != null ? url.openStream() : null;
            cr = new ClassReader(is);
        }

        classFile = ClassFile.create(loader, urlString, cr);
        synchronized (classFileTable) {
            classFileTable.put(urlString, cname, classFile);
        }

        /* record volatile variables */
        for (String fname : classFile.getFieldNames()) {
            if ((classFile.getFieldAccess(fname) & ACC_VOLATILE) != 0) {
                metadata.addVolatileVariable(cname, fname);
            }
        }

        return classFile;
    }

    private static URL getResource(ClassLoader loader, String cname) {
        String name = cname + ".class";
        return loader == null ? ClassLoader.getSystemResource(name) : loader.getResource(name);
    }

    /**
     * Checks if one class or interface is the same as, extends, or implements
     * another class or interface.
     *
     * @param loader
     *            the initiating loader of {@code type0}, may be null if it is
     *            the bootstrap class loader or unknown
     * @param type0
     *            the name of the first class or interface
     * @param type1
     *            the name of the second class of interface
     * @return {@code true} if {@code class1} is assignable from {@code class0}
     */
    public static boolean isSubtypeOf(ClassLoader loader, String type0, String type1) {
        if (type0.startsWith("[") || type1.startsWith("[")) {
            /* subtyping rules for array type are quite tricky;
             * see JLS $4.10.3. Subtyping among Array Types */
            throw new UnsupportedOperationException("Subtyping rules for array type not implemented!");
        }

        ClassFile classFile0 = getInstance(loader, type0);
        ClassFile classFile1 = getInstance(loader, type1);
        if (classFile0 == null || classFile1 == null) {
            System.err.printf("[Warning] failed to check subtyping relation between %s and %s:%n"
                    + "    unable to locate the class file of %s",
                    type0, type1, classFile0 == null ? type0 : type1);
            return false;
        }
        return classFile1.isAssignableFrom(classFile0);
    }

}