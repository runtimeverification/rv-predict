package com.runtimeverification.rvpredict.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;

public class ClassMetadata implements Opcodes {

    @Deprecated
    static final ConcurrentHashMap<String, ClassMetadata> cache = new ConcurrentHashMap<>();

    private static final Table<String, String, ClassMetadata> classMetadataTbl = HashBasedTable.create();

    private final String urlString;

    private final String cname;
    private final String supername;
    private final ImmutableList<String> interfaces;
    private final ImmutableMap<String, Integer> fieldToAccessFlag;

    private ClassMetadata(String urlString, String cname, String supername,
            ImmutableList<String> interfaces, ImmutableMap<String, Integer> fieldToAccess) {
        this.urlString = urlString;
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

    public String getURLString() {
        return urlString;
    }

    public ImmutableSet<String> getFieldNames() {
        return fieldToAccessFlag.keySet();
    }

    public int getAccess(String fieldName) {
        return fieldToAccessFlag.get(fieldName);
    }

    @Override
    public int hashCode() {
        return cname.hashCode() * 17 + urlString.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ClassMetadata) {
            ClassMetadata otherClassMetadata = (ClassMetadata) object;
            return cname.equals(otherClassMetadata) && urlString.equals(otherClassMetadata.urlString);
        }
        return false;
    }

    private static ClassMetadata create(String urlString, ClassReader cr) {
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
        return new ClassMetadata(urlString, cname, supername, interfaces, mapBuilder.build());
    }

    /**
     * Retrieves the metadata of a class.
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
    public static ClassMetadata getInstance(ClassLoader loader, String cname, byte[] cbuf) {
        return getInstance0(getResource(loader, cname), cname, new ClassReader(cbuf));
    }

    /**
     * Retrieves the metadata of a class when the bytecode of the class is not
     * available as in {@link #getInstance(ClassLoader, String, byte[])}.
     * <p>
     * Since the bytecode of the class is not given, we need to know how to
     * locate it. And that is what the {@code loader} is for.
     *
     * @param loader
     *            the initiating loader of the class, may be null if the
     *            bootstrap loader
     * @param cname
     *            the name of the class in the internal form of fully qualified
     *            class and interface names
     * @return the class metadata
     */
    public static ClassMetadata getInstance(ClassLoader loader, String cname) {
        try {
            URL url = getResource(loader, cname);
            InputStream is = url != null ? url.openStream() : null;
            return getInstance0(url, cname, new ClassReader(is));
        } catch (IOException e) {
            System.err.println("ASM ClassReader: unable to read " + cname);
            throw new RuntimeException(e);
        }
    }

    private static ClassMetadata getInstance0(URL url, String cname, ClassReader cr) {
        // generated class may not have a URL
        String urlString = url != null ? url.toString() : "";
        ClassMetadata classMetadata;
        synchronized (classMetadataTbl) {
            classMetadata = classMetadataTbl.get(urlString, cname);
        }
        if (classMetadata != null) {
            return classMetadata;
        }
        classMetadata = ClassMetadata.create(urlString, cr);
        synchronized (classMetadataTbl) {
            classMetadataTbl.put(urlString, cname, classMetadata);
        }
        cache.put(cname, classMetadata);

        // TODO(YilongL): is this really the best place to add tracking variables?
        for (String fname : classMetadata.getFieldNames()) {
            Metadata.trackVariable(cname, fname, classMetadata.getAccess(fname));
        }

        return classMetadata;
    }

    private static URL getResource(ClassLoader loader, String cname) {
        String name = cname + ".class";
        return loader == null ? ClassLoader.getSystemResource(name) : loader.getResource(name);
    }

}