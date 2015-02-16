package com.runtimeverification.rvpredict.instrumentation.transformer;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.instrumentation.Metadata;

import org.objectweb.asm.*;
import java.util.HashSet;
import java.util.Set;

public class ClassTransformer extends ClassVisitor implements Opcodes {

    private final Configuration config;
    private final ClassLoader loader;

    private String className;
    private String source;

    private int version;

    private final Set<String> finalFields = new HashSet<>();

    public static byte[] transform(ClassLoader loader, byte[] cbuf, Configuration config) {
        ClassReader cr = new ClassReader(cbuf);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassTransformer transformer = new ClassTransformer(cw, loader, config);
        cr.accept(transformer, 0);
        return cw.toByteArray();
    }

    private ClassTransformer(ClassWriter cw, ClassLoader loader, Configuration config) {
        super(ASM5, cw);
        assert cw != null;

        this.loader = loader;
        this.config = config;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
        className = name;
        this.version = version;
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        this.source = source;
        cv.visitSource(source, debug);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature,
            Object value) {
        // TODO(YilongL): remove this method completely
        if ((access & ACC_FINAL) != 0) {
            finalFields.add(name);
        }
        if ((access & ACC_VOLATILE) != 0) {
            Metadata.addVolatileVariable(className, name);
        }

        return cv.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
            String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        assert mv != null;

        /* do not instrument synthesized bridge method; otherwise, it may cause
         * infinite recursion at runtime */
        if ((access & ACC_BRIDGE) == 0) {
            int crntMaxLocals = 0;
            for (Type type : Type.getArgumentTypes(desc)) {
                crntMaxLocals += type.getSize();
            }

            mv = new MethodTransformer(mv, source, className, version, name, desc, access,
                    crntMaxLocals, finalFields, loader, config);
        }

        if ("<clinit>".equals(name)) {
            mv = new ClassInitializerTransformer(mv, access, name, desc);
        }

        return mv;
    }

}
